package whatsapp.webhook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import whatsapp.webhook.entity.WhatsAppMessageLink;
import whatsapp.webhook.repository.WhatsAppMessageLinkRepository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

@Service
public class WhatsAppMessageLinkService {

    @Autowired
    private WhatsAppMessageLinkRepository linkRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Persist outbound Meta message id with workflow keys from flow_token.
     * Idempotent on message_id and on (domain, task_id, po_id, level).
     */
    public void saveFromSend(
            String domain,
            Map<String, Object> payload,
            String metaResponseBody) {

        try {
            String messageId = extractMetaMessageId(metaResponseBody);
            if (messageId == null || messageId.trim().isEmpty()) {
                System.out.println("No Meta message id in send response — link not saved");
                return;
            }

            if (linkRepository.findByMessageId(messageId.trim()).isPresent()) {
                System.out.println("Message link already exists for " + messageId);
                return;
            }

            String flowToken = findFlowToken(payload);
            if (flowToken == null || !flowToken.contains("|")) {
                System.out.println("No flow_token in payload — link not saved for " + messageId);
                return;
            }

            String[] parts = flowToken.split("\\|");
            if (parts.length < 10) {
                System.out.println("Invalid flow_token structure — link not saved");
                return;
            }

            String taskId = parts[0].trim();
            String poId = parts[2].trim();
            String poNumber = parts[3].trim();
            int level = Integer.parseInt(parts[6].trim());
            String tokenDomain = parts[7].trim();

            String resolvedDomain =
                    (domain != null && !domain.trim().isEmpty())
                            ? domain.trim()
                            : tokenDomain;

            String phone = payload != null && payload.get("to") != null
                    ? String.valueOf(payload.get("to"))
                    : null;

            Optional<WhatsAppMessageLink> byKeys =
                    linkRepository.findByDomainAndTaskIdAndPoIdAndLevel(
                            resolvedDomain,
                            taskId,
                            poId,
                            level
                    );

            if (byKeys.isPresent()) {
                WhatsAppMessageLink existing = byKeys.get();
                // Keep first outbound id; do not overwrite on resend
                System.out.println(
                        "Link already exists for domain/task/po/level — keeping message_id="
                                + existing.getMessageId()
                );
                return;
            }

            WhatsAppMessageLink link = new WhatsAppMessageLink();
            link.setMessageId(messageId.trim());
            link.setDomain(resolvedDomain);
            link.setTaskId(taskId);
            link.setPoId(poId);
            link.setLevel(level);
            link.setPoNumber(poNumber);
            link.setPhone(phone);

            try {
                linkRepository.save(link);
                System.out.println(
                        "✅ Saved message link: " + messageId
                                + " domain=" + resolvedDomain
                                + " task=" + taskId
                                + " po=" + poId
                                + " level=" + level
                );
            } catch (DataIntegrityViolationException e) {
                System.out.println(
                        "⚠️ Message link duplicate ignored: " + messageId
                );
            }
        } catch (Exception e) {
            System.out.println("❌ Failed to save message link: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Optional<WhatsAppMessageLink> findByMessageId(String messageId) {
        if (messageId == null || messageId.trim().isEmpty()) {
            return Optional.empty();
        }
        return linkRepository.findByMessageId(messageId.trim());
    }

    String extractMetaMessageId(String body) {
        if (body == null || body.trim().isEmpty()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode messages = root.path("messages");
            if (messages.isArray() && messages.size() > 0) {
                String id = messages.get(0).path("id").asText(null);
                if (id != null && !id.trim().isEmpty()) {
                    return id.trim();
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    String findFlowToken(Object node) {
        if (node == null) {
            return null;
        }
        if (node instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) node;
            Object direct = map.get("flow_token");
            if (direct != null) {
                String value = String.valueOf(direct).trim();
                if (!value.isEmpty() && !"null".equalsIgnoreCase(value)) {
                    return value;
                }
            }
            for (Object value : map.values()) {
                String found = findFlowToken(value);
                if (found != null) {
                    return found;
                }
            }
        } else if (node instanceof Collection) {
            for (Object value : (Collection<?>) node) {
                String found = findFlowToken(value);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
