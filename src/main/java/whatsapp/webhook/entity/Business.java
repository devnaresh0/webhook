package whatsapp.webhook.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "businesses")
public class Business {

    @Id
    @Column(name = "domain")
    private String domain;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "domain",
            referencedColumnName = "domain",
            insertable = false,
            updatable = false
    )
    private BusinessCredentials businessCredentials;

    @Column(name = "business_name")
    private String businessName;

    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);

    // Getters & Setters

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public BusinessCredentials getBusinessCredentials() {
        return businessCredentials;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
