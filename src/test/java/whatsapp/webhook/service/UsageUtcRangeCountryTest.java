package whatsapp.webhook.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One midnight-boundary case per country: a message one second before local
 * midnight belongs to that calendar day; the message at local midnight belongs
 * to the next day. Expected UTC wall-clock bounds are asserted as well.
 */
class UsageUtcRangeCountryTest {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 14);

    static Stream<Arguments> countries() {
        return Stream.of(
                Arguments.of("Kenya", "KE", "Africa/Nairobi"),
                Arguments.of("India", "IN", "Asia/Kolkata"),
                Arguments.of("United States (Eastern)", "US", "America/New_York"),
                Arguments.of("United Kingdom", "GB", "Europe/London"),
                Arguments.of("Japan", "JP", "Asia/Tokyo"),
                Arguments.of("Australia (Sydney)", "AU", "Australia/Sydney"),
                Arguments.of("Brazil (Sao Paulo)", "BR", "America/Sao_Paulo"),
                Arguments.of("South Africa", "ZA", "Africa/Johannesburg"),
                Arguments.of("United Arab Emirates", "AE", "Asia/Dubai"),
                Arguments.of("Singapore", "SG", "Asia/Singapore"),
                Arguments.of("Germany", "DE", "Europe/Berlin"),
                Arguments.of("France", "FR", "Europe/Paris"),
                Arguments.of("Canada (Toronto)", "CA", "America/Toronto"),
                Arguments.of("Mexico (Mexico City)", "MX", "America/Mexico_City"),
                Arguments.of("Nigeria", "NG", "Africa/Lagos"),
                Arguments.of("Egypt", "EG", "Africa/Cairo"),
                Arguments.of("Saudi Arabia", "SA", "Asia/Riyadh"),
                Arguments.of("Indonesia (Jakarta)", "ID", "Asia/Jakarta"),
                Arguments.of("Philippines", "PH", "Asia/Manila"),
                Arguments.of("Thailand", "TH", "Asia/Bangkok"),
                Arguments.of("Vietnam", "VN", "Asia/Ho_Chi_Minh"),
                Arguments.of("South Korea", "KR", "Asia/Seoul"),
                Arguments.of("China", "CN", "Asia/Shanghai"),
                Arguments.of("Hong Kong", "HK", "Asia/Hong_Kong"),
                Arguments.of("Taiwan", "TW", "Asia/Taipei"),
                Arguments.of("Malaysia", "MY", "Asia/Kuala_Lumpur"),
                Arguments.of("Pakistan", "PK", "Asia/Karachi"),
                Arguments.of("Bangladesh", "BD", "Asia/Dhaka"),
                Arguments.of("Nepal", "NP", "Asia/Kathmandu"),
                Arguments.of("Sri Lanka", "LK", "Asia/Colombo"),
                Arguments.of("Iran", "IR", "Asia/Tehran"),
                Arguments.of("Turkey", "TR", "Europe/Istanbul"),
                Arguments.of("Russia (Moscow)", "RU", "Europe/Moscow"),
                Arguments.of("Ukraine", "UA", "Europe/Kiev"),
                Arguments.of("Poland", "PL", "Europe/Warsaw"),
                Arguments.of("Italy", "IT", "Europe/Rome"),
                Arguments.of("Spain", "ES", "Europe/Madrid"),
                Arguments.of("Netherlands", "NL", "Europe/Amsterdam"),
                Arguments.of("Sweden", "SE", "Europe/Stockholm"),
                Arguments.of("Switzerland", "CH", "Europe/Zurich"),
                Arguments.of("Israel", "IL", "Asia/Jerusalem"),
                Arguments.of("Argentina", "AR", "America/Argentina/Buenos_Aires"),
                Arguments.of("Chile", "CL", "America/Santiago"),
                Arguments.of("Colombia", "CO", "America/Bogota"),
                Arguments.of("Peru", "PE", "America/Lima"),
                Arguments.of("New Zealand", "NZ", "Pacific/Auckland"),
                Arguments.of("Fiji", "FJ", "Pacific/Fiji"),
                Arguments.of("Ghana", "GH", "Africa/Accra"),
                Arguments.of("Morocco", "MA", "Africa/Casablanca"),
                Arguments.of("Ethiopia", "ET", "Africa/Addis_Ababa")
        );
    }

    @ParameterizedTest(name = "{index}: {0} ({1}) {2}")
    @MethodSource("countries")
    void midnightBoundarySplitsCalendarDays(
            String country,
            String countryCode,
            String zoneId) {

        ZoneId zone = ZoneId.of(zoneId);
        UsageUtcRange dayRange = UsageUtcRange.of(DAY, DAY, zone);
        UsageUtcRange nextDayRange = UsageUtcRange.of(DAY.plusDays(1), DAY.plusDays(1), zone);

        ZonedDateTime localMidnightNext =
                DAY.plusDays(1).atStartOfDay(zone);

        LocalDateTime justBeforeUtc = localMidnightNext
                .minusSeconds(1)
                .withZoneSameInstant(java.time.ZoneOffset.UTC)
                .toLocalDateTime();

        LocalDateTime atMidnightUtc = localMidnightNext
                .withZoneSameInstant(java.time.ZoneOffset.UTC)
                .toLocalDateTime();

        assertTrue(
                dayRange.contains(justBeforeUtc),
                country + ": 1s before local midnight must be on " + DAY
        );
        assertFalse(
                dayRange.contains(atMidnightUtc),
                country + ": local midnight must not be on " + DAY
        );
        assertTrue(
                nextDayRange.contains(atMidnightUtc),
                country + ": local midnight must be on " + DAY.plusDays(1)
        );

        LocalDateTime expectedFrom = DAY
                .atStartOfDay(zone)
                .withZoneSameInstant(java.time.ZoneOffset.UTC)
                .toLocalDateTime();
        LocalDateTime expectedToExclusive = DAY
                .plusDays(1)
                .atStartOfDay(zone)
                .withZoneSameInstant(java.time.ZoneOffset.UTC)
                .toLocalDateTime();

        assertEquals(expectedFrom, dayRange.fromInclusive, country + " from");
        assertEquals(expectedToExclusive, dayRange.toExclusive, country + " toExclusive");
        assertEquals(countryCode.length(), 2, country + " ISO country code");
    }
}
