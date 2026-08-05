package whatsapp.webhook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import whatsapp.webhook.entity.Business;

@Repository
public interface BusinessRepository extends JpaRepository<Business, String> {
}