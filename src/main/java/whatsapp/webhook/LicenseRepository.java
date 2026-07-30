package whatsapp.webhook;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LicenseRepository extends JpaRepository<License, Long> {


    Optional<License> findByDomain(String domain);

    License findByToken(String token);

    Optional<License> findByPhoneNumberId(String phoneNumberId);

}
