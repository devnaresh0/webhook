package whatsapp.webhook;

import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.HashMap;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
@RestController
@RequestMapping("/webhook")
public class WhatsAppWebhookController {
    @Autowired
    private WhatsAppResponseService responseService;

    private static final String VERIFY_TOKEN = "1234";
    private RestTemplate restTemplate = new RestTemplate();
    // Webhook verification endpoint
    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(value = "hub.mode", required = false) String mode,
            @RequestParam(value = "hub.verify_token", required = false) String token,
            @RequestParam(value = "hub.challenge", required = false) String challenge) {

        System.out.println("Verification Request Received");
        System.out.println("Mode: " + mode);
        System.out.println("Token: " + token);
        System.out.println("Challenge: " + challenge);

        if ("subscribe".equals(mode) && VERIFY_TOKEN.equals(token)) {
            System.out.println("Webhook verified successfully");
            return ResponseEntity.ok(challenge);
        }

        System.out.println("Webhook verification failed");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
    }

    @PostMapping
    public ResponseEntity<String> receiveMessage(@RequestBody Map<String, Object> payload) {

        try {
            System.out.println("========== INCOMING WEBHOOK ==========");
            System.out.println(payload);

            List<Map<String, Object>> entryList =
                    (List<Map<String, Object>>) payload.get("entry");

            if (entryList == null) return ok();

            for (Map<String, Object> entry : entryList) {

                List<Map<String, Object>> changes =
                        (List<Map<String, Object>>) entry.get("changes");

                if (changes == null) continue;

                for (Map<String, Object> change : changes) {

                    Map<String, Object> value =
                            (Map<String, Object>) change.get("value");

                    if (value == null) continue;

                    // ================= PHONE =================
                    String phone = null;

                    List<Map<String, Object>> contacts =
                            (List<Map<String, Object>>) value.get("contacts");

                    if (contacts != null && !contacts.isEmpty()) {
                        phone = contacts.get(0).get("wa_id").toString();
                    }

                    List<Map<String, Object>> messages =
                            (List<Map<String, Object>>) value.get("messages");

                    if (messages == null) continue;

                    for (Map<String, Object> message : messages) {

                        if (phone == null && message.get("from") != null) {
                            phone = message.get("from").toString();
                        }

                        System.out.println("📱 PHONE: " + phone);

                        // ================= INTERACTIVE =================
                        Map<String, Object> interactive =
                                (Map<String, Object>) message.get("interactive");

                        if (interactive == null) {
                            System.out.println("❌ interactive missing");
                            continue;
                        }

                        Map<String, Object> nfmReply =
                                (Map<String, Object>) interactive.get("nfm_reply");

                        if (nfmReply == null) {
                            System.out.println("❌ nfm_reply missing");
                            continue;
                        }

                        // ================= RESPONSE JSON =================
                        Object responseObj = nfmReply.get("response_json");

                        Map<String, Object> responseJson = null;

                        ObjectMapper objectMapper = new ObjectMapper();

                        try {
                            if (responseObj instanceof String) {
                                responseJson = objectMapper.readValue(
                                        (String) responseObj, Map.class);
                            } else if (responseObj instanceof Map) {
                                responseJson = (Map<String, Object>) responseObj;
                            }
                        } catch (Exception ex) {
                            System.out.println("❌ Failed to parse response_json");
                            ex.printStackTrace();
                            continue;
                        }

                        if (responseJson == null) {
                            System.out.println("❌ responseJson null");
                            continue;
                        }

                        System.out.println("📦 RESPONSE JSON: " + responseJson);

                        // ================= PO ID EXTRACTION =================
                        String poId = null;

                        // Case 1: po_id
                        if (responseJson.get("po_id") != null) {
                            poId = responseJson.get("po_id").toString();
                        }

                        // Case 2: flow_token inside response_json
                        if (poId == null && responseJson.get("flow_token") != null) {
                            String flowToken = responseJson.get("flow_token").toString();

                            if (flowToken != null && !"unused".equalsIgnoreCase(flowToken)) {
                                if (flowToken.startsWith("PO_")) {
                                    poId = flowToken.substring(3);
                                } else {
                                    poId = flowToken;
                                }
                            }
                        }

                        // Case 3: flow_token outside
                        if (poId == null && nfmReply.get("flow_token") != null) {
                            String flowToken = nfmReply.get("flow_token").toString();

                            if (flowToken != null && !"unused".equalsIgnoreCase(flowToken)) {
                                if (flowToken.startsWith("PO_")) {
                                    poId = flowToken.substring(3);
                                } else {
                                    poId = flowToken;
                                }
                            }
                        }

                        System.out.println("🧾 PO ID: " + poId);

                        if (poId == null) {
                            System.out.println("⚠️ PO ID missing, using TEST ID");
                            poId = "99999"; // temp
                        }

                        // ================= ACTION =================
                        Object selectObj = responseJson.get("screen_0_Select_0");

                        if (selectObj == null) {
                            selectObj = responseJson.get("screen_0_Choose_one_0");
                        }

                        if (selectObj == null) {
                            System.out.println("❌ selection missing");
                            continue;
                        }

                        String valueStr;

                        if (selectObj instanceof List) {
                            valueStr = ((List<?>) selectObj).get(0).toString();
                        } else {
                            valueStr = selectObj.toString();
                        }

                        System.out.println("🎯 USER SELECTION: " + valueStr);

                        String action =
                                valueStr.contains("Accept") ? "APPROVE" : "REJECT";

                        System.out.println("✅ ACTION: " + action);

//                        // ================= API CALL =================
//                        String appUrl =
//                                "http://197.220.114.46:9632/NexxRetail/api/workflow/whatsapp-action";
//
//                        Map<String, Object> request = new HashMap<String, Object>();
//                        request.put("poId", poId);
//                        request.put("phone", phone);
//                        request.put("action", action);
//
//                        System.out.println("🚀 CALLING API...");
//                        System.out.println("➡ URL: " + appUrl);
//                        System.out.println("➡ BODY: " + request);
//
//                        HttpHeaders headers = new HttpHeaders();
//                        headers.setContentType(MediaType.APPLICATION_JSON);
//                        headers.add("X-API-KEY", "secret123");
//
//                        HttpEntity<Map<String, Object>> entity =
//                                new HttpEntity<Map<String, Object>>(request, headers);
//
//                        try {
//                            ResponseEntity<String> response =
//                                    restTemplate.postForEntity(appUrl, entity, String.class);
//
//                            System.out.println("📡 STATUS: " + response.getStatusCode());
//                            System.out.println("📡 RESPONSE: " + response.getBody());
//
//                        } catch (Exception ex) {
//                            System.out.println("🔥 API CALL FAILED");
//                            ex.printStackTrace();
//                        }
                        responseService.saveResponse(phone, poId, action, responseJson);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("ERROR");
        }

        return ok();
    }

    private ResponseEntity<String> ok() {
        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}