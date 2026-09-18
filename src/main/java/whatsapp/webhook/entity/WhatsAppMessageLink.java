package whatsapp.webhook.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Links an outbound Meta message id (wamid) to workflow identifiers
 * so usage rows can show APPROVE / REJECT decisions.
 * Multiple rows per domain/po/level/phone are allowed; only one is active.
 * On document update/resend, older rows are marked inactive (superseded).
 */
@Entity
@Table(
        name = "whatsapp_message_links",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_wml_message_id",
                        columnNames = "message_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_wml_domain_task_level",
                        columnList = "domain,task_id,level"
                ),
                @Index(
                        name = "idx_wml_domain_po_level_phone_active",
                        columnList = "domain,po_id,level,phone,active"
                ),
                @Index(
                        name = "idx_wml_domain_po_level_user_active",
                        columnList = "domain,po_id,level,recipient_user_id,active"
                )
        }
)
public class WhatsAppMessageLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private String messageId;

    @Column(name = "domain", nullable = false)
    private String domain;

    @Column(name = "task_id", nullable = false)
    private String taskId;

    @Column(name = "po_id", nullable = false)
    private String poId;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "po_number")
    private String poNumber;

    @Column(name = "phone")
    private String phone;

    /**
     * Recipient Nexx user id from flow_token (parts[1]).
     * Used so multiple approvers sharing one WhatsApp number do not
     * supersede each other's messages.
     */
    @Column(name = "recipient_user_id")
    private String recipientUserId;

    /**
     * false = superseded by a newer approval message for the same
     * doc/level/recipient user.
     */
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getPoId() {
        return poId;
    }

    public void setPoId(String poId) {
        this.poId = poId;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(String recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
