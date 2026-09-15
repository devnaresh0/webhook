package whatsapp.webhook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import whatsapp.webhook.entity.PendingWebhookMessage;

import java.util.List;

public interface PendingWebhookMessageRepository
        extends JpaRepository<PendingWebhookMessage, Long> {

    List<PendingWebhookMessage>
    findByDomainAndWhatsappIdAndStatusOrderByCreatedAtAsc(
            String domain,
            String whatsappId,
            String status);
}
