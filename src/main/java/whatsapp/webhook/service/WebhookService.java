package whatsapp.webhook.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import whatsapp.webhook.entity.WhatsAppWebhookLog;
import whatsapp.webhook.repository.WhatsAppWebhookLogRepository;

import java.util.List;
import java.util.Map;

@Service
public class WebhookService {

    @Autowired
    private WhatsAppWebhookLogRepository webhookLogRepository;

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

}
