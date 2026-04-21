package whatsapp.webhook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

@Service
public class WhatsAppResponseService {

    @Autowired
    private WhatsAppResponseRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    public void saveResponse(String phone,
                             String action,
                             Object responseJson,
                             String userName) {

        try {
            // 🔥 Extract flow_token
            String token = (String) ((Map) responseJson).get("flow_token");

            System.out.println("FLOW TOKEN RECEIVED: " + token);

            String taskId;
            int userId;
            long poId;

            if (token != null && token.contains("|")) {

                String[] parts = token.split("\\|");

                if (parts.length == 3) {
                    taskId = parts[0];
                    userId = Integer.parseInt(parts[1]);
                    poId = Long.parseLong(parts[2]);
                } else {
                    throw new RuntimeException("Invalid flow_token format: " + token);
                }

            } else {
                throw new RuntimeException("flow_token missing or invalid");
            }

            // 🔥 First response check
            boolean isFirst = !repository.existsByPoId(String.valueOf(poId));

            // 🔥 Save response
            WhatsAppResponse entity = new WhatsAppResponse();
            entity.setPhone(phone);
            entity.setPoId(String.valueOf(poId));
            entity.setAction(action);
            entity.setUserName(userName);

            String json = objectMapper.writeValueAsString(responseJson);
            entity.setResponseJson(json);

            repository.save(entity);

            System.out.println("💾 Saved to DB");

            // 🔥 Process FIRST response
            if (isFirst) {

                System.out.println("🚀 FIRST RESPONSE → APPROVING TASK");

                callExternalApi(taskId, userId, poId, action);

                notifyOtherUsers(phone, String.valueOf(poId), userName);

            } else {

                System.out.println("⏭️ Already processed → notifying this user");

                WhatsAppResponse firstResponse =
                        repository.findTopByPoIdOrderByCreatedAtAsc(String.valueOf(poId));

                String approvedBy = null;

                if (firstResponse != null) {
                    approvedBy = firstResponse.getUserName();
                    if (approvedBy == null) {
                        approvedBy = firstResponse.getPhone();
                    }
                }

                sendWhatsAppMessage(
                        phone,
                        "This order already approved by " + approvedBy + ". No action needed."
                );
            }

        } catch (Exception e) {
            System.out.println("❌ DB Save Failed");
            e.printStackTrace();
        }
    }

    // 🔥 Call main app
    private void callExternalApi(String taskId, int userId, long poId, String action) {

        String appUrl =
                "https://tiesha-uncast-cher.ngrok-free.dev/NexxRetail/api/workflow/whatsapp-action";

        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> request = new HashMap<>();
        request.put("taskId", taskId);
        request.put("userId", userId);
        request.put("poId", poId);
        request.put("action", action);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-API-KEY", "secret123");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(appUrl, entity, String.class);

            System.out.println("📡 STATUS: " + response.getStatusCode());

        } catch (Exception ex) {
            System.out.println("🔥 API CALL FAILED");
            ex.printStackTrace();
        }
    }

    // 🔔 Notify others
    private void notifyOtherUsers(String firstUser, String poId, String userName) {

        try {
            WhatsAppResponse firstResponse =
                    repository.findTopByPoIdOrderByCreatedAtAsc(poId);

            String approvedBy = null;

            if (firstResponse != null) {
                approvedBy = firstResponse.getUserName();
                if (approvedBy == null) {
                    approvedBy = firstResponse.getPhone();
                }
            }

            List<WhatsAppResponse> responses = repository.findByPoId(poId);

            for (WhatsAppResponse res : responses) {

                String userPhone = res.getPhone();

                if (userPhone.equals(firstUser)) continue;

                sendWhatsAppMessage(
                        userPhone,
                        "This order already approved by " + approvedBy + ". No action required."
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 📩 WhatsApp send
    private void sendWhatsAppMessage(String phone, String message) {

        String url = "https://graph.facebook.com/v18.0/1051734401346630/messages";

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
        headers.setBearerAuth("EAAVTVgUf33UBQ5gLu5xcGlhxEzUiL4IVIN8HHz2px1Ja3kugrujsDcNEqeUlLoUf3J034yvGYgeowEbp7Mt4pFeDjAx4JVTs8HNjhqwM3zVUXn3Eb84tTZBMjMhiZCdKs0krUeIru6AzdZCmMOQ5LlnTBzZBOGEUV3S3WbdQLK5BH4qmj82uh2aNJVCHzAZDZD");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(url, entity, String.class);
            System.out.println("📩 Notification sent to: " + phone);
        } catch (Exception e) {
            System.out.println("❌ Failed to send notification");
            e.printStackTrace();
        }
    }
}