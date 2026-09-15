package whatsapp.webhook.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "business_credentials")
public class BusinessCredentials implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "domain", nullable = false, unique = true)
    private String domain;

    @Column(name = "activation_key")
    private String activationKey;

    @Column(name = "activation_token")
    private String activationToken;

    @Column(name = "license_status")
    private String licenseStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt =
            LocalDateTime.now(ZoneOffset.UTC);

    @Column(name = "is_static_ip")
    private Boolean isStaticIp;

    @Column(name = "ip_address")
    private String ipAddress;

    // =====================================================
    // GETTERS / SETTERS
    // =====================================================

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

    public String getActivationKey() {
        return activationKey;
    }

    public void setActivationKey(String activationKey) {
        this.activationKey = activationKey;
    }

    public String getActivationToken() {
        return activationToken;
    }

    public void setActivationToken(String activationToken) {
        this.activationToken = activationToken;
    }

    public String getLicenseStatus() {
        return licenseStatus;
    }

    public void setLicenseStatus(String licenseStatus) {
        this.licenseStatus = licenseStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getStaticIp() {
        return isStaticIp;
    }

    public void setStaticIp(Boolean staticIp) {
        isStaticIp = staticIp;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
