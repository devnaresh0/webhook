package whatsapp.webhook.service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Converts a caller calendar-day range in an IANA zone into an exclusive UTC
 * LocalDateTime window for timestamp-without-time-zone columns stored as UTC wall-clock.
 */
public final class UsageUtcRange {

    public final LocalDateTime fromInclusive;
    public final LocalDateTime toExclusive;

    private UsageUtcRange(LocalDateTime fromInclusive, LocalDateTime toExclusive) {
        this.fromInclusive = fromInclusive;
        this.toExclusive = toExclusive;
    }

    public static ZoneId resolveZone(String timeZone) {
        try {
            return ZoneId.of(
                    timeZone == null || timeZone.trim().isEmpty()
                            ? "UTC"
                            : timeZone.trim()
            );
        } catch (DateTimeException e) {
            throw new IllegalArgumentException(
                    "Invalid timeZone. Use an IANA id such as UTC or Asia/Kolkata."
            );
        }
    }

    public static UsageUtcRange of(LocalDate fromDate, LocalDate toDate, ZoneId zone) {
        LocalDateTime from = fromDate
                .atStartOfDay(zone)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();

        LocalDateTime toExclusive = toDate
                .plusDays(1)
                .atStartOfDay(zone)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();

        return new UsageUtcRange(from, toExclusive);
    }

    public boolean contains(LocalDateTime utcWallClock) {
        return !utcWallClock.isBefore(fromInclusive)
                && utcWallClock.isBefore(toExclusive);
    }
}
