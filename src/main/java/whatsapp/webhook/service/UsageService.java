package whatsapp.webhook.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import whatsapp.webhook.entity.BusinessBalanceTransaction;
import whatsapp.webhook.entity.WhatsAppMessageLink;
import whatsapp.webhook.entity.WhatsAppPhoneNumber;
import whatsapp.webhook.model.WhatsAppResponse;

import whatsapp.webhook.repository.BusinessBalanceRepository;
import whatsapp.webhook.repository.BusinessBalanceTransactionRepository;
import whatsapp.webhook.repository.CustomerConversationRepository;
import whatsapp.webhook.repository.MessagingRateRepository;
import whatsapp.webhook.repository.WhatsAppPhoneNumberRepository;
import whatsapp.webhook.repository.WhatsAppResponseRepository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UsageService {

    @Autowired
    private CustomerConversationRepository conversationRepository;

    @Autowired
    private BusinessBalanceTransactionRepository transactionRepository;

    @Autowired
    private MessagingRateRepository rateRepository;

    @Autowired
    private BusinessBalanceRepository balanceRepository;

    @Autowired
    private WhatsAppPhoneNumberRepository phoneRepository;

    @Autowired
    private WhatsAppMessageLinkService messageLinkService;

    @Autowired
    private WhatsAppResponseRepository responseRepository;


    public Map<String, Object> getUsage(
            String phoneNumberId,
            String fromDate,
            String toDate,
            String timeZone,
            int page,
            int pageSize) {

        ZoneId zone = UsageUtcRange.resolveZone(timeZone);

        // Caller calendar day in their zone → UTC range for DB (stored as UTC wall clock)
        UsageUtcRange range = UsageUtcRange.of(
                LocalDate.parse(fromDate),
                LocalDate.parse(toDate),
                zone
        );
        LocalDateTime from = range.fromInclusive;
        LocalDateTime toExclusive = range.toExclusive;


        // ==========================================
        // PAGINATION
        // ==========================================

        int offset =
                (page - 1) * pageSize;


        // ==========================================
        // USAGE RECORDS
        // ==========================================

        // ==========================================
// DEBUG - USAGE QUERY
// ==========================================

        System.out.println("==========================================");
        System.out.println("          GET USAGE DEBUG");
        System.out.println("==========================================");
        System.out.println("Phone Number ID : [" + phoneNumberId + "]");
        System.out.println("Time Zone       : [" + zone + "]");
        System.out.println("From UTC        : [" + from + "]");
        System.out.println("To UTC (excl)   : [" + toExclusive + "]");
        System.out.println("Page            : [" + page + "]");
        System.out.println("Page Size       : [" + pageSize + "]");
        System.out.println("Offset          : [" + offset + "]");
        System.out.println("==========================================");


        // Bind as UTC Timestamps so native queries are not shifted by the JVM zone
        Timestamp fromTs = Timestamp.from(from.toInstant(ZoneOffset.UTC));
        Timestamp toTs = Timestamp.from(toExclusive.toInstant(ZoneOffset.UTC));

        List<Object[]> rows =
                conversationRepository.getUsage(
                        phoneNumberId,
                        fromTs,
                        toTs,
                        pageSize,
                        offset
                );


        System.out.println("==========================================");
        System.out.println("ROWS RETURNED   : " + rows.size());
        System.out.println("==========================================");


// Print every returned row
        for (Object[] debugRow : rows) {

            System.out.println(
                    "MESSAGE ID      : " + debugRow[0]
            );

            System.out.println(
                    "SENT AT         : " + debugRow[1]
            );

            System.out.println(
                    "CATEGORY        : " + debugRow[2]
            );

            System.out.println(
                    "PRICING MODEL   : " + debugRow[3]
            );

            System.out.println(
                    "PRICING TYPE    : " + debugRow[4]
            );

            System.out.println(
                    "BILLABLE        : " + debugRow[5]
            );

            System.out.println(
                    "PHONE NUMBER ID : " + debugRow[6]
            );

            System.out.println("------------------------------------------");
        }


        long totalItems =
                conversationRepository.countUsage(
                        phoneNumberId,
                        fromTs,
                        toTs
                );

        System.out.println("==========================================");
        System.out.println("TOTAL ITEMS     : " + totalItems);
        System.out.println("==========================================");

        int totalPages =
                (int) Math.ceil(
                        (double) totalItems / pageSize
                );


        // ==========================================
        // MESSAGE CATEGORY COUNTS
        // ==========================================

        List<Object[]> categoryCounts =
                conversationRepository.countMessagesByCategory(
                        phoneNumberId,
                        fromTs,
                        toTs
                );


        int utilityMessages = 0;
        int marketingMessages = 0;
        int authenticationMessages = 0;


        for (Object[] categoryRow : categoryCounts) {

            if (categoryRow == null ||
                    categoryRow.length < 2) {
                continue;
            }


            String category =
                    categoryRow[0] != null
                            ? categoryRow[0].toString().trim()
                            : "";


            long count = 0;

            if (categoryRow[1] != null) {
                count =
                        ((Number) categoryRow[1])
                                .longValue();
            }


            if ("utility".equalsIgnoreCase(category)) {

                utilityMessages +=
                        (int) count;

            } else if (
                    "marketing".equalsIgnoreCase(category)) {

                marketingMessages +=
                        (int) count;

            } else if (
                    "authentication".equalsIgnoreCase(category)) {

                authenticationMessages +=
                        (int) count;
            }
        }


        // ==========================================
        // TABLE RESULTS
        // ==========================================

        List<Map<String, Object>> result =
                new ArrayList<>();


        for (Object[] row : rows) {

            /*
             * Query order:
             *
             * 0 = message_id
             * 1 = sent_at
             * 2 = conversation_type
             * 3 = pricing_model
             * 4 = pricing_type
             * 5 = billable
             * 6 = phone_number_id
             * 7 = customer_phone (receiver)
             */

            String messageId =
                    row[0] != null
                            ? row[0].toString()
                            : "";


            java.sql.Timestamp sentAt =
                    (java.sql.Timestamp) row[1];

            String sentAtUtc =
                    formatUtcInstant(sentAt);


            String category =
                    row[2] != null
                            ? row[2].toString()
                            : "";


            String pricingType =
                    row[4] != null
                            ? row[4].toString()
                            : "";


            Boolean billable =
                    (Boolean) row[5];


            String rowPhoneNumberId =
                    row[6] != null
                            ? row[6].toString()
                            : "";


            String receiver =
                    row.length > 7 && row[7] != null
                            ? row[7].toString()
                            : "";


            // ==========================================
            // DOMAIN
            // ==========================================

            String domain =
                    phoneRepository
                            .findByPhoneNumberId(
                                    rowPhoneNumberId
                            )
                            .map(WhatsAppPhoneNumber::getDomain)
                            .orElse(null);


            // ==========================================
            // LEDGER (source of truth for cost + balance)
            // ==========================================

            Optional<BusinessBalanceTransaction> ledger =
                    (messageId != null && !messageId.trim().isEmpty())
                            ? transactionRepository.findByReferenceId(messageId)
                            : Optional.<BusinessBalanceTransaction>empty();


            // ==========================================
            // COST — only what was actually charged
            // ==========================================

            Double cost = 0.0;

            if (ledger.isPresent() && ledger.get().getCost() != null) {
                cost = ledger.get().getCost().doubleValue();
            }


            // ==========================================
            // BALANCE — remaining wallet from ledger
            // ==========================================

            BigDecimal rowBalance =
                    resolveUsageBalance(
                            messageId,
                            domain,
                            sentAt
                    );


            // ==========================================
            // TABLE ITEM
            // ==========================================

            Map<String, Object> item =
                    new HashMap<>();


            item.put(
                    "date",
                    sentAtUtc
            );


            item.put(
                    "sent_at",
                    sentAtUtc
            );


            item.put(
                    "data_type",
                    "Usage"
            );


            item.put(
                    "pricing_category",
                    category
            );


            item.put(
                    "pricing_type",
                    pricingType
            );


            item.put(
                    "volume",
                    1
            );


            item.put(
                    "load_amount",
                    null
            );


            item.put(
                    "cost",
                    cost
            );


            item.put(
                    "balance",
                    rowBalance
            );


            item.put(
                    "phone_number_id",
                    rowPhoneNumberId
            );

            item.put(
                    "receiver",
                    receiver
            );

            Map<String, Object> decision =
                    resolveDecision(messageId);
            Object decisionValue = decision.get("decision");
            item.put(
                    "decision",
                    decisionValue != null && !String.valueOf(decisionValue).trim().isEmpty()
                            ? decisionValue
                            : "Pending"
            );
            item.put("decision_action", decision.get("decision_action"));
            item.put("task_id", decision.get("task_id"));
            item.put("po_id", decision.get("po_id"));
            item.put("level", decision.get("level"));
            item.put("domain", decision.get("domain"));


            result.add(item);
        }


        // ==========================================
        // RESPONSE
        // ==========================================

        Map<String, Object> response =
                new HashMap<>();


        response.put(
                "results",
                result
        );


        response.put(
                "page",
                page
        );


        response.put(
                "pageSize",
                pageSize
        );


        response.put(
                "maxItems",
                totalItems
        );


        response.put(
                "totalPage",
                totalPages
        );


        // ==========================================
        // MESSAGE SUMMARY
        // ==========================================

        response.put(
                "utilityMessages",
                utilityMessages
        );


        response.put(
                "marketingMessages",
                marketingMessages
        );


        response.put(
                "authenticationMessages",
                authenticationMessages
        );


        return response;
    }

    /**
     * DB stores UTC wall-clock in timestamp-without-time-zone.
     * Return unambiguous Instant string so every client can show local time.
     */
    private String formatUtcInstant(Timestamp sentAt) {
        if (sentAt == null) {
            return null;
        }
        // Use Instant directly — Timestamp#toLocalDateTime() follows the JVM zone
        // and would falsely re-label IST wall-clock as UTC (e.g. 18:30Z → 00:00Z).
        return sentAt.toInstant().toString();
    }

    /**
     * Match usage message_id → outbound link → approval response.
     * Returns Accepted / Rejected / Pending (never null for UI).
     */
    private Map<String, Object> resolveDecision(String messageId) {
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("decision", "Pending");
        out.put("decision_action", null);
        out.put("task_id", null);
        out.put("po_id", null);
        out.put("level", null);
        out.put("domain", null);

        if (messageId == null || messageId.trim().isEmpty()) {
            return out;
        }

        Optional<WhatsAppMessageLink> linkOpt =
                messageLinkService.findByMessageId(messageId);
        if (!linkOpt.isPresent()) {
            return out;
        }

        WhatsAppMessageLink link = linkOpt.get();
        out.put("task_id", link.getTaskId());
        out.put("po_id", link.getPoId());
        out.put("level", link.getLevel());
        out.put("domain", link.getDomain());

        Optional<WhatsAppResponse> responseOpt =
                responseRepository.findFirstByDomainAndTaskIdAndLevelOrderByIdAsc(
                        link.getDomain(),
                        link.getTaskId(),
                        link.getLevel()
                );

        if (!responseOpt.isPresent()) {
            // Legacy rows without domain
            List<WhatsAppResponse> legacy =
                    responseRepository.findByTaskIdAndLevel(
                            link.getTaskId(),
                            link.getLevel()
                    );
            if (legacy != null && !legacy.isEmpty()) {
                responseOpt = Optional.of(legacy.get(0));
            }
        }

        if (!responseOpt.isPresent()) {
            out.put("decision", "Pending");
            return out;
        }

        String action = responseOpt.get().getAction();
        out.put("decision_action", action);

        if (action != null && "APPROVE".equalsIgnoreCase(action.trim())) {
            out.put("decision", "Accepted");
        } else if (action != null && "REJECT".equalsIgnoreCase(action.trim())) {
            out.put("decision", "Rejected");
        } else if (action == null || action.trim().isEmpty()) {
            out.put("decision", "Pending");
        } else {
            out.put("decision", action);
        }

        return out;
    }

    /**
     * Prefer exact ledger row by message id, then nearest prior snapshot,
     * then live wallet — never return null so the UI cannot go blank.
     */
    private BigDecimal resolveUsageBalance(
            String messageId,
            String domain,
            Timestamp sentAt) {

        return UsageBalanceResolver.resolve(
                messageId,
                domain,
                sentAt,
                new UsageBalanceResolver.Sources() {
                    @Override
                    public Optional<BigDecimal> balanceByReferenceId(String id) {
                        return transactionRepository
                                .findByReferenceId(id)
                                .map(BusinessBalanceTransaction::getBalance);
                    }

                    @Override
                    public Optional<BigDecimal> priorBalance(
                            String d,
                            java.time.LocalDateTime at) {
                        return transactionRepository
                                .findTopByDomainAndTransactionDateLessThanEqualOrderByTransactionDateDescIdDesc(
                                        d,
                                        at
                                )
                                .map(BusinessBalanceTransaction::getBalance);
                    }

                    @Override
                    public Optional<BigDecimal> liveWallet(String d) {
                        return balanceRepository
                                .findById(d)
                                .map(b -> b.getBalance());
                    }
                }
        );
    }
}