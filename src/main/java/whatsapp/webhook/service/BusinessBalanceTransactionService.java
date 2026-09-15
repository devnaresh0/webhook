package whatsapp.webhook.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import whatsapp.webhook.entity.BusinessBalanceTransaction;
import whatsapp.webhook.repository.BusinessBalanceTransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
public class BusinessBalanceTransactionService {

    @Autowired
    private BusinessBalanceTransactionRepository transactionRepository;


    public void saveOpeningBalance(
            String domain,
            BigDecimal balance) {

        BusinessBalanceTransaction transaction =
                new BusinessBalanceTransaction();

        transaction.setDomain(domain);
        transaction.setTransactionDate(LocalDateTime.now(ZoneOffset.UTC));
        transaction.setDataType("Op. Bal");

        transaction.setPricingCategory("-");
        transaction.setPricingType("-");
        transaction.setMessages(null);
        transaction.setLoadAmount(null);
        transaction.setCost(null);

        transaction.setBalance(
                balance != null ? balance : BigDecimal.ZERO
        );

        transaction.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));

        transactionRepository.save(transaction);
    }


    public void saveLoad(
            String domain,
            BigDecimal loadAmount,
            BigDecimal balance) {

        BusinessBalanceTransaction transaction =
                new BusinessBalanceTransaction();

        transaction.setDomain(domain);
        transaction.setTransactionDate(LocalDateTime.now(ZoneOffset.UTC));
        transaction.setDataType("Load");

        transaction.setPricingCategory("-");
        transaction.setPricingType("-");
        transaction.setMessages(null);

        transaction.setLoadAmount(loadAmount);
        transaction.setCost(null);

        transaction.setBalance(
                balance != null ? balance : BigDecimal.ZERO
        );

        transaction.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));

        transactionRepository.save(transaction);
    }


    @Transactional
    public void saveUsage(
            String domain,
            String pricingCategory,
            String pricingType,
            Integer messages,
            BigDecimal cost,
            BigDecimal balance,
            String referenceId) {

        // =====================================================
        // 1. CHECK DUPLICATE REFERENCE ID
        // =====================================================

        if (referenceId != null &&
                !referenceId.trim().isEmpty()) {

            Optional<BusinessBalanceTransaction> existing =
                    transactionRepository
                            .findByReferenceId(referenceId.trim());

            if (existing.isPresent()) {

                System.out.println(
                        "⚠️ Usage already exists for referenceId: "
                                + referenceId
                );

                System.out.println(
                        "⚠️ Duplicate usage transaction NOT saved."
                );

                return;
            }
        }


        // =====================================================
        // 2. CREATE NEW USAGE TRANSACTION
        // =====================================================

        BusinessBalanceTransaction transaction =
                new BusinessBalanceTransaction();

        transaction.setDomain(domain);

        transaction.setTransactionDate(
                LocalDateTime.now(ZoneOffset.UTC)
        );

        transaction.setDataType("Usage");

        transaction.setPricingCategory(
                pricingCategory
        );

        transaction.setPricingType(
                pricingType
        );

        transaction.setMessages(
                messages
        );

        transaction.setLoadAmount(null);

        transaction.setCost(
                cost != null ? cost : BigDecimal.ZERO
        );

        transaction.setBalance(
                balance != null ? balance : BigDecimal.ZERO
        );

        transaction.setReferenceId(
                referenceId != null && !referenceId.trim().isEmpty()
                        ? referenceId.trim()
                        : null
        );

        transaction.setCreatedAt(
                LocalDateTime.now(ZoneOffset.UTC)
        );


        // =====================================================
        // 3. SAVE (unique index is the race-safe guard)
        // =====================================================

        try {
            transactionRepository.save(transaction);
        } catch (DataIntegrityViolationException e) {
            System.out.println(
                    "⚠️ Duplicate usage ignored (unique reference_id): "
                            + referenceId
            );
            return;
        }

        System.out.println(
                "✅ Usage transaction saved."
        );

        System.out.println(
                "Reference ID = "
                        + referenceId
        );

        System.out.println(
                "Balance = "
                        + transaction.getBalance()
        );
    }
}