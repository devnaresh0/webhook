package whatsapp.webhook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import whatsapp.webhook.entity.BusinessBalanceTransaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessBalanceTransactionRepository
        extends JpaRepository<BusinessBalanceTransaction, Long> {
    Optional<BusinessBalanceTransaction> findByReferenceId(
            String referenceId
    );

    @Query(value =
            "SELECT * " +
                    "FROM business_balance_transactions " +
                    "WHERE domain = :domain " +
                    "AND transaction_date BETWEEN :fromDate AND :toDate " +
                    "ORDER BY transaction_date DESC " +
                    "LIMIT :pageSize OFFSET :offset",
            nativeQuery = true)
    List<BusinessBalanceTransaction> getTransactions(
            @Param("domain") String domain,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("pageSize") int pageSize,
            @Param("offset") int offset
    );

    @Query(value =
            "SELECT COUNT(*) " +
                    "FROM business_balance_transactions " +
                    "WHERE domain = :domain " +
                    "AND transaction_date BETWEEN :fromDate AND :toDate",
            nativeQuery = true)
    long countTransactions(
            @Param("domain") String domain,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );
}