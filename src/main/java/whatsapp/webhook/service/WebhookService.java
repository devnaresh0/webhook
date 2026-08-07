package whatsapp.webhook.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import whatsapp.webhook.entity.CustomerConversation;
import whatsapp.webhook.entity.WhatsAppWebhookLog;
import whatsapp.webhook.repository.CustomerConversationRepository;
//import whatsapp.webhook.repository.WhatsAppMessageRepository;
import whatsapp.webhook.repository.WhatsAppWebhookLogRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class WebhookService {
//    @Autowired
//    private WhatsAppMessageRepository whatsAppMessageRepository;
    @Autowired
    private WhatsAppWebhookLogRepository webhookLogRepository;
    @Autowired
    private CustomerConversationRepository conversationRepository;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private BillingService billingService;

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Entry point called by WebhookController
     */
    public void processWebhook(Map<String, Object> payload) {

        try {

            System.out.println("========== INCOMING WEBHOOK ==========");

            saveWebhook(payload);

            processEntries(payload);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Save raw webhook payload
     */
    private void saveWebhook(Map<String, Object> payload) {

        try {

            WhatsAppWebhookLog log = new WhatsAppWebhookLog();

            log.setPayload(objectMapper.writeValueAsString(payload));

            webhookLogRepository.save(log);

        } catch (Exception ex) {

            ex.printStackTrace();

        }

    }

    /**
     * Loop through all entries
     */
    private void processEntries(Map<String, Object> payload) {

        List<Map<String, Object>> entries =
                (List<Map<String, Object>>) payload.get("entry");

        if (entries == null || entries.isEmpty()) {
            return;
        }

        for (Map<String, Object> entry : entries) {

            processEntry(entry);

        }

    }

    /**
     * Process one entry
     */
    private void processEntry(Map<String, Object> entry) {

        List<Map<String, Object>> changes =
                (List<Map<String, Object>>) entry.get("changes");

        if (changes == null || changes.isEmpty()) {
            return;
        }

        for (Map<String, Object> change : changes) {

            processChange(change);

        }

    }

    /**
     * Process one change
     */
    private void processChange(Map<String, Object> change) {

        Map<String, Object> value =
                (Map<String, Object>) change.get("value");

        if (value == null) {
            return;
        }

        processValue(value);

    }


    /**
     * Decide what kind of webhook this is
     */
    private void processValue(Map<String, Object> value) {

        saveConversation(value);

        List<Map<String, Object>> messages =
                (List<Map<String, Object>>) value.get("messages");

        if (messages != null && !messages.isEmpty()) {
            conversationService.processMessages(value);
        }

        List<Map<String, Object>> statuses =
                (List<Map<String, Object>>) value.get("statuses");

        if (statuses != null && !statuses.isEmpty()) {
            billingService.processStatuses(value);
        }
    }
    @SuppressWarnings("unchecked")
    private void saveConversation(Map<String, Object> value) {

        try {

            List<Map<String, Object>> statuses =
                    (List<Map<String, Object>>) value.get("statuses");

            if (statuses == null || statuses.isEmpty()) {
                return;
            }

            Map<String, Object> status = statuses.get(0);

            Map<String, Object> metadata =
                    (Map<String, Object>) value.get("metadata");

            Map<String, Object> pricing =
                    (Map<String, Object>) status.get("pricing");
            System.out.println("Billable      : " + pricing.get("billable"));
            System.out.println("Category      : " + pricing.get("category"));
            System.out.println("Pricing Model : " + pricing.get("pricing_model"));
            System.out.println("Type          : " + pricing.get("type"));

            String phoneNumberId = metadata != null
                    ? String.valueOf(metadata.get("phone_number_id"))
                    : null;

            String customerPhone = String.valueOf(status.get("recipient_id"));
            String messageId = String.valueOf(status.get("id"));
            String statusValue = String.valueOf(status.get("status"));
            if (!"sent".equalsIgnoreCase(statusValue)
                    && !"delivered".equalsIgnoreCase(statusValue)) {
                return;
            }

            boolean billable = false;
            String category = null;
            String pricingModel = null;
            String pricingType = null;

            if (pricing != null) {
                billable = Boolean.parseBoolean(String.valueOf(pricing.get("billable")));
                category = String.valueOf(pricing.get("category"));
                pricingModel = String.valueOf(pricing.get("pricing_model"));
                pricingType = String.valueOf(pricing.get("type"));
            }

            // Always create a NEW row
            CustomerConversation conversation =
                    conversationRepository
                            .findByMessageId(messageId)
                            .orElse(new CustomerConversation());
            if ("sent".equalsIgnoreCase(statusValue)) {
                conversation.setIsSent(1);
            }

            if ("delivered".equalsIgnoreCase(statusValue)) {
                conversation.setIsDelivered(1);
            }
            if ("sent".equalsIgnoreCase(statusValue)) {
                conversation.setSentAt(LocalDateTime.now());
            }

            if ("delivered".equalsIgnoreCase(statusValue)) {
                conversation.setDeliveredAt(LocalDateTime.now());
            }
//
//            WhatsAppMessage message = new WhatsAppMessage();
//
//            message.setMessageId(messageId);          // same Meta message id
//            message.setPhoneNumberId(phoneNumberId);
//            message.setRecipientPhone(customerPhone);
//            message.setDirection("OUTGOING");         // or INCOMING
//            message.setMessageType("text");           // extract from webhook
//            message.setMessageBody(body);             // if available
//            message.setMediaUrl(mediaUrl);            // if available // same m// <-- add this
            conversation.setPhoneNumberId(phoneNumberId);
            conversation.setCustomerPhone(customerPhone);
            conversation.setMessageId(messageId);
            conversation.setStatus(statusValue);
            conversation.setConversationType(category);
            conversation.setPricingModel(pricingModel);
            conversation.setPricingType(pricingType);
            conversation.setBillable(billable);

            if (conversation.getId() == null) {
                conversation.setWindowOpenedAt(LocalDateTime.now());
                conversation.setWindowExpiresAt(LocalDateTime.now().plusHours(24));
            }

            conversationRepository.save(conversation);
//            System.out.println("===== SAVING WHATSAPP MESSAGE =====");
//            System.out.println("Message ID: " + message.getMessageId());
//            System.out.println("Phone: " + message.getRecipientPhone());

//            whatsAppMessageRepository.save(message);

            System.out.println("===== MESSAGE SAVED =====");

            System.out.println("Conversation saved successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
