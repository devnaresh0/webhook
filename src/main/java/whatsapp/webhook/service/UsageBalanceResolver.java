package whatsapp.webhook.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Resolves the Balance column for usage rows so the UI never shows a blank cell.
 * Prefer ledger snapshot by message id, then nearest prior ledger, then live wallet.
 */
public final class UsageBalanceResolver {

    public interface Sources {
        Optional<BigDecimal> balanceByReferenceId(String messageId);

        Optional<BigDecimal> priorBalance(String domain, LocalDateTime sentAt);

        Optional<BigDecimal> liveWallet(String domain);
    }

    private UsageBalanceResolver() {
    }

    /**
     * Never returns null — blank UI cells come from JSON null.
     */
    public static BigDecimal resolve(
            String messageId,
            String domain,
            Timestamp sentAt,
            Sources sources) {

        if (messageId != null && !messageId.trim().isEmpty()) {
            Optional<BigDecimal> byRef =
                    sources.balanceByReferenceId(messageId.trim());
            if (byRef.isPresent() && byRef.get() != null) {
                return byRef.get();
            }
        }

        if (domain != null && sentAt != null) {
            Optional<BigDecimal> prior =
                    sources.priorBalance(domain, sentAt.toLocalDateTime());
            if (prior.isPresent() && prior.get() != null) {
                return prior.get();
            }
        }

        if (domain != null) {
            Optional<BigDecimal> live = sources.liveWallet(domain);
            if (live.isPresent() && live.get() != null) {
                return live.get();
            }
        }

        return BigDecimal.ZERO;
    }
}
