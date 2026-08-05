package whatsapp.webhook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import whatsapp.webhook.entity.BusinessCredentials;

import java.util.Optional;

@Repository
public interface BusinessCredentialRepository
        extends JpaRepository<BusinessCredentials, Long> {
    Optional<BusinessCredentials> findByDomain(String domain);

    Optional<BusinessCredentials> findByActivationToken(String activationToken);
}
