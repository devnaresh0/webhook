package whatsapp.webhook.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import whatsapp.webhook.repository.CustomerConversationRepository;
import whatsapp.webhook.repository.MessagingRateRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class UsageService {

    @Autowired
    private CustomerConversationRepository conversationRepository;

    @Autowired
    private MessagingRateRepository rateRepository;

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

        // Page is 1-based
        int offset = (page - 1) * pageSize;

        List<Object[]> rows =
                conversationRepository.getUsage(
                        phoneNumberId,
                        from,
                        to,
                        pageSize,
                        offset);

        long totalItems =
                conversationRepository.countUsage(
                        phoneNumberId,
                        from,
                        to);

        int totalPages =
                (int) Math.ceil(
                        (double) totalItems / pageSize);

        List<Map<String, Object>> result =
                new ArrayList<>();

        for (Object[] row : rows) {

            java.sql.Timestamp sentAt =
                    (java.sql.Timestamp) row[0];

            String category =
                    (String) row[1];

            String pricingModel =
                    (String) row[2];

            String pricingType =
                    (String) row[3];

            Boolean billable =
                    (Boolean) row[4];

            String rowPhoneNumberId =
                    row[5] != null
                            ? row[5].toString()
                            : "";

            Double rate =
                    rateRepository
                            .findFirstByPricingCategoryIgnoreCase(category)
                            .map(r -> r.getPricePerConversation().doubleValue())
                            .orElse(0.0);

            Double cost = 0.0;

            if (Boolean.TRUE.equals(billable)
                    && !"free_customer_service"
                    .equalsIgnoreCase(pricingType)) {

                cost = rate;
            }

            Map<String, Object> item =
                    new HashMap<>();

            item.put("date", sentAt);
            item.put("sent_at", sentAt);
            item.put("pricing_category", category);
            item.put("pricing_model", pricingModel);
            item.put("pricing_type", pricingType);
            item.put("billable", billable);
            item.put("volume", 1);
            item.put("cost", cost);
            item.put("phone_number_id", rowPhoneNumberId);

            result.add(item);
        }

        Map<String, Object> response =
                new HashMap<>();

        response.put("results", result);
        response.put("page", page);
        response.put("pageSize", pageSize);
        response.put("maxItems", totalItems);
        response.put("totalPage", totalPages);

        return response;
    }
}