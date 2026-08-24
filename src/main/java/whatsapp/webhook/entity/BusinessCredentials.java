package whatsapp.webhook.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "business_credentials")
public class BusinessCredentials {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String domain;

    @Column(name = "serial_number")
    private String serialNumber;

    @Column(name = "activation_key")
    private String activationKey;

    @Column(name = "activation_token")
    private String activationToken;

    @Column(name = "license_status")
    private String licenseStatus;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt =
            LocalDateTime.now();

    // =====================================================
    // IP ADDRESS
    // =====================================================

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


    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
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


    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(
            LocalDateTime expiresAt) {

        this.expiresAt = expiresAt;
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            LocalDateTime createdAt) {

        this.createdAt = createdAt;
    }


    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(
            String ipAddress) {

        this.ipAddress = ipAddress;
    }
}