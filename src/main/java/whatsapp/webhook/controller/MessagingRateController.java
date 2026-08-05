package whatsapp.webhook.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import whatsapp.webhook.entity.MessagingRate;
import whatsapp.webhook.repository.MessagingRateRepository;
@RestController
@RequestMapping("/api/rates")
public class MessagingRateController {

    @Autowired
    private MessagingRateRepository messagingRateRepository;

    @GetMapping("/{category}")
    public ResponseEntity<Double> getRate(@PathVariable String category) {

        MessagingRate rate = messagingRateRepository
                .findFirstByPricingCategoryIgnoreCase(category)
                .orElse(null);

        if (rate == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(rate.getPricePerConversation().doubleValue());
    }
}