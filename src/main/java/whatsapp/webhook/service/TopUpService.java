package whatsapp.webhook.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import whatsapp.webhook.entity.BusinessBalance;
import whatsapp.webhook.entity.BusinessBalanceTransaction;
import whatsapp.webhook.repository.BusinessBalanceRepository;
import whatsapp.webhook.repository.BusinessBalanceTransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class TopUpService {

    @Autowired
    private BusinessBalanceRepository businessBalanceRepository;

    @Autowired
    private BusinessBalanceTransactionRepository transactionRepository;


    // =====================================================
    // PROCESS TOP-UP
    // =====================================================

    @Transactional
    public Map<String, Object> processTopUp(
            String domain,
            BigDecimal amount,
            String mobileNumber,
            String referenceId,
            String status) {


        Map<String, Object> result =
                new HashMap<>();


        // =================================================
        // 1. CHECK IF REFERENCE ALREADY EXISTS
        // =================================================

        Optional<BusinessBalanceTransaction> existing =
                transactionRepository
                        .findByReferenceId(
                                referenceId
                        );


        if (existing.isPresent()) {

            BusinessBalanceTransaction existingTransaction =
                    existing.get();


            System.out.println(
                    "⚠️ TOP-UP REFERENCE ALREADY EXISTS"
            );

            System.out.println(
                    "Reference ID = "
                            + referenceId
            );


            result.put(
                    "success",
                    true
            );

            result.put(
                    "message",
                    "Top-up already processed"
            );

            result.put(
                    "referenceId",
                    referenceId
            );

            result.put(
                    "status",
                    existingTransaction.getStatus()
            );

            result.put(
                    "balance",
                    existingTransaction.getBalance()
            );

            return result;
        }


        // =================================================
        // 2. FIND BUSINESS BALANCE
        // =================================================

        BusinessBalance businessBalance =
                businessBalanceRepository
                        .findByDomain(domain)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Business balance not found for domain: "
                                                + domain
                                )
                        );


        BigDecimal currentBalance =
                businessBalance.getBalance();


        if (currentBalance == null) {

            currentBalance =
                    BigDecimal.ZERO;
        }


        // =================================================
        // 3. FAILED PAYMENT
        // =================================================

        if ("FAILED".equalsIgnoreCase(status)) {

            BusinessBalanceTransaction transaction =
                    new BusinessBalanceTransaction();

            transaction.setDomain(domain);

            transaction.setTransactionDate(
                    LocalDateTime.now()
            );

            transaction.setDataType(
                    "Load"
            );

            transaction.setPricingCategory(
                    "-"
            );

            transaction.setPricingType(
                    "-"
            );

            transaction.setMessages(
                    null
            );

            transaction.setLoadAmount(
                    amount
            );

            transaction.setCost(
                    null
            );

            // Balance stays unchanged
            transaction.setBalance(
                    currentBalance
            );

            transaction.setReferenceId(
                    referenceId
            );

            transaction.setStatus(
                    "FAILED"
            );

            transaction.setCreatedAt(
                    LocalDateTime.now()
            );


            transactionRepository.save(
                    transaction
            );


            System.out.println(
                    "❌ TOP-UP FAILED"
            );

            System.out.println(
                    "Domain = " + domain
            );

            System.out.println(
                    "Amount = " + amount
            );

            System.out.println(
                    "Balance unchanged = "
                            + currentBalance
            );


            result.put(
                    "success",
                    false
            );

            result.put(
                    "message",
                    "Top-up failed"
            );

            result.put(
                    "status",
                    "FAILED"
            );

            result.put(
                    "balance",
                    currentBalance
            );

            result.put(
                    "referenceId",
                    referenceId
            );

            return result;
        }


        // =================================================
        // 4. SUCCESS PAYMENT
        // =================================================

        BigDecimal newBalance =
                currentBalance.add(
                        amount
                );


        // =================================================
        // 5. UPDATE BUSINESS BALANCE
        // =================================================

        businessBalance.setBalance(
                newBalance
        );

        businessBalanceRepository.save(
                businessBalance
        );


        // =================================================
        // 6. SAVE LOAD TRANSACTION
        // =================================================

        BusinessBalanceTransaction transaction =
                new BusinessBalanceTransaction();

        transaction.setDomain(
                domain
        );

        transaction.setTransactionDate(
                LocalDateTime.now()
        );

        transaction.setDataType(
                "Load"
        );

        transaction.setPricingCategory(
                "-"
        );

        transaction.setPricingType(
                "-"
        );

        transaction.setMessages(
                null
        );

        transaction.setLoadAmount(
                amount
        );

        transaction.setCost(
                null
        );

        transaction.setBalance(
                newBalance
        );

        transaction.setReferenceId(
                referenceId
        );

        transaction.setStatus(
                "SUCCESS"
        );

        transaction.setCreatedAt(
                LocalDateTime.now()
        );


        transactionRepository.save(
                transaction
        );


        // =================================================
        // 7. LOG
        // =================================================

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "✅ TOP-UP SUCCESS"
        );

        System.out.println(
                "Domain = " + domain
        );

        System.out.println(
                "Amount = " + amount
        );

        System.out.println(
                "Old Balance = " + currentBalance
        );

        System.out.println(
                "New Balance = " + newBalance
        );

        System.out.println(
                "Reference ID = " + referenceId
        );

        System.out.println(
                "=========================================="
        );


        // =================================================
        // 8. RESPONSE
        // =================================================

        result.put(
                "success",
                true
        );

        result.put(
                "message",
                "Top-up successful"
        );

        result.put(
                "status",
                "SUCCESS"
        );

        result.put(
                "amount",
                amount
        );

        result.put(
                "balance",
                newBalance
        );

        result.put(
                "referenceId",
                referenceId
        );

        return result;
    }
}
