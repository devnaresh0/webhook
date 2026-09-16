package whatsapp.webhook.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Links an outbound Meta message id (wamid) to workflow identifiers
 * so usage rows can show APPROVE / REJECT decisions.
 */
@Entity
@Table(
        name = "whatsapp_message_links",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_wml_message_id",
                        columnNames = "message_id"
                ),
                @UniqueConstraint(
                        name = "uk_wml_domain_task_po_level",
                        columnNames = {"domain", "task_id", "po_id", "level"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_wml_domain_task_level",
                        columnList = "domain,task_id,level"
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
