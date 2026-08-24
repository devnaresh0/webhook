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
            String customerPhone
    );

    Optional<CustomerConversation>
    findByMessageId(String messageId);


    // ==============================
    // USAGE DATA
    // ==============================

    @Query(value =
            "SELECT " +
                    "message_id, " +
                    "sent_at, " +
                    "conversation_type, " +
                    "pricing_model, " +
                    "pricing_type, " +
                    "billable, " +
                    "phone_number_id " +
                    "FROM customer_conversations " +
                    "WHERE phone_number_id = :phoneNumberId " +
                    "AND sent_at BETWEEN :fromDate AND :toDate " +
                    "ORDER BY sent_at DESC " +
                    "LIMIT :pageSize OFFSET :offset",
            nativeQuery = true)
    List<Object[]> getUsage(
            @Param("phoneNumberId") String phoneNumberId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("pageSize") int pageSize,
            @Param("offset") int offset
    );


    // ==============================
    // TOTAL USAGE COUNT
    // ==============================

    @Query(value =
            "SELECT COUNT(*) " +
                    "FROM customer_conversations " +
                    "WHERE phone_number_id = :phoneNumberId " +
                    "AND sent_at BETWEEN :fromDate AND :toDate",
            nativeQuery = true)
    long countUsage(
            @Param("phoneNumberId") String phoneNumberId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );


    // ==============================
    // MESSAGE CATEGORY COUNTS
    // ==============================

    @Query(value =
            "SELECT " +
                    "LOWER(conversation_type) AS category, " +
                    "COUNT(*) AS message_count " +
                    "FROM customer_conversations " +
                    "WHERE phone_number_id = :phoneNumberId " +
                    "AND sent_at BETWEEN :fromDate AND :toDate " +
                    "GROUP BY LOWER(conversation_type)",
            nativeQuery = true)
    List<Object[]> countMessagesByCategory(
            @Param("phoneNumberId") String phoneNumberId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );
}