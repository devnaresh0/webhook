package whatsapp.webhook.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import whatsapp.webhook.entity.BusinessCredentials;
import whatsapp.webhook.entity.PendingWebhookMessage;
import whatsapp.webhook.model.WhatsAppResponse;
import whatsapp.webhook.repository.BusinessCredentialRepository;
import whatsapp.webhook.repository.PendingWebhookMessageRepository;
import whatsapp.webhook.repository.WhatsAppResponseRepository;

import java.util.*;

@Service
public class ApprovalService {

    @Autowired
    private WhatsAppResponseRepository repository;
    @Autowired
    private BusinessCredentialRepository businessCredentialsRepository;
    @Autowired
    private ObjectMapper objectMapper;



    @Autowired
    private PendingWebhookMessageRepository pendingWebhookMessageRepository;



    // ================= MAIN METHOD =================
    public void processApproval(String phone,
                                String action,
                                Map<String, Object> responseJson,
                                String userName) {

        try {
            System.out.println("========== PROCESSING APPROVAL ==========");

            String token = (String) ((Map) responseJson).get("flow_token");

            if (token == null || !token.contains("|")) {
                throw new RuntimeException("Invalid flow_token");
            }

            String[] parts = token.split("\\|");

            if (parts.length < 10) {
                throw new RuntimeException("Invalid token structure: " + token);
            }

            String taskId = parts[0];
            int userId = Integer.parseInt(parts[1]);
            long poId = Long.parseLong(parts[2]);
            String poNumber = parts[3];
            int menuId = Integer.parseInt(parts[4]);
            String createdBy = parts[5];
            int level = Integer.parseInt(parts[6]);
            String domain = parts[7];
            int tenantId = Integer.parseInt(parts[8]);
            int localId = Integer.parseInt(parts[9]);

            // -------- DUPLICATE CLICK --------
            if (repository.existsByTaskIdAndLevel(taskId, level)) {

                List<WhatsAppResponse> list =
                        repository.findByTaskIdAndLevel(taskId, level);

                if (list.isEmpty()) {
                    return;
                }

                String approvedBy = list.get(0).getUserName();

                sendWhatsAppMessage(
                        domain,
                        phone,
                        poNumber + " ✅ Already approved at Level " + level
                                + " by " + approvedBy
                );
                return;
            }

            // -------- NEW APPROVAL --------
            WhatsAppResponse entity = new WhatsAppResponse();
            entity.setPhone(phone);
            entity.setPoId(String.valueOf(poId));
            entity.setAction(action);
            entity.setTaskId(taskId);
            entity.setUserName(createdBy);    // from token parts[5] — use this in messages
            entity.setLevel(level);
            entity.setResponseJson(objectMapper.writeValueAsString(responseJson));

            repository.save(entity);

            callExternalApi(
                    domain, tenantId, localId, taskId, userId,
                    poId, poNumber, menuId, action, createdBy, level
            );

            notifyOthers(domain, poId, level, phone, userName);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= CALL MAIN APP =================
    private void callExternalApi(
            String domain,
            int tenantId,
            int localId,
            String taskId,
            int userId,
            long poId,
            String poNumber,
            int menuId,
            String action,
            String createdBy,
            int level) {

        try {

            // =================================================
            // 1. VALIDATE DOMAIN
            // =================================================

            if (domain == null || domain.trim().isEmpty()) {

                System.out.println("❌ Domain is empty.");
                return;
            }

            System.out.println("==========================================");
            System.out.println("CALLING MAIN APPLICATION");
            System.out.println("Domain = " + domain);
            System.out.println("==========================================");


            // =================================================
            // 2. FIND BUSINESS BY DOMAIN
            // =================================================

            BusinessCredentials credentials =
                    businessCredentialsRepository
                            .findByDomain(domain)
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Business credentials not found for domain: "
                                                    + domain
                                    )
                            );


            // =================================================
            // 3. CHECK STATIC / NON-STATIC IP
            // =================================================

            Boolean isStaticIp = credentials.getStaticIp();

            System.out.println(
                    "Is Static IP = " + isStaticIp
            );


            // =================================================
            // 4. NON-STATIC IP
            // =================================================

            if (!Boolean.TRUE.equals(isStaticIp)) {

                System.out.println(
                        "📦 NON-STATIC IP → Saving message for polling"
                );

                savePendingMessage(
                        credentials,
                        domain,
                        taskId,
                        tenantId,
                        localId,
                        userId,
                        poId,
                        poNumber,
                        menuId,
                        action,
                        createdBy,
                        level
                );

                return;
            }


            // =================================================
// 5. STATIC IP
// =================================================

            String ipAddress =
                    credentials.getIpAddress();

            if (ipAddress != null) {
                ipAddress = ipAddress.trim();
            }

            if (ipAddress == null ||
                    ipAddress.isEmpty()) {

                System.out.println(
                        "❌ Static IP is not configured for domain: "
                                + domain
                );

                return;
            }

            System.out.println(
                    "IP Address = [" + ipAddress + "]"
            );


// =================================================
// 6. BUILD MAIN APP URL
// =================================================

            String url =
                    ipAddress
                            + "/NexxRetail/api/workflow/whatsapp-action";

            System.out.println(
                    "Main App URL = [" + url + "]"
            );


            System.out.println(
                    "Main App URL = " + url
            );


            // =================================================
            // 7. CREATE REQUEST BODY
            // =================================================

            Map<String, Object> request =
                    new HashMap<>();

            request.put("taskId", taskId);
            request.put("tenantId", tenantId);
            request.put("localId", localId);
            request.put("domain", domain);
            request.put("userId", userId);
            request.put("poId", poId);
            request.put("poNumber", poNumber);
            request.put("menuId", menuId);
            request.put("action", action);
            request.put("createdBy", createdBy);
            request.put("level", level);


            // =================================================
            // 8. HEADERS
            // =================================================

            HttpHeaders headers =
                    new HttpHeaders();

            headers.setContentType(
                    MediaType.APPLICATION_JSON
            );


            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(
                            request,
                            headers
                    );


            // =================================================
            // 9. CALL MAIN APPLICATION
            // =================================================

            RestTemplate restTemplate =
                    new RestTemplate();

            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            url,
                            entity,
                            String.class
                    );


            // =================================================
            // 10. LOG RESPONSE
            // =================================================

            System.out.println(
                    "========== MAIN APP RESPONSE =========="
            );

            System.out.println(
                    "Status Code : "
                            + response.getStatusCode()
            );

            System.out.println(
                    "Response     : "
                            + response.getBody()
            );

            System.out.println(
                    "========================================"
            );


        } catch (Exception e) {

            System.out.println(
                    "❌ ERROR CALLING MAIN APPLICATION"
            );

            e.printStackTrace();
        }
    }
    // =====================================================
// SAVE MESSAGE FOR NON-STATIC IP
// =====================================================

    private void savePendingMessage(
            BusinessCredentials credentials,
            String domain,
            String taskId,
            int tenantId,
            int localId,
            int userId,
            long poId,
            String poNumber,
            int menuId,
            String action,
            String createdBy,
            int level) {

        try {

            Map<String, Object> request = new HashMap<>();

            request.put("taskId", taskId);
            request.put("tenantId", tenantId);
            request.put("localId", localId);
            request.put("domain", domain);
            request.put("userId", userId);
            request.put("poId", poId);
            request.put("poNumber", poNumber);
            request.put("menuId", menuId);
            request.put("action", action);
            request.put("createdBy", createdBy);
            request.put("level", level);

            PendingWebhookMessage pending =
                    new PendingWebhookMessage();

            pending.setDomain(domain);

            pending.setWhatsappId(
                    credentials.getActivationKey()
            );

            pending.setTaskId(taskId);

            pending.setPayload(
                    objectMapper.writeValueAsString(request)
            );

            pending.setStatus("PENDING");

            pendingWebhookMessageRepository.save(pending);

            System.out.println(
                    "✅ NON-STATIC IP MESSAGE SAVED"
            );

            System.out.println(
                    "Domain = " + domain
            );

            System.out.println(
                    "WhatsApp ID = "
                            + credentials.getActivationKey()
            );

            System.out.println(
                    "Task ID = " + taskId
            );

        } catch (Exception e) {

            System.out.println(
                    "❌ ERROR SAVING PENDING MESSAGE"
            );

            e.printStackTrace();
        }
    }
    // ================= NOTIFY USERS =================
    private void notifyOthers(
            String domain,
            long poId,
            int level,
            String approvedPhone,
            String userName) {

        List<WhatsAppResponse> all =
                repository.findByPoIdAndLevel(String.valueOf(poId), level);

        for (WhatsAppResponse res : all) {
            if (approvedPhone.equals(res.getPhone())) {
                continue;
            }

            sendWhatsAppMessage(
                    domain,
                    res.getPhone(),
                    "Level " + level + " approved " + userName
            );
        }
    }
    // ================= SEND MESSAGE =================
    private void sendWhatsAppMessage(String domain, String phone, String message) {

        BusinessCredentials credentials = businessCredentialsRepository
                .findByDomain(domain)
                .orElseThrow(() ->
                        new RuntimeException("Business credentials not found for domain: " + domain));

        String phoneNumberId = credentials.getActivationKey();
        String accessToken = credentials.getActivationToken();

//        if (phoneNumberId == null || phoneNumberId.isBlank()
//                || accessToken == null || accessToken.isBlank()) {
//            System.out.println("❌ WhatsApp credentials missing for domain: " + domain);
//            return;
//        }

        String url = "https://graph.facebook.com/v25.0/"
                + phoneNumberId.trim()
                + "/messages";

        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", phone);
        body.put("type", "text");
        Map<String, String> text = new HashMap<>();
        text.put("body", message);
        body.put("text", text);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken.trim());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, entity, String.class);

            System.out.println("Meta send status: " + response.getStatusCode());
            System.out.println("Meta send body  : " + response.getBody());
        } catch (Exception e) {
            System.out.println("❌ Failed to send WhatsApp message for domain: " + domain);
            e.printStackTrace();
        }
    }
}