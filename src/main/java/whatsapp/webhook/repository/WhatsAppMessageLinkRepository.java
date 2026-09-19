package whatsapp.webhook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import whatsapp.webhook.entity.WhatsAppMessageLink;

import java.util.List;
import java.util.Optional;

@Repository
public interface WhatsAppMessageLinkRepository
        extends JpaRepository<WhatsAppMessageLink, Long> {

    Optional<WhatsAppMessageLink> findByMessageId(String messageId);

    List<WhatsAppMessageLink> findByDomainAndPoIdAndLevelAndRecipientUserIdAndActiveTrue(
            String domain,
            String poId,
            Integer level,
            String recipientUserId
    );

    List<WhatsAppMessageLink> findByDomainAndPoIdAndActiveTrueAndLevelLessThan(
            String domain,
            String poId,
            Integer level
    );

    boolean existsByDomainAndPoIdAndActiveTrueAndLevelGreaterThan(
            String domain,
            String poId,
            Integer level
    );

    boolean existsByDomainAndPoIdAndLevelGreaterThan(
            String domain,
            String poId,
            Integer level
    );
}
