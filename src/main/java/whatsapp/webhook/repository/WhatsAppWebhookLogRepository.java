package whatsapp.webhook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import whatsapp.webhook.entity.WhatsAppWebhookLog;

@Repository
public interface WhatsAppWebhookLogRepository
        extends JpaRepository<WhatsAppWebhookLog, Long> {

}
