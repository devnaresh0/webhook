package whatsapp.webhook.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_conversations")
public class CustomerConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String domain;

    @Column(name = "phone_number_id")
    private String phoneNumberId;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "meta_conversation_id")
    private String metaConversationId;

    @Column(name = "conversation_type")
    private String conversationType;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "last_customer_message_at")
    private LocalDateTime lastCustomerMessageAt;

    @Column(name = "last_business_message_at")
    private LocalDateTime lastBusinessMessageAt;

    private Boolean billable;

    @Column(name = "meta_cost")
    private BigDecimal metaCost;

    private String currency;

    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Getters & Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getPhoneNumberId() {
        return phoneNumberId;
    }

    public void setPhoneNumberId(String phoneNumberId) {
        this.phoneNumberId = phoneNumberId;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getMetaConversationId() {
        return metaConversationId;
    }

    public void setMetaConversationId(String metaConversationId) {
        this.metaConversationId = metaConversationId;
    }

    public String getConversationType() {
        return conversationType;
    }

    public void setConversationType(String conversationType) {
        this.conversationType = conversationType;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(LocalDateTime openedAt) {
        this.openedAt = openedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getLastCustomerMessageAt() {
        return lastCustomerMessageAt;
    }

    public void setLastCustomerMessageAt(LocalDateTime lastCustomerMessageAt) {
        this.lastCustomerMessageAt = lastCustomerMessageAt;
    }

    public LocalDateTime getLastBusinessMessageAt() {
        return lastBusinessMessageAt;
    }

    public void setLastBusinessMessageAt(LocalDateTime lastBusinessMessageAt) {
        this.lastBusinessMessageAt = lastBusinessMessageAt;
    }

    public Boolean getBillable() {
        return billable;
    }

    public void setBillable(Boolean billable) {
        this.billable = billable;
    }

    public BigDecimal getMetaCost() {
        return metaCost;
    }

    public void setMetaCost(BigDecimal metaCost) {
        this.metaCost = metaCost;
    }
}
