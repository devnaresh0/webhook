package whatsapp.webhook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import whatsapp.webhook.entity.CustomerConversation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerConversationRepository
        extends JpaRepository<CustomerConversation, Long> {

    Optional<CustomerConversation>
    findTopByPhoneNumberIdAndCustomerPhoneOrderByWindowExpiresAtDesc(
            String phoneNumberId,
            String customerPhone);

    Optional<CustomerConversation> findByMessageId(String messageId);

    @Query(value =
            "SELECT DATE(sent_at), " +
                    "conversation_type, " +
                    "pricing_model, " +
                    "pricing_type, " +
                    "billable, " +
                    "COUNT(*), " +
                    "MAX(sent_at) " +
                    "FROM customer_conversations " +
                    "WHERE sent_at BETWEEN :fromDate AND :toDate " +
                    "GROUP BY DATE(sent_at), conversation_type, pricing_model, pricing_type, billable " +
                    "ORDER BY MAX(sent_at) DESC",
            nativeQuery = true)
    List<Object[]> getUsage(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);
}