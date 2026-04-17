package whatsapp.webhook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class WhatsAppResponseService {

    @Autowired
    private WhatsAppResponseRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    public void saveResponse(String phone, String poId,
                             String action, Object responseJson,
                             String userName,int level) {


        try {
            String token = (String) ((java.util.Map) responseJson).get("flow_token");

            String parsedPoId = null;
            int parsedLevel = 0;

            if (token != null && token.contains("|")) {

                String[] parts = token.split("\\|");

                parsedPoId = parts[0];                  // PO02161
                parsedLevel = Integer.parseInt(parts[1]); // 1

            } else {
                parsedPoId = token;
            }
            // ✅ Check if FIRST response for this PO
            boolean isFirst = !repository.existsByPoId(parsedPoId);
            // ✅ ALWAYS SAVE response
            WhatsAppResponse entity = new WhatsAppResponse();
            entity.setPhone(phone);
            entity.setPoId(parsedPoId);
            entity.setAction(action);
            entity.setUserName(userName);

            String json = objectMapper.writeValueAsString(responseJson);
            entity.setResponseJson(json);

            repository.save(entity);

            System.out.println("💾 Saved to DB");

            // ✅ ONLY FIRST RESPONSE → CALL API
            if (isFirst) {

                System.out.println("🚀 FIRST RESPONSE → CALLING API");

                callExternalApi(phone, parsedPoId, action, parsedLevel);
                notifyOtherUsers(phone, parsedPoId, userName);
            } else {

                System.out.println("⏭️ Already processed → notifying this user");

                // ✅ GET FIRST APPROVER FROM DB
                WhatsAppResponse firstResponse =
                        repository.findTopByPoIdOrderByCreatedAtAsc(parsedPoId);
                String approvedBy = null;

                if (firstResponse != null) {
                    approvedBy = firstResponse.getUserName();

                    if (approvedBy == null) {
                        approvedBy = firstResponse.getPhone();
                    }
                }

                // ✅ SEND MESSAGE USING FIRST USER
                sendWhatsAppMessage(phone,
                        parsedPoId + " already approved..." +
                                approvedBy + ". No action needed.");
            }

        } catch (Exception e) {
            System.out.println("❌ DB Save Failed");
            e.printStackTrace();
        }
    }
    private void sendWhatsAppMessage(String phone, String message) {

        String url = "https://graph.facebook.com/v18.0/1051734401346630/messages";

        org.springframework.web.client.RestTemplate restTemplate =
                new org.springframework.web.client.RestTemplate();

        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", phone);
        body.put("type", "text");

        java.util.Map<String, String> text = new java.util.HashMap<>();
        text.put("body", message);

        body.put("text", text);

        org.springframework.http.HttpHeaders headers =
                new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        // 🔴 IMPORTANT: replace with your real token
        headers.setBearerAuth("EAAVTVgUf33UBQxStyqSKHQF3Q3zTVAmeYswTl5yPvngtcjfX2RV5BZBb7mjqFB8ZAUc4WZAZAIGiMZAeisJVpiggiu67dANajEAJubhIL0gzDM4a8ZAsJ34XydBRemPjbEwEZAdCExLR6L5s44ufJHdHeO2qrwoccplGJLiOlGf0yX4upTvJSMwxrwx3uMXXAZDZD");

        org.springframework.http.HttpEntity<java.util.Map<String, Object>> entity =
                new org.springframework.http.HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(url, entity, String.class);
            System.out.println("📩 Notification sent to: " + phone);
        } catch (Exception e) {
            System.out.println("❌ Failed to send notification");
            e.printStackTrace();
        }
    }
    // ✅ THIS MUST BE OUTSIDE saveResponse()
    private void callExternalApi(String phone, String poId, String action,int level) {

        String appUrl =
                "https://tiesha-uncast-cher.ngrok-free.dev/NexxRetail/api/workflow/whatsapp-action";

        org.springframework.web.client.RestTemplate restTemplate =
                new org.springframework.web.client.RestTemplate();

        java.util.Map<String, Object> request = new java.util.HashMap<>();
        request.put("poId", poId);
        request.put("phone", phone);
        request.put("action", action);
        request.put("level", level);  //  ADD THIS

        org.springframework.http.HttpHeaders headers =
                new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.add("X-API-KEY", "secret123");

        org.springframework.http.HttpEntity<java.util.Map<String, Object>> entity =
                new org.springframework.http.HttpEntity<>(request, headers);

        try {
            org.springframework.http.ResponseEntity<String> response =
                    restTemplate.postForEntity(appUrl, entity, String.class);

            System.out.println("📡 STATUS: " + response.getStatusCode());

        } catch (Exception ex) {
            System.out.println("🔥 API CALL FAILED");
            ex.printStackTrace();
        }

    }
    private void notifyOtherUsers(String firstUser, String poId, String userName) {

        try {
            // ✅ GET FIRST APPROVER
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

                sendWhatsAppMessage(userPhone,
                          poId + " already approved by " +
                                approvedBy + ". No action required.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}