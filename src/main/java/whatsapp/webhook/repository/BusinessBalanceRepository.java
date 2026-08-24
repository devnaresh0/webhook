package whatsapp.webhook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import whatsapp.webhook.entity.BusinessBalance;

import java.util.Optional;

@Repository
public interface BusinessBalanceRepository extends JpaRepository<BusinessBalance, String> {
    Optional<BusinessBalance> findByDomain(String domain);
}