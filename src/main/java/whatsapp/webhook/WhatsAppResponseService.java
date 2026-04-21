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

    // ================= MAIN METHOD =================
    public void saveResponse(String phone,
                             String action,
                             Object responseJson,
                             String userName) {

        try {

            // 🔥 EXTRACT FLOW TOKEN
            String token = (String) ((Map) responseJson).get("flow_token");

            if (token == null || !token.contains("|")) {
                throw new RuntimeException("Invalid flow_token");
            }

            String[] parts = token.split("\\|");

            String taskId = parts[0];      // 🔥 KEY FIX
            int userId = Integer.parseInt(parts[1]);
            long poId = Long.parseLong(parts[2]);

            System.out.println("TASK ID: " + taskId);

            // 🔥 CHECK IF TASK ALREADY HANDLED (PER LEVEL)
            boolean alreadyHandled = repository.existsByTaskId(taskId);

            if (alreadyHandled) {

                System.out.println("⚠️ Duplicate click ignored");

                WhatsAppResponse first =
                        repository.findTopByTaskIdOrderByCreatedAtAsc(taskId);

                String approvedBy = first.getUserName() != null
                        ? first.getUserName()
                        : first.getPhone();

                sendWhatsAppMessage(
                        phone,
                        "✅ Already approved by " + approvedBy
                );

                return; // ❗ STOP HERE
            }

            // 🔥 SAVE FIRST RESPONSE
            WhatsAppResponse entity = new WhatsAppResponse();
            entity.setPhone(phone);
            entity.setPoId(String.valueOf(poId));
            entity.setAction(action);
            entity.setUserName(userName);
            entity.setTaskId(taskId); // 🔥 IMPORTANT

            String json = objectMapper.writeValueAsString(responseJson);
            entity.setResponseJson(json);

            repository.save(entity);

            System.out.println("💾 FIRST APPROVAL SAVED");

            // 🔥 CALL MAIN API
            callExternalApi(taskId, userId, poId, action);

            // 🔥 NOTIFY OTHER USERS
            notifyOthers(taskId, phone, userName);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= CALL MAIN APP =================
    private void callExternalApi(String taskId, int userId, long poId, String action) {

        String url = "https://tiesha-uncast-cher.ngrok-free.dev/NexxRetail/api/workflow/whatsapp-action";

        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> request = new HashMap<>();
        request.put("taskId", taskId);
        request.put("userId", userId);
        request.put("poId", poId);
        request.put("action", action);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            restTemplate.postForEntity(url, entity, String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= NOTIFY OTHERS =================
    private void notifyOthers(String taskId, String approvedPhone, String userName) {

        List<WhatsAppResponse> all = repository.findByTaskId(taskId);

        for (WhatsAppResponse res : all) {

            if (res.getPhone().equals(approvedPhone)) continue;

            sendWhatsAppMessage(
                    res.getPhone(),
                    "Approved by " + userName
            );
        }
    }

    // ================= SEND MESSAGE =================
    private void sendWhatsAppMessage(String phone, String message) {

        String url = "https://graph.facebook.com/v25.0/1051734401346630/messages";

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}