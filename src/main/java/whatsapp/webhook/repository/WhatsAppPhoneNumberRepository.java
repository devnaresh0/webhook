package whatsapp.webhook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import whatsapp.webhook.entity.WhatsAppPhoneNumber;

import java.util.List;
import java.util.Optional;

@Repository
public interface WhatsAppPhoneNumberRepository
        extends JpaRepository<WhatsAppPhoneNumber, Long> {

    List<WhatsAppPhoneNumber> findByDomain(String domain);

    Optional<WhatsAppPhoneNumber> findByPhoneNumberId(String phoneNumberId);
}