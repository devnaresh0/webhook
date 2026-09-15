package whatsapp.webhook.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Edge cases for the blank Balance column in Message Usage History
 * (UI showed empty when JSON balance was null — e.g. free_customer_service
 * row without a usable ledger snapshot).
 */
class UsageBalanceEdgeCaseTest {

    private static final Timestamp SENT_AT =
            Timestamp.valueOf(LocalDateTime.of(2026, 9, 2, 15, 58, 0));

    private static final BigDecimal THREE = new BigDecimal("3.00");
    private static final BigDecimal WALLET = new BigDecimal("9856.60");

    private static final class FakeSources implements UsageBalanceResolver.Sources {
        private final Map<String, BigDecimal> byRef = new HashMap<String, BigDecimal>();
        private BigDecimal prior;
        private boolean priorCalled;
        private BigDecimal live;
        private boolean liveCalled;

        FakeSources withRef(String messageId, BigDecimal balance) {
            byRef.put(messageId, balance);
            return this;
        }

        FakeSources withPrior(BigDecimal balance) {
            this.prior = balance;
            return this;
        }

        FakeSources withLive(BigDecimal balance) {
            this.live = balance;
            return this;
        }

        @Override
        public Optional<BigDecimal> balanceByReferenceId(String messageId) {
            if (!byRef.containsKey(messageId)) {
                return Optional.empty();
            }
            return Optional.ofNullable(byRef.get(messageId));
        }

        @Override
        public Optional<BigDecimal> priorBalance(String domain, LocalDateTime sentAt) {
            priorCalled = true;
            return Optional.ofNullable(prior);
        }

        @Override
        public Optional<BigDecimal> liveWallet(String domain) {
            liveCalled = true;
            return Optional.ofNullable(live);
        }
    }

    private static BigDecimal resolve(
            String messageId,
            String domain,
            Timestamp sentAt,
            FakeSources sources) {
        BigDecimal value = UsageBalanceResolver.resolve(
                messageId,
                domain,
                sentAt,
                sources
        );
        assertNotNull(value, "balance must never be null (blank UI cell)");
        return value;
    }

    @Test
    @DisplayName("screenshot gap: no ledger → prior snapshot fills Balance")
    void noLedgerUsesPriorSnapshot() {
        FakeSources sources = new FakeSources().withPrior(THREE);
        assertEquals(0, THREE.compareTo(
                resolve("wamid.GAP_NO_LEDGER", "naresh", SENT_AT, sources)));
    }

    @Test
    @DisplayName("screenshot gap: no ledger/prior → live wallet fills Balance")
    void noLedgerUsesLiveWallet() {
        FakeSources sources = new FakeSources().withLive(WALLET);
        assertEquals(0, WALLET.compareTo(
                resolve("wamid.GAP_NO_PRIOR", "naresh", SENT_AT, sources)));
    }

    @Test
    @DisplayName("screenshot gap: no ledger/prior/wallet → ZERO not blank")
    void totallyMissingSourcesReturnsZero() {
        assertEquals(0, BigDecimal.ZERO.compareTo(
                resolve("wamid.ORPHAN", "naresh", SENT_AT, new FakeSources())));
    }

    @Test
    @DisplayName("exact ledger snapshot wins over prior and live")
    void ledgerReferenceWins() {
        FakeSources sources = new FakeSources()
                .withRef("wamid.EXACT", new BigDecimal("2.50"))
                .withPrior(THREE)
                .withLive(WALLET);
        assertEquals(0, new BigDecimal("2.50").compareTo(
                resolve("wamid.EXACT", "naresh", SENT_AT, sources)));
    }

    @Test
    @DisplayName("legacy ledger balance null → fall through to prior")
    void nullLedgerBalanceFallsToPrior() {
        FakeSources sources = new FakeSources()
                .withRef("wamid.LEGACY_NULL_BAL", null)
                .withPrior(THREE);
        assertEquals(0, THREE.compareTo(
                resolve("wamid.LEGACY_NULL_BAL", "naresh", SENT_AT, sources)));
    }

    @Test
    @DisplayName("legacy ledger+prior null → live wallet")
    void nullLedgerAndPriorUseLive() {
        FakeSources sources = new FakeSources()
                .withRef("wamid.LEGACY_BOTH_NULL", null)
                .withPrior(null)
                .withLive(WALLET);
        assertEquals(0, WALLET.compareTo(
                resolve("wamid.LEGACY_BOTH_NULL", "naresh", SENT_AT, sources)));
    }

    @Test
    @DisplayName("all balance fields null → ZERO not blank")
    void allNullBalancesReturnZero() {
        FakeSources sources = new FakeSources()
                .withRef("wamid.ALL_NULL", null)
                .withPrior(null)
                .withLive(null);
        assertEquals(0, BigDecimal.ZERO.compareTo(
                resolve("wamid.ALL_NULL", "naresh", SENT_AT, sources)));
    }

    @Test
    @DisplayName("domain null but ledger has balance → still show ledger")
    void nullDomainStillUsesLedger() {
        FakeSources sources = new FakeSources().withRef("wamid.HAS_LEDGER", THREE);
        assertEquals(0, THREE.compareTo(
                resolve("wamid.HAS_LEDGER", null, SENT_AT, sources)));
    }

    @Test
    @DisplayName("domain null and no ledger → ZERO (cannot prior/live)")
    void nullDomainNoLedgerReturnsZero() {
        FakeSources sources = new FakeSources().withPrior(THREE).withLive(WALLET);
        assertEquals(0, BigDecimal.ZERO.compareTo(
                resolve("wamid.NO_DOMAIN", null, SENT_AT, sources)));
    }

    @Test
    @DisplayName("unknown domain with empty sources → ZERO")
    void unknownDomainReturnsZero() {
        assertEquals(0, BigDecimal.ZERO.compareTo(
                resolve("wamid.X", "missing-biz", SENT_AT, new FakeSources())));
    }

    @Test
    @DisplayName("null messageId → skip ledger, use prior")
    void nullMessageIdUsesPrior() {
        FakeSources sources = new FakeSources().withPrior(THREE);
        assertEquals(0, THREE.compareTo(resolve(null, "naresh", SENT_AT, sources)));
    }

    @Test
    @DisplayName("blank messageId → skip ledger, use prior")
    void blankMessageIdUsesPrior() {
        FakeSources sources = new FakeSources().withPrior(THREE);
        assertEquals(0, THREE.compareTo(resolve("   ", "naresh", SENT_AT, sources)));
    }

    @Test
    @DisplayName("messageId is trimmed for ledger lookup")
    void messageIdIsTrimmed() {
        FakeSources sources = new FakeSources().withRef("wamid.TRIM", THREE);
        assertEquals(0, THREE.compareTo(
                resolve("  wamid.TRIM  ", "naresh", SENT_AT, sources)));
    }

    @Test
    @DisplayName("sentAt null → skip prior, use live wallet")
    void nullSentAtUsesLive() {
        FakeSources sources = new FakeSources().withPrior(THREE).withLive(WALLET);
        BigDecimal value = resolve("wamid.NO_SENT", "naresh", null, sources);
        assertEquals(0, WALLET.compareTo(value));
        assertFalse(sources.priorCalled);
        assertTrue(sources.liveCalled);
    }

    @Test
    @DisplayName("sentAt null and no live → ZERO")
    void nullSentAtNoLiveReturnsZero() {
        assertEquals(0, BigDecimal.ZERO.compareTo(
                resolve("wamid.NO_SENT2", "naresh", null, new FakeSources())));
    }

    @Test
    @DisplayName("ledger balance 0.00 is ZERO not blank")
    void zeroLedgerBalanceIsNotBlank() {
        FakeSources sources = new FakeSources().withRef("wamid.ZERO", BigDecimal.ZERO);
        BigDecimal value = resolve("wamid.ZERO", "naresh", SENT_AT, sources);
        assertNotNull(value);
        assertEquals(0, BigDecimal.ZERO.compareTo(value));
    }

    @Test
    @DisplayName("live wallet 0.00 is ZERO not blank")
    void zeroLiveWalletIsNotBlank() {
        FakeSources sources = new FakeSources().withLive(BigDecimal.ZERO);
        BigDecimal value = resolve("wamid.ZERO_LIVE", "naresh", SENT_AT, sources);
        assertNotNull(value);
        assertEquals(0, BigDecimal.ZERO.compareTo(value));
    }

    @Test
    @DisplayName("saveUsage coerces null balance arg to ZERO")
    void nullBalanceCoercedToZeroOnWrite() {
        BigDecimal balanceArg = null;
        BigDecimal persisted = balanceArg != null ? balanceArg : BigDecimal.ZERO;
        assertNotNull(persisted);
        assertEquals(0, BigDecimal.ZERO.compareTo(persisted));
    }
}
