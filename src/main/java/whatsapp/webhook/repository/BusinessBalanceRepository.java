package whatsapp.webhook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import whatsapp.webhook.entity.BusinessBalance;

@Repository
public interface BusinessBalanceRepository extends JpaRepository<BusinessBalance, String> {
}