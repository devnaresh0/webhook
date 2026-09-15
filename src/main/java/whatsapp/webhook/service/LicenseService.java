package whatsapp.webhook.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import whatsapp.webhook.entity.*;
import whatsapp.webhook.model.ActivationRequest;
import whatsapp.webhook.model.ActivationResponse;

import whatsapp.webhook.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class LicenseService {

    @Autowired
    BusinessRepository businessRepository;

    @Autowired
    BusinessCredentialRepository credentialsRepository;
    @Autowired
    private MessagingRateRepository messagingRateRepository;
    @Autowired
    private BusinessBalanceTransactionService balanceTransactionService;


    @Autowired
    BusinessBalanceRepository balanceRepository;

    @Autowired
    WhatsAppPhoneNumberRepository phoneRepository;

    public ActivationResponse activate(ActivationRequest request) {

        ActivationResponse response = new ActivationResponse();

        System.out.println("========== LICENSE ACTIVATION ==========");
        System.out.println("Request Token          : " + request.getToken());
        System.out.println("Request Activation Key : " + request.getActivationKey());
        System.out.println("Request Domain         : " + request.getDomain());

        BusinessCredentials credentials = credentialsRepository
                .findByActivationToken(request.getToken())
                .orElse(null);

        if (credentials == null) {
            System.out.println("ERROR : Invalid Token");

            response.setSuccess(false);
            response.setMessage("Invalid Token");
            return response;
        }
        System.out.println("DB Activation Key : " + credentials.getActivationKey());
        System.out.println("Request Activation Key : " + request.getActivationKey());
        System.out.println("---------- DATABASE VALUES ----------");
        System.out.println("DB Token          : " + credentials.getActivationToken());
        System.out.println("DB Domain         : " + credentials.getDomain());
        System.out.println("DB Active         : " + credentials.getLicenseStatus());
        if (!credentials.getActivationKey().equals(request.getActivationKey())) {

            System.out.println("ERROR : Activation Key Mismatch");

            response.setSuccess(false);
            response.setMessage("Invalid Activation Key");
            return response;
        }
        // =========================================
// SAVE STATIC IP CONFIGURATION
// =========================================

        credentials.setStaticIp(
                request.getStaticIpEnabled()
        );

        credentials.setIpAddress(
                request.getStaticIpUrl()
        );

        credentialsRepository.save(credentials);

        System.out.println(
                "Static IP Enabled : "
                        + credentials.getStaticIp()
        );

        System.out.println(
                "Static IP Address : "
                        + credentials.getIpAddress()
        );

        if (!credentials.getDomain().equalsIgnoreCase(request.getDomain())) {

            System.out.println("ERROR : Domain Mismatch");

            response.setSuccess(false);
            response.setMessage("Domain Mismatch");
            return response;
        }

        if (!"ACTIVE".equalsIgnoreCase(credentials.getLicenseStatus())) {

            System.out.println("ERROR : License Disabled");

            response.setSuccess(false);
            response.setMessage("License Disabled");
            return response;
        }
        System.out.println("SUCCESS : License Activated Successfully");

        response.setSuccess(true);
        response.setMessage("License Activated Successfully");
        Business business = businessRepository.findById(credentials.getDomain())
                .orElseThrow(() -> new RuntimeException("Business not found"));

        response.setCompanyName(business.getBusinessName());

        return response;
    }


    public Business getBusinessByDomain(String domain) {

        return businessRepository.findById(domain)
                .orElseThrow(() -> new RuntimeException("Business not found"));
    }
    public void saveFreeUsageTransaction(
            String phoneNumberId,
            String pricingCategory,
            String pricingType,
            int messages,
            String messageId) {

        WhatsAppPhoneNumber phone =
                phoneRepository
                        .findByPhoneNumberId(phoneNumberId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Phone number not found"));

        BusinessBalance balance =
                balanceRepository
                        .findById(phone.getDomain())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Business balance not found"));

        if (balance.getBalance() == null) {
            balance.setBalance(BigDecimal.ZERO);
        }

        BigDecimal currentBalance =
                balance.getBalance();


        /*
         * No deduction for free customer service.
         * Balance remains unchanged.
         */

        balanceTransactionService.saveUsage(
                phone.getDomain(),
                pricingCategory,
                pricingType,
                messages,
                BigDecimal.ZERO,
                currentBalance,       // <-- BALANCE IS PASSED HERE
                messageId
        );

        System.out.println("======================================");
        System.out.println("Free Usage Transaction Saved");
        System.out.println("Message Id      : " + messageId);
        System.out.println("Category        : " + pricingCategory);
        System.out.println("Pricing Type    : " + pricingType);
        System.out.println("Cost            : 0.00");
        System.out.println("Balance         : " + currentBalance);
        System.out.println("======================================");
    }
    public void deductBalance(
            String phoneNumberId,
            double amount,
            String pricingCategory,
            String pricingType,
            int messages,
            String messageId) {

        WhatsAppPhoneNumber phone =
                phoneRepository
                        .findByPhoneNumberId(phoneNumberId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Phone number not found"));

        BusinessBalance balance =
                balanceRepository
                        .findById(phone.getDomain())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Business balance not found"));

        if (balance.getBalance() == null) {
            balance.setBalance(BigDecimal.ZERO);
        }

        BigDecimal currentBalance =
                balance.getBalance();

        BigDecimal deduction =
                BigDecimal.valueOf(amount);

        BigDecimal newBalance =
                currentBalance.subtract(deduction);

        balance.setBalance(newBalance);
        balance.setLastUpdated(LocalDateTime.now(ZoneOffset.UTC));

        balanceRepository.save(balance);

        // Save EVERY usage transaction,
        // including zero-cost usage.
        balanceTransactionService.saveUsage(
                phone.getDomain(),
                pricingCategory,
                pricingType,
                messages,
                deduction,
                newBalance,
                messageId
        );

        System.out.println(
                "Usage transaction saved. " +
                        "Cost = " + deduction +
                        ", Balance = " + newBalance);
    }

    public Double getBalance(String domain) {

        Map<String, Object> details =
                getBalanceDetails(domain);

        if (details == null) {
            throw new RuntimeException(
                    "Business balance not found"
            );
        }

        return ((Number) details.get("balance"))
                .doubleValue();
    }
    public Map<String, Object> getBalanceDetails(String domain) {

        BusinessBalance row =
                balanceRepository
                        .findById(domain)
                        .orElse(null);

        if (row == null) {
            return null;
        }

        Map<String, Object> result =
                new HashMap<String, Object>();

        result.put("domain", domain);

        double amount = 0.0;
        if (row.getBalance() != null) {
            amount = row.getBalance().doubleValue();
        }
        result.put("balance", amount);

        String currency = "INR";
        if (row.getCurrency() != null
                && !row.getCurrency().trim().isEmpty()) {
            currency = row.getCurrency().trim();
        }
        result.put("currency", currency);

        result.put("lastUpdated", row.getLastUpdated());

        return result;
    }

    public void processConversationCharge(
            String phoneNumberId,
            String pricingCategory,
            String pricingType,
            String messageId) {

        MessagingRate rate =
                messagingRateRepository
                        .findFirstByPricingCategoryIgnoreCase(
                                pricingCategory)
                        .orElse(null);

        if (rate == null) {
            System.out.println("======================================");
            System.out.println("No Usage Transaction");
            System.out.println("Reason : Rate not configured");
            System.out.println("Category : " + pricingCategory);
            System.out.println("Message Id : " + messageId);
            System.out.println("======================================");
            return;
        }

        BigDecimal price = rate.getPricePerConversation();

        if (price == null || price.compareTo(BigDecimal.ZERO) == 0) {
            System.out.println("======================================");
            System.out.println("No Usage Transaction");
            System.out.println("Reason : Rate value is empty / zero");
            System.out.println("Category : " + pricingCategory);
            System.out.println("Price    : " + price);
            System.out.println("Message Id : " + messageId);
            System.out.println("======================================");
            return;
        }

        deductBalance(
                phoneNumberId,
                price.doubleValue(),
                pricingCategory,
                pricingType,
                1,
                messageId
        );
    }
    public boolean isLicenseActive(String domain) {

        BusinessCredentials credentials = credentialsRepository
                .findByDomain(domain)
                .orElse(null);

        if (credentials == null) {
            return false;
        }

        if (!"ACTIVE".equalsIgnoreCase(credentials.getLicenseStatus())) {
            return false;
        }

        return true;
    }
    public boolean areWhatsAppCredentialsValid(
            String domain,
            String phoneNumberId,
            String accessToken) {

        BusinessCredentials credentials =
                credentialsRepository
                        .findByDomain(domain)
                        .orElse(null);

        if (credentials == null) {

            System.out.println(
                    "❌ No credentials found for domain: "
                            + domain
            );

            return false;
        }

        // Phone Number ID is stored in activation_key
        boolean phoneNumberMatches =
                phoneNumberId.equals(
                        credentials.getActivationKey()
                );

        // WhatsApp Access Token is stored in activation_token
        boolean tokenMatches =
                accessToken.equals(
                        credentials.getActivationToken()
                );

        System.out.println(
                "Phone Number ID matches = "
                        + phoneNumberMatches
        );

        System.out.println(
                "Access Token matches = "
                        + tokenMatches
        );

        return phoneNumberMatches && tokenMatches;
    }
}
