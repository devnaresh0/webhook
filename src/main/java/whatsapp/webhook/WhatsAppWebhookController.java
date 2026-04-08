package whatsapp.webhook;

import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    // Webhook event receiver
    @PostMapping
    public ResponseEntity<String> receiveMessage(@RequestBody Map<String, Object> payload) {

        try {
            System.out.println("Incoming Webhook Payload:");
            System.out.println(payload);

            // 🔹 Step 1: entry
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

                    // 🔥 only process user messages
                    if (!value.containsKey("messages")) continue;

                    List<Map<String, Object>> messages =
                            (List<Map<String, Object>>) value.get("messages");

                    for (Map<String, Object> message : messages) {

                        if (!"interactive".equals(message.get("type"))) continue;

                        String phone = (String) message.get("from");

                        Map<String, Object> interactive =
                                (Map<String, Object>) message.get("interactive");

                        Map<String, Object> nfmReply =
                                (Map<String, Object>) interactive.get("nfm_reply");

                        // 🔥 PO ID
                        String flowToken = nfmReply.get("flow_token") != null
                                ? nfmReply.get("flow_token").toString()
                                : null;

                        if (flowToken == null) return ok(); // safety

// remove extra prefix
                        String poId = flowToken.startsWith("PO_")
                                ? flowToken.substring(3)
                                : flowToken;
                        // 🔥 Action
                        ObjectMapper objectMapper = new ObjectMapper();

                        String responseJsonStr = nfmReply.get("response_json").toString();

                        Map<String, Object> responseJson =
                                objectMapper.readValue(responseJsonStr, Map.class);

                        Object selectObj = responseJson.get("screen_0_Select_0");

                        String valueStr;

                        if (selectObj instanceof List) {
                            valueStr = ((List<?>) selectObj).get(0).toString();
                        } else {
                            valueStr = selectObj.toString();
                        }

                        String action =
                                valueStr.contains("Accept") ? "APPROVE" : "REJECT";

                        System.out.println("PO: " + poId +
                                " Action: " + action +
                                " Phone: " + phone);



                        String appUrl = "https://tiesha-uncast-cher.ngrok-free.dev/whatsapp-action";

                        Map<String, Object> request = new HashMap<>();
                        request.put("poId", poId);
                        request.put("phone", phone);
                        request.put("action", action);

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        headers.add("X-API-KEY", "secret123");

                        HttpEntity<Map<String, Object>> entity =
                                new HttpEntity<>(request, headers);

                        restTemplate.postForEntity(appUrl, entity, String.class);
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