package whatsapp.webhook.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_webhook_log")
public class WhatsAppWebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;





    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "received_at")
    private LocalDateTime receivedAt = LocalDateTime.now();

    // Getters & Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }







    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }
}
