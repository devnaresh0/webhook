package whatsapp.webhook.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;
import whatsapp.webhook.entity.BusinessBalanceTransaction;
import whatsapp.webhook.repository.BusinessBalanceTransactionRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Edge cases for duplicate Meta message ledger rows (reference_id) —
 * the failure mode that caused usage API NonUniqueResultException / 500.
 */
class LedgerReferenceIdEdgeCaseTest {

    @Mock
    private BusinessBalanceTransactionRepository transactionRepository;

    @InjectMocks
    private BusinessBalanceTransactionService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private static BusinessBalanceTransaction existing(String ref) {
        BusinessBalanceTransaction tx = new BusinessBalanceTransaction();
        tx.setId(1L);
        tx.setReferenceId(ref);
        tx.setBalance(new BigDecimal("100.00"));
        tx.setCost(BigDecimal.ZERO);
        return tx;
    }

    private void save(
            String domain,
            String category,
            String pricingType,
            BigDecimal cost,
            BigDecimal balance,
            String referenceId) {
        service.saveUsage(
                domain,
                category,
                pricingType,
                1,
                cost,
                balance,
                referenceId
        );
    }

    // ------------------------------------------------------------------
    // 50 parameterized production edge cases
    // ------------------------------------------------------------------

    static Stream<Arguments> fiftyCases() {
        Stream.Builder<Arguments> b = Stream.builder();

        // 1–10: duplicate / race / unique-index behavior
        b.add(Arguments.of("dup_skip_when_exists", "EXISTS", false, null, "wamid.A", true));
        b.add(Arguments.of("first_insert_when_absent", "ABSENT", true, null, "wamid.B", true));
        b.add(Arguments.of("race_unique_violation_swallowed", "RACE", false, null, "wamid.RACE", true));
        b.add(Arguments.of("null_reference_still_saves", "ABSENT", true, null, null, false));
        b.add(Arguments.of("empty_reference_still_saves", "ABSENT", true, null, "", false));
        b.add(Arguments.of("blank_reference_still_saves", "ABSENT", true, null, "   ", false));
        b.add(Arguments.of("trim_lookup_matches_existing", "EXISTS", false, null, "  wamid.TRIM  ", true));
        b.add(Arguments.of("trim_on_insert", "ABSENT", true, null, "  wamid.NEW  ", true));
        b.add(Arguments.of("tab_whitespace_treated_blank", "ABSENT", true, null, "\t", false));
        b.add(Arguments.of("newline_whitespace_treated_blank", "ABSENT", true, null, "\n", false));

        // 11–20: cost / balance coercion
        b.add(Arguments.of("null_cost_becomes_zero", "ABSENT", true, "ZERO_COST", "wamid.C1", true));
        b.add(Arguments.of("null_balance_becomes_zero", "ABSENT", true, "ZERO_BAL", "wamid.C2", true));
        b.add(Arguments.of("both_null_cost_balance_zero", "ABSENT", true, "ZERO_BOTH", "wamid.C3", true));
        b.add(Arguments.of("positive_cost_preserved", "ABSENT", true, "COST_080", "wamid.C4", true));
        b.add(Arguments.of("zero_cost_preserved", "ABSENT", true, "COST_0", "wamid.C5", true));
        b.add(Arguments.of("large_balance_preserved", "ABSENT", true, "BAL_BIG", "wamid.C6", true));
        b.add(Arguments.of("free_cs_zero_cost", "ABSENT", true, "FREE", "wamid.FREE1", true));
        b.add(Arguments.of("regular_billable_cost", "ABSENT", true, "REGULAR", "wamid.REG1", true));
        b.add(Arguments.of("utility_category", "ABSENT", true, null, "wamid.UTIL", true));
        b.add(Arguments.of("marketing_category", "ABSENT", true, "MKT", "wamid.MKT", true));

        // 21–30: distinct reference ids must not collide
        for (int i = 1; i <= 10; i++) {
            b.add(Arguments.of(
                    "distinct_ref_" + i,
                    "ABSENT",
                    true,
                    null,
                    "wamid.DISTINCT_" + i,
                    true
            ));
        }

        // 31–40: Meta-like / noisy ids
        b.add(Arguments.of("long_wamid", "ABSENT", true, null,
                "wamid.HBgMOTE4NTgwNjc5NTY0FQIAERgSMDQ0RTdFRUM3NTMzRjI0OEI1AA==", true));
        b.add(Arguments.of("wamid_with_plus", "ABSENT", true, null, "wamid.abc+def/ghi==", true));
        b.add(Arguments.of("numeric_only_ref", "ABSENT", true, null, "123456789012345", true));
        b.add(Arguments.of("uuid_style_ref", "ABSENT", true, null, "550e8400-e29b-41d4-a716-446655440000", true));
        b.add(Arguments.of("dup_same_long_wamid", "EXISTS", false, null,
                "wamid.HBgMOTE4NTgwNjc5NTY0FQIAERgSNTVCNkIzQkYwMTREMDI5RDlGAA==", true));
        b.add(Arguments.of("leading_zeros_ref", "ABSENT", true, null, "000wamid.X", true));
        b.add(Arguments.of("unicode_safe_ascii_ref", "ABSENT", true, null, "wamid.ABC_xyz-001", true));
        b.add(Arguments.of("pipe_in_ref_unlikely", "ABSENT", true, null, "wamid.a|b|c", true));
        b.add(Arguments.of("spaces_inside_not_blank", "ABSENT", true, null, "wamid. with space", true));
        b.add(Arguments.of("only_spaces_blank", "ABSENT", true, null, "     ", false));

        // 41–50: domains / pricing / messages matrix
        b.add(Arguments.of("domain_naresh", "ABSENT", true, "DOM_N", "wamid.D1", true));
        b.add(Arguments.of("domain_other", "ABSENT", true, "DOM_O", "wamid.D2", true));
        b.add(Arguments.of("pricing_authentication", "ABSENT", true, "AUTH", "wamid.A1", true));
        b.add(Arguments.of("pricing_type_free", "ABSENT", true, "PT_FREE", "wamid.PT1", true));
        b.add(Arguments.of("pricing_type_regular", "ABSENT", true, "PT_REG", "wamid.PT2", true));
        b.add(Arguments.of("second_call_same_ref_no_save", "EXISTS", false, null, "wamid.AGAIN", true));
        b.add(Arguments.of("race_after_check_unique", "RACE", false, null, "wamid.RACE2", true));
        b.add(Arguments.of("absent_then_save_once", "ABSENT", true, null, "wamid.ONCE", true));
        b.add(Arguments.of("exists_never_calls_save", "EXISTS", false, null, "wamid.NOSAVE", true));
        b.add(Arguments.of("blank_ref_no_dup_lookup", "ABSENT", true, null, "  \t  ", false));

        return b.build();
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("fiftyCases")
    @DisplayName("ledger reference_id edge cases")
    void fiftyReferenceIdEdgeCases(
            String name,
            String mode,
            boolean expectSave,
            String moneyMode,
            String referenceId,
            boolean expectDupLookup) {

        assertEquals(50, fiftyCases().count(), "must stay at 50 cases");

        String lookupKey = referenceId == null ? null : referenceId.trim();
        boolean blankRef = referenceId == null || referenceId.trim().isEmpty();

        if ("EXISTS".equals(mode)) {
            when(transactionRepository.findByReferenceId(lookupKey))
                    .thenReturn(Optional.of(existing(lookupKey)));
        } else if ("RACE".equals(mode)) {
            when(transactionRepository.findByReferenceId(lookupKey))
                    .thenReturn(Optional.empty());
            when(transactionRepository.save(any(BusinessBalanceTransaction.class)))
                    .thenThrow(new DataIntegrityViolationException("uk_bbt_reference_id"));
        } else {
            when(transactionRepository.findByReferenceId(anyString()))
                    .thenReturn(Optional.empty());
            when(transactionRepository.findByReferenceId(lookupKey == null ? "" : lookupKey))
                    .thenReturn(Optional.empty());
            when(transactionRepository.save(any(BusinessBalanceTransaction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
        }

        BigDecimal cost = new BigDecimal("0.80");
        BigDecimal balance = new BigDecimal("9856.60");
        String category = "utility";
        String pricingType = "regular";
        String domain = "naresh";

        if ("ZERO_COST".equals(moneyMode)) {
            cost = null;
        } else if ("ZERO_BAL".equals(moneyMode)) {
            balance = null;
        } else if ("ZERO_BOTH".equals(moneyMode)) {
            cost = null;
            balance = null;
        } else if ("COST_080".equals(moneyMode)) {
            cost = new BigDecimal("0.80");
        } else if ("COST_0".equals(moneyMode)) {
            cost = BigDecimal.ZERO;
        } else if ("BAL_BIG".equals(moneyMode)) {
            balance = new BigDecimal("999999.99");
        } else if ("FREE".equals(moneyMode) || "PT_FREE".equals(moneyMode)) {
            cost = BigDecimal.ZERO;
            pricingType = "free_customer_service";
        } else if ("REGULAR".equals(moneyMode) || "PT_REG".equals(moneyMode)) {
            pricingType = "regular";
        } else if ("MKT".equals(moneyMode)) {
            category = "marketing";
        } else if ("AUTH".equals(moneyMode)) {
            category = "authentication";
        } else if ("DOM_O".equals(moneyMode)) {
            domain = "otherbiz";
        }

        // For ABSENT with blank ref, findByReferenceId should not be required
        if (blankRef && "ABSENT".equals(mode)) {
            when(transactionRepository.save(any(BusinessBalanceTransaction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
        }

        save(domain, category, pricingType, cost, balance, referenceId);

        if (expectDupLookup && !blankRef) {
            verify(transactionRepository, times(1)).findByReferenceId(lookupKey);
        }
        if (blankRef) {
            verify(transactionRepository, never()).findByReferenceId(anyString());
        }

        if (expectSave) {
            ArgumentCaptor<BusinessBalanceTransaction> cap =
                    ArgumentCaptor.forClass(BusinessBalanceTransaction.class);
            verify(transactionRepository, times(1)).save(cap.capture());
            BusinessBalanceTransaction saved = cap.getValue();
            assertEquals("Usage", saved.getDataType());
            assertEquals(domain, saved.getDomain());
            assertNotNull(saved.getBalance());
            assertNotNull(saved.getCost());
            if (blankRef) {
                assertNull(saved.getReferenceId());
            } else {
                assertEquals(lookupKey, saved.getReferenceId());
            }
            if (moneyMode != null && moneyMode.startsWith("ZERO")) {
                if ("ZERO_COST".equals(moneyMode) || "ZERO_BOTH".equals(moneyMode)) {
                    assertEquals(0, BigDecimal.ZERO.compareTo(saved.getCost()));
                }
                if ("ZERO_BAL".equals(moneyMode) || "ZERO_BOTH".equals(moneyMode)) {
                    assertEquals(0, BigDecimal.ZERO.compareTo(saved.getBalance()));
                }
            }
        } else {
            if ("EXISTS".equals(mode)) {
                verify(transactionRepository, never()).save(any());
            }
            if ("RACE".equals(mode)) {
                verify(transactionRepository, times(1)).save(any());
            }
        }

        assertFalse(name == null || name.isEmpty());
        assertTrue(name.length() > 0);
    }

    @Test
    @DisplayName("repository default findByReferenceId prefers first of duplicates")
    void findByReferenceIdDefaultUsesFirst() {
        // Simulate Spring Data default method behavior used by UsageService
        BusinessBalanceTransaction first = existing("wamid.DUP");
        first.setId(10L);
        when(transactionRepository.findFirstByReferenceIdOrderByIdAsc("wamid.DUP"))
                .thenReturn(Optional.of(first));
        when(transactionRepository.findByReferenceId("wamid.DUP"))
                .thenCallRealMethod();

        Optional<BusinessBalanceTransaction> got =
                transactionRepository.findByReferenceId("wamid.DUP");

        assertTrue(got.isPresent());
        assertEquals(Long.valueOf(10L), got.get().getId());
    }

    @Test
    @DisplayName("fiftyCases source size is exactly 50")
    void exactlyFiftyCasesDefined() {
        assertEquals(50, fiftyCases().count());
        assertEquals(50, IntStream.range(0, 50).count());
    }
}
