package whatsapp.webhook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ConversationService {

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    public void processMessages(Map<String, Object> value) {

        String phone = null;

        List<Map<String, Object>> contacts =
                (List<Map<String, Object>>) value.get("contacts");

        if (contacts != null && !contacts.isEmpty()) {
            phone = String.valueOf(contacts.get(0).get("wa_id"));
        }

        List<Map<String, Object>> messages =
                (List<Map<String, Object>>) value.get("messages");

        if (messages == null || messages.isEmpty()) {
            return;
        }

        for (Map<String, Object> message : messages) {

            if (phone == null && message.get("from") != null) {
                phone = String.valueOf(message.get("from"));
            }

            if (phone == null) {
                System.out.println("Phone not found.");
                continue;
            }

            System.out.println("====================================");
            System.out.println("Incoming Message");
            System.out.println("Phone : " + phone);
            System.out.println("====================================");

            processMessage(phone, contacts, message);
        }
    }

    @SuppressWarnings("unchecked")
    private void processMessage(String phone,
                                List<Map<String, Object>> contacts,
                                Map<String, Object> message) {

        Map<String, Object> interactive =
                (Map<String, Object>) message.get("interactive");

        if (interactive == null) {
            return;
        }

        Map<String, Object> nfmReply =
                (Map<String, Object>) interactive.get("nfm_reply");

        if (nfmReply == null) {
            return;
        }

        processFlowReply(phone, contacts, nfmReply);
    }

    @SuppressWarnings("unchecked")
    private void processFlowReply(String phone,
                                  List<Map<String, Object>> contacts,
                                  Map<String, Object> nfmReply) {

        Object responseObj = nfmReply.get("response_json");

        if (responseObj == null) {
            System.out.println("response_json missing");
            return;
        }

        Map<String, Object> responseJson = null;

        try {
            if (responseObj instanceof String) {
                responseJson = objectMapper.readValue((String) responseObj, Map.class);
            } else if (responseObj instanceof Map) {
                responseJson = (Map<String, Object>) responseObj;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        if (responseJson == null) {
            return;
        }

        System.out.println("Flow response_json: " + responseJson);

        String userName = extractUserName(responseJson, contacts);
        String action = extractAction(responseJson);
        String reason = extractReason(responseJson);

        if (action == null) {
            System.out.println("No action selected. keys=" + responseJson.keySet());
            return;
        }

        System.out.println("Reason : " + reason);

        approvalService.processApproval(phone, action, responseJson, userName, reason);
    }

    @SuppressWarnings("unchecked")
    private String extractUserName(Map<String, Object> responseJson,
                                   List<Map<String, Object>> contacts) {

        if (responseJson.get("name") != null) {
            return responseJson.get("name").toString();
        }

        if (contacts != null && !contacts.isEmpty()) {
            Map<String, Object> profile =
                    (Map<String, Object>) contacts.get(0).get("profile");
            if (profile != null && profile.get("name") != null) {
                return profile.get("name").toString();
            }
        }

        return null;
    }

    private String extractReason(Map<String, Object> responseJson) {
        Object reason = firstNonNull(
                responseJson.get("reason"),
                responseJson.get("screen_0_reason"),
                responseJson.get("screen_RECOMMEND_reason")
        );
        if (reason == null) {
            return null;
        }
        String value = reason.toString().trim();
        return value.isEmpty() ? null : value;
    }

    private String extractAction(Map<String, Object> responseJson) {

        Object selected = firstNonNull(
                responseJson.get("approval"),
                responseJson.get("Choose_one_f437a0"),
                responseJson.get("screen_0_Choose_one_f437a0"),
                responseJson.get("screen_RECOMMEND_Choose_one_f437a0"),
                responseJson.get("screen_0_Select_0"),
                responseJson.get("screen_0_Choose_one_0")
        );

        if (selected == null) {
            for (Map.Entry<String, Object> entry : responseJson.entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }
                String key = entry.getKey() == null ? "" : entry.getKey().toLowerCase();
                String raw = entry.getValue().toString();
                if (key.contains("approv") || key.contains("choose") || key.contains("select")
                        || raw.contains("Accept") || raw.contains("Reject")
                        || raw.contains("0_Accept") || raw.contains("1_Reject")) {
                    selected = entry.getValue();
                    break;
                }
            }
        }

        if (selected == null) {
            return null;
        }

        String value;
        if (selected instanceof List && !((List<?>) selected).isEmpty()) {
            value = ((List<?>) selected).get(0).toString();
        } else {
            value = selected.toString();
        }

        System.out.println("User Selection : " + value);

        String lower = value.toLowerCase();
        if (lower.contains("accept") || lower.contains("0_accept") || "approve".equals(lower)) {
            return "APPROVE";
        }
        if (lower.contains("reject") || lower.contains("1_reject")) {
            return "REJECT";
        }

        return null;
    }

    private Object firstNonNull(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}