package whatsapp.webhook;

import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/webhook")
public class WhatsAppWebhookController {
    @Autowired
    private LicenseService licenseService;

    @Autowired
    private WhatsAppResponseService responseService;

    private static final String VERIFY_TOKEN = "1234";

    // ================= VERIFY =================
    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(value = "hub.mode", required = false) String mode,
            @RequestParam(value = "hub.verify_token", required = false) String token,
            @RequestParam(value = "hub.challenge", required = false) String challenge) {

        if ("subscribe".equals(mode) && VERIFY_TOKEN.equals(token)) {
            return ResponseEntity.ok(challenge);
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
    }

    // ================= RECEIVE =================
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

                    // ================= MESSAGES =================
                    List<Map<String, Object>> messages =
                            (List<Map<String, Object>>) value.get("messages");

                    if (messages == null) {

                        List<Map<String, Object>> statuses =
                                (List<Map<String, Object>>) value.get("statuses");

                        if (statuses != null && !statuses.isEmpty()) {

                            Map<String, Object> statusEvent = statuses.get(0);

                            String messageId = String.valueOf(statusEvent.get("id"));
                            String status = String.valueOf(statusEvent.get("status"));

                            Map<String, Object> pricing =
                                    (Map<String, Object>) statusEvent.get("pricing");

                            if (pricing != null) {

                                boolean billable =
                                        Boolean.parseBoolean(pricing.get("billable").toString());

                                String category =
                                        pricing.get("category").toString();

                                String type =
                                        pricing.get("type").toString();

                                Map<String, Object> metadata =
                                        (Map<String, Object>) value.get("metadata");

                                String phoneNumberId = "";
                                String displayPhoneNumber = "";

                                if (metadata != null) {

                                    if (metadata.get("phone_number_id") != null) {
                                        phoneNumberId = metadata.get("phone_number_id").toString();
                                    }

                                    if (metadata.get("display_phone_number") != null) {
                                        displayPhoneNumber = metadata.get("display_phone_number").toString();
                                    }
                                }

                                System.out.println("========================================");
                                System.out.println("Message Id          : " + messageId);
                                System.out.println("Status              : " + status);
                                System.out.println("Billable            : " + billable);
                                System.out.println("Category            : " + category);
                                System.out.println("Type                : " + type);
                                System.out.println("Phone Number Id     : " + phoneNumberId);
                                System.out.println("Display Phone       : " + displayPhoneNumber);
                                System.out.println("========================================");

                                if ("delivered".equalsIgnoreCase(status)
                                        && billable
                                        && "utility".equalsIgnoreCase(category)
                                        && "regular".equalsIgnoreCase(type)) {

                                    System.out.println("✅ Utility Delivered Message");
                                    System.out.println("₹0.50 will be deducted.");

                                    try {

                                        String recipientNumber = statusEvent.get("recipient_id").toString();

                                        licenseService.processUtilityCharge(
                                                phoneNumberId,
                                                recipientNumber,
                                                0.50
                                        );

                                        System.out.println("========================================");
                                        System.out.println("✅ Balance Deducted Successfully");
                                        System.out.println("Amount      : ₹0.50");
                                        System.out.println("Message Id  : " + messageId);
                                        System.out.println("Phone Id    : " + phoneNumberId);
                                        System.out.println("========================================");

                                    } catch (Exception ex) {

                                        System.out.println("========================================");
                                        System.out.println("❌ Balance Deduction Failed");
                                        System.out.println("Reason : " + ex.getMessage());
                                        ex.printStackTrace();
                                        System.out.println("========================================");
                                    }

                                } else {

                                    System.out.println("========================================");
                                    System.out.println("ℹ Balance Not Deducted");
                                    System.out.println("Reason:");
                                    System.out.println("Status    = " + status);
                                    System.out.println("Billable  = " + billable);
                                    System.out.println("Category  = " + category);
                                    System.out.println("Type      = " + type);
                                    System.out.println("========================================");
                                }
                            }
                        }

                        continue;
                    }

                    for (Map<String, Object> message : messages) {

                        if (phone == null && message.get("from") != null) {
                            phone = message.get("from").toString();
                        }

                        if (phone == null) {
                            System.out.println("❌ Phone missing, skipping...");
                            continue;
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

                        // ================= USER NAME =================
                        String userName = null;

                        // 1. Try from response_json
                        if (responseJson.get("name") != null) {
                            userName = responseJson.get("name").toString();
                        }

                        // 2. Fallback from WhatsApp profile
                        if (userName == null && contacts != null && !contacts.isEmpty()) {
                            Map<String, Object> profile =
                                    (Map<String, Object>) contacts.get(0).get("profile");

                            if (profile != null && profile.get("name") != null) {
                                userName = profile.get("name").toString();
                            }
                        }

                        System.out.println("👤 USER: " + userName);

                        // ================= PO ID =================
                        String poId = null;
                        int level = 0;

                        if (responseJson.get("po_id") != null) {
                            poId = responseJson.get("po_id").toString();
                        }

                        if (poId == null && responseJson.get("flow_token") != null) {
                            poId = responseJson.get("flow_token").toString();
                        }

                        if (poId == null && nfmReply.get("flow_token") != null) {
                            poId = nfmReply.get("flow_token").toString();
                        }

                        if (poId == null) {
                            System.out.println("⚠️ PO ID missing, using TEST ID");
                            poId = "99999";
                        }

                        System.out.println(poId);

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

                        // ================= SAVE =================
                        // NO CHANGE NEEDED HERE (already correct)
                        responseService.saveResponse(
                                phone,
                                action,
                                responseJson,
                                userName
                        );
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