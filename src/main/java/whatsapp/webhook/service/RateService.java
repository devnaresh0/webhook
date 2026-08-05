package whatsapp.webhook.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import whatsapp.webhook.entity.MessagingRate;
import whatsapp.webhook.model.MessagingRateResponse;
import whatsapp.webhook.repository.MessagingRateRepository;

@Service
public class RateService {

    @Autowired
    private MessagingRateRepository messagingRateRepository;

    public Double getRate(String category) {

        return messagingRateRepository
                .findFirstByPricingCategoryIgnoreCase(category)
                .map(rate -> rate.getPricePerConversation().doubleValue())
                .orElse(0.0);
    }
}