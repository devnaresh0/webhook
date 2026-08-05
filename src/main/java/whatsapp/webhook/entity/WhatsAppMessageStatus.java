package whatsapp.webhook.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_message_status")
public class WhatsAppMessageStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id")
    private Long messageId;

    private String status;

    @Column(name = "webhook_timestamp")
    private LocalDateTime webhookTimestamp;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters & Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getWebhookTimestamp() {
        return webhookTimestamp;
    }

    public void setWebhookTimestamp(LocalDateTime webhookTimestamp) {
        this.webhookTimestamp = webhookTimestamp;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}