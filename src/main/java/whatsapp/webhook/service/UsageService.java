package whatsapp.webhook.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import whatsapp.webhook.entity.BusinessBalanceTransaction;
import whatsapp.webhook.entity.WhatsAppPhoneNumber;

import whatsapp.webhook.repository.BusinessBalanceRepository;
import whatsapp.webhook.repository.BusinessBalanceTransactionRepository;
import whatsapp.webhook.repository.CustomerConversationRepository;
import whatsapp.webhook.repository.MessagingRateRepository;
import whatsapp.webhook.repository.WhatsAppPhoneNumberRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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


    public Map<String, Object> getUsage(
            String phoneNumberId,
            String fromDate,
            String toDate,
            int page,
            int pageSize) {

        LocalDateTime from =
                LocalDate.parse(fromDate).atStartOfDay();

        LocalDateTime to =
                LocalDate.parse(toDate).atTime(23, 59, 59);


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
        System.out.println("From Date       : [" + from + "]");
        System.out.println("To Date         : [" + to + "]");
        System.out.println("Page            : [" + page + "]");
        System.out.println("Page Size       : [" + pageSize + "]");
        System.out.println("Offset          : [" + offset + "]");
        System.out.println("==========================================");


        List<Object[]> rows =
                conversationRepository.getUsage(
                        phoneNumberId,
                        from,
                        to,
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
                        from,
                        to
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
                        from,
                        to
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
             */

            String messageId =
                    row[0] != null
                            ? row[0].toString()
                            : "";


            java.sql.Timestamp sentAt =
                    (java.sql.Timestamp) row[1];


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


            // ==========================================
            // RATE
            // ==========================================

            Double rate =
                    rateRepository
                            .findFirstByPricingCategoryIgnoreCase(
                                    category
                            )
                            .map(r ->
                                    r.getPricePerConversation()
                                            .doubleValue()
                            )
                            .orElse(0.0);


            // ==========================================
            // COST
            // ==========================================

            Double cost = 0.0;


            if (Boolean.TRUE.equals(billable)
                    && !"free_customer_service"
                    .equalsIgnoreCase(pricingType)) {

                cost = rate;
            }


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
            // HISTORICAL BALANCE
            // ==========================================

            Optional<BusinessBalanceTransaction>
                    transaction =
                    transactionRepository
                            .findByReferenceId(messageId);


            BigDecimal rowBalance =
                    transaction
                            .map(
                                    BusinessBalanceTransaction
                                            ::getBalance
                            )
                            .orElse(null);


            /*
             * For old/free records where no
             * transaction exists, keep balance
             * null rather than using today's balance.
             */
            if (rowBalance == null) {

                rowBalance = null;
            }


            // ==========================================
            // TABLE ITEM
            // ==========================================

            Map<String, Object> item =
                    new HashMap<>();


            item.put(
                    "date",
                    sentAt
            );


            item.put(
                    "sent_at",
                    sentAt
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
}