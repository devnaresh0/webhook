package whatsapp.webhook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import whatsapp.webhook.entity.MessagingRate;

import java.util.Optional;

@Repository
public interface MessagingRateRepository
        extends JpaRepository<MessagingRate, Long> {

    Optional<MessagingRate> findFirstByPricingCategoryIgnoreCase(
            String pricingCategory);
    Optional<MessagingRate> findFirstByPricingCategoryIgnoreCaseAndPricingModelIgnoreCase(
            String pricingCategory,
            String pricingModel);


}
