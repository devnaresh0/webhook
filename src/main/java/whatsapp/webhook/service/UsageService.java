package whatsapp.webhook.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import whatsapp.webhook.entity.MessagingRate;
import whatsapp.webhook.repository.CustomerConversationRepository;
import whatsapp.webhook.repository.MessagingRateRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class UsageService {

    @Autowired
    private CustomerConversationRepository conversationRepository;

    @Autowired
    private MessagingRateRepository rateRepository;

    public List<Map<String,Object>> getUsage(
            String fromDate,
            String toDate) {

        LocalDateTime from =
                LocalDate.parse(fromDate).atStartOfDay();

        LocalDateTime to =
                LocalDate.parse(toDate).atTime(23,59,59);

        List<Object[]> rows =
                conversationRepository.getUsage(from,to);

        List<Map<String,Object>> result =
                new ArrayList<>();

        for (Object[] row : rows) {

            String date = row[0].toString();

            String category = (String) row[1];

            String pricingModel = (String) row[2];

            String pricingType = (String) row[3];

            Boolean billable = (Boolean) row[4];

            Long volume = ((Number) row[5]).longValue();

            java.sql.Timestamp sentAt =
                    (java.sql.Timestamp) row[6];

            Double rate =
                    rateRepository
                            .findFirstByPricingCategoryIgnoreCase(category)
                            .map(r -> r.getPricePerConversation().doubleValue())
                            .orElse(0.0);

            Double cost = 0.0;

            if (Boolean.TRUE.equals(billable)
                    && !"free_customer_service".equalsIgnoreCase(pricingType)) {

                cost = rate * volume;
            }
            Map<String, Object> item = new HashMap<>();

            item.put("date", date);
            item.put("sent_at", sentAt);
            item.put("pricing_category", category);
            item.put("pricing_model", pricingModel);
            item.put("pricing_type", pricingType);
            item.put("billable", billable);
            item.put("volume", volume);
            item.put("cost", cost);

            result.add(item);
        }

        return result;
    }
}