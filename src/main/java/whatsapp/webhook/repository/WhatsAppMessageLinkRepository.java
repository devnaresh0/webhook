package whatsapp.webhook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import whatsapp.webhook.entity.WhatsAppMessageLink;

import java.util.Optional;

@Repository
public interface WhatsAppMessageLinkRepository
        extends JpaRepository<WhatsAppMessageLink, Long> {

    Optional<WhatsAppMessageLink> findByMessageId(String messageId);

    Optional<WhatsAppMessageLink> findByDomainAndTaskIdAndPoIdAndLevel(
            String domain,
            String taskId,
            String poId,
            Integer level
    );
}
