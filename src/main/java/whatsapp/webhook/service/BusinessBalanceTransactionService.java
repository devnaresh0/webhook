package whatsapp.webhook.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import whatsapp.webhook.entity.BusinessBalanceTransaction;
import whatsapp.webhook.repository.BusinessBalanceTransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setDataType("Op. Bal");

        transaction.setPricingCategory("-");
        transaction.setPricingType("-");
        transaction.setMessages(null);
        transaction.setLoadAmount(null);
        transaction.setCost(null);

        transaction.setBalance(balance);

        transaction.setCreatedAt(LocalDateTime.now());

        transactionRepository.save(transaction);
    }


    public void saveLoad(
            String domain,
            BigDecimal loadAmount,
            BigDecimal balance) {

        BusinessBalanceTransaction transaction =
                new BusinessBalanceTransaction();

        transaction.setDomain(domain);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setDataType("Load");

        transaction.setPricingCategory("-");
        transaction.setPricingType("-");
        transaction.setMessages(null);

        transaction.setLoadAmount(loadAmount);
        transaction.setCost(null);

        transaction.setBalance(balance);

        transaction.setCreatedAt(LocalDateTime.now());

        transactionRepository.save(transaction);
    }


    public void saveUsage(
            String domain,
            String pricingCategory,
            String pricingType,
            Integer messages,
            BigDecimal cost,
            BigDecimal balance,
            String referenceId) {

        BusinessBalanceTransaction transaction =
                new BusinessBalanceTransaction();

        transaction.setDomain(domain);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setDataType("Usage");

        transaction.setPricingCategory(pricingCategory);
        transaction.setPricingType(pricingType);
        transaction.setMessages(messages);

        transaction.setLoadAmount(null);
        transaction.setCost(cost);
        transaction.setBalance(balance);

        transaction.setReferenceId(referenceId);
        transaction.setCreatedAt(LocalDateTime.now());

        transactionRepository.save(transaction);
    }
}