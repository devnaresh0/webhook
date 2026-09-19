package whatsapp.webhook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import whatsapp.webhook.entity.WhatsAppMessageLink;
import whatsapp.webhook.repository.WhatsAppMessageLinkRepository;

import java.util.Collection;
import java.util.List;
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
     * On resend for the same domain/po/level/recipient user, older links are marked inactive.
     */
    @Transactional
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
            String recipientUserId = parts[1].trim();
            String poId = parts[2].trim();
            String poNumber = parts[3].trim();
            int level = Integer.parseInt(parts[6].trim());
            String tokenDomain = parts[7].trim();

            String resolvedDomain =
                    (domain != null && !domain.trim().isEmpty())
                            ? domain.trim()
                            : tokenDomain;

            String phone = payload != null && payload.get("to") != null
                    ? String.valueOf(payload.get("to")).trim()
                    : null;
            if (phone != null && phone.isEmpty()) {
                phone = null;
            }

            int superseded = deactivateOlderLinks(
                    resolvedDomain, poId, level, recipientUserId
            );
            int lowerLevels = deactivateLowerLevelLinks(resolvedDomain, poId, level);
            if (superseded > 0 || lowerLevels > 0) {
                System.out.println(
                        "Superseded " + superseded
                                + " same-level + " + lowerLevels
                                + " lower-level link(s) for domain=" + resolvedDomain
                                + " po=" + poId
                                + " level=" + level
                                + " recipientUserId=" + recipientUserId
                );
            }

            WhatsAppMessageLink link = new WhatsAppMessageLink();
            link.setMessageId(messageId.trim());
            link.setDomain(resolvedDomain);
            link.setTaskId(taskId);
            link.setPoId(poId);
            link.setLevel(level);
            link.setPoNumber(poNumber);
            link.setPhone(phone);
            link.setRecipientUserId(recipientUserId);
            link.setActive(Boolean.TRUE);

            try {
                linkRepository.save(link);
                System.out.println(
                        "✅ Saved message link: " + messageId
                                + " domain=" + resolvedDomain
                                + " task=" + taskId
                                + " po=" + poId
                                + " level=" + level
                                + " recipientUserId=" + recipientUserId
                                + " active=true"
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

    /**
     * Mark prior active outbound approvals for this recipient user/doc/level inactive.
     * Keyed by recipient user id (not phone) so multiple approvers sharing one
     * WhatsApp number keep their own active messages.
     */
    int deactivateOlderLinks(
            String domain,
            String poId,
            int level,
            String recipientUserId) {

        if (recipientUserId == null || recipientUserId.trim().isEmpty()) {
            System.out.println(
                    "Skip supersede deactivate — recipientUserId missing"
                            + " (domain=" + domain
                            + " po=" + poId
                            + " level=" + level + ")"
            );
            return 0;
        }

        List<WhatsAppMessageLink> older =
                linkRepository.findByDomainAndPoIdAndLevelAndRecipientUserIdAndActiveTrue(
                        domain, poId, level, recipientUserId.trim()
                );

        if (older == null || older.isEmpty()) {
            return 0;
        }

        for (WhatsAppMessageLink existing : older) {
            existing.setActive(Boolean.FALSE);
            linkRepository.save(existing);
            System.out.println(
                    "Deactivated message link id=" + existing.getId()
                            + " message_id=" + existing.getMessageId()
                            + " recipientUserId=" + recipientUserId
            );
        }
        return older.size();
    }

    /**
     * When a higher workflow level is notified, older-level WhatsApp approvals
     * for this PO are no longer valid (e.g. L1 bubble after L2 was sent / update).
     */
    int deactivateLowerLevelLinks(String domain, String poId, int level) {
        if (level <= 1) {
            return 0;
        }

        List<WhatsAppMessageLink> lower =
                linkRepository.findByDomainAndPoIdAndActiveTrueAndLevelLessThan(
                        domain, poId, level
                );

        if (lower == null || lower.isEmpty()) {
            return 0;
        }

        for (WhatsAppMessageLink existing : lower) {
            existing.setActive(Boolean.FALSE);
            linkRepository.save(existing);
            System.out.println(
                    "Deactivated lower-level link id=" + existing.getId()
                            + " message_id=" + existing.getMessageId()
                            + " level=" + existing.getLevel()
            );
        }
        return lower.size();
    }

    /**
     * True when this outbound message should no longer accept Flow replies.
     */
    public boolean isMessageSuperseded(String messageId) {
        if (messageId == null || messageId.trim().isEmpty()) {
            return false;
        }

        Optional<WhatsAppMessageLink> linkOpt =
                linkRepository.findByMessageId(messageId.trim());
        if (!linkOpt.isPresent()) {
            return false;
        }

        WhatsAppMessageLink link = linkOpt.get();
        if (!link.isActive()) {
            return true;
        }

        // Workflow already moved past this message's level
        if (linkRepository.existsByDomainAndPoIdAndActiveTrueAndLevelGreaterThan(
                link.getDomain(), link.getPoId(), link.getLevel())) {
            return true;
        }

        // A newer active message exists for the same recipient at this level
        if (link.getRecipientUserId() != null) {
            List<WhatsAppMessageLink> activeSameUser =
                    linkRepository.findByDomainAndPoIdAndLevelAndRecipientUserIdAndActiveTrue(
                            link.getDomain(),
                            link.getPoId(),
                            link.getLevel(),
                            link.getRecipientUserId()
                    );
            if (activeSameUser != null) {
                for (WhatsAppMessageLink other : activeSameUser) {
                    if (other.getId() != null
                            && link.getId() != null
                            && other.getId() > link.getId()) {
                        return true;
                    }
                    if (other.getMessageId() != null
                            && !other.getMessageId().equals(link.getMessageId())) {
                        // Another active link for same user/level — treat this as stale
                        if (other.getCreatedAt() != null
                                && link.getCreatedAt() != null
                                && other.getCreatedAt().isAfter(link.getCreatedAt())) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * True if any outbound approval exists for a higher workflow level on this PO
     * (means lower levels are already past — show Accepted on usage even without
     * a WhatsApp reply at that lower level).
     */
    public boolean hasHigherLevelMessage(String domain, String poId, int level) {
        if (domain == null || poId == null) {
            return false;
        }
        return linkRepository.existsByDomainAndPoIdAndLevelGreaterThan(
                domain, poId, level
        );
    }

    public boolean hasActiveHigherLevel(String domain, String poId, int level) {
        if (domain == null || poId == null) {
            return false;
        }
        return linkRepository.existsByDomainAndPoIdAndActiveTrueAndLevelGreaterThan(
                domain, poId, level
        );
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
