package whatsapp.webhook.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import whatsapp.webhook.model.WhatsAppResponse;
import whatsapp.webhook.repository.WhatsAppResponseRepository;

import java.util.*;

@Service
public class ApprovalService {

    @Autowired
    private WhatsAppResponseRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    // ================= MAIN METHOD =================
    public void processApproval(String phone,
                                String action,
                                Map<String, Object> responseJson,
                                String userName) {

        try {



            System.out.println("========== PROCESSING APPROVAL ==========");

            // 🔥 EXTRACT FLOW TOKEN
            String token = (String) ((Map) responseJson).get("flow_token");

            if (token == null || !token.contains("|")) {
                throw new RuntimeException("Invalid flow_token");
            }

            String[] parts = token.split("\\|");

            if (parts.length < 7) {
                throw new RuntimeException("Invalid token structure: " + token);
            }

            // 🔥 PARSE TOKEN
            String taskId = parts[0];
            int userId = Integer.parseInt(parts[1]);
            long poId = Long.parseLong(parts[2]);
            String poNumber = parts[3];   // ✅ THIS is what you want
            int menuId = Integer.parseInt(parts[4]);
            String createdBy = parts[5];
       //     String isSPO = parts[6];
            int level = Integer.parseInt(parts[6]);   // 🔥 IMPORTANT

            System.out.println("📦 PO: " + poNumber);
            System.out.println("👤 USER: " + userName);
            System.out.println("🎯 LEVEL: " + level);
            System.out.println("🎯 User: " + createdBy);
            System.out.println("🧾 TASK ID: " + taskId);

            // 🔥 DUPLICATE CHECK (LEVEL BASED)
            boolean alreadyHandled =
                    repository.existsByTaskIdAndLevel(
                            taskId,
                            level
                    );

            if (alreadyHandled) {

                System.out.println("⚠️ DUPLICATE CLICK → LEVEL " + level);

                List<WhatsAppResponse> list =
                        repository.findByTaskIdAndLevel(
                                taskId,
                                level
                        );

                if (list.isEmpty()) {
                    System.out.println("⚠️ No existing record found");
                    return;
                }

                WhatsAppResponse first = list.get(0);

                if (first == null) {
                    System.out.println("⚠️ No existing record found for duplicate taskId");
                    return;
                }

                String approvedBy = first.getUserName() != null
                        ? first.getUserName()
                        : first.getPhone();

                sendWhatsAppMessage(
                        phone,
                        poNumber + " ✅ Already approved at Level " + level + " by " + approvedBy
                );

                return;
            }
            // 🔥 SAVE APPROVAL
            WhatsAppResponse entity = new WhatsAppResponse();
            entity.setPhone(phone);
            entity.setPoId(String.valueOf(poId));
            entity.setAction(action);
            entity.setTaskId(taskId);
            entity.setUserName(userName);
            entity.setLevel(level); // 🔥 NEW COLUMN

            String json = objectMapper.writeValueAsString(responseJson);
            entity.setResponseJson(json);

            repository.save(entity);

            System.out.println("💾 LEVEL " + level + " APPROVAL SAVED");

            // 🔥 CALL MAIN WORKFLOW
            callExternalApi(taskId, userId, poId,poNumber,menuId, action,createdBy,level);

            // 🔥 NOTIFY SAME LEVEL USERS
            notifyOthers(poId, level, phone, createdBy);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= CALL MAIN APP =================
    private void callExternalApi(String taskId, int userId, long poId,String poNumber,int menuId, String action,String createdBy,int level) {
        String url;
  //     try{
           url = "https://tiesha-uncast-cher.ngrok-free.dev/NexxRetail/api/workflow/whatsapp-action";
//
//       }catch(Exception e){
 //         url = "http://197.220.114.46:9632/NexxRetail/api/workflow/whatsapp-action";

//       }


        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> request = new HashMap<>();
        request.put("taskId", taskId);
        request.put("userId", userId);
        request.put("poId", poId);
        request.put("poNumber", poNumber);
        request.put("menuId",menuId);
        request.put("action", action);
        request.put("createdBy",createdBy);
        request.put("level",level);
      //  request.put("poType", isSPO);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            restTemplate.postForEntity(url, entity, String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= NOTIFY USERS =================
    private void notifyOthers(long poId, int level, String approvedPhone, String userName) {

        List<WhatsAppResponse> all =
                repository.findByPoIdAndLevel(String.valueOf(poId), level);

        for (WhatsAppResponse res : all) {

            if (res.getPhone().equals(approvedPhone)) continue;

            sendWhatsAppMessage(
                    res.getPhone(),
                    "Level " + level + " approved  "
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
            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, entity, String.class);

            System.out.println("========== META SEND MESSAGE RESPONSE ==========");
            System.out.println("Status Code : " + response.getStatusCode());
            System.out.println("Headers     : " + response.getHeaders());
            System.out.println("Body        : " + response.getBody());
            System.out.println("===============================================");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}