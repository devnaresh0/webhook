package whatsapp.webhook.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_phone_numbers")
public class WhatsAppPhoneNumber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String domain;

//    @Column(name = "waba_id")
//    private String wabaId;

    @Column(name = "phone_number_id")
    private String phoneNumberId;

    @Column(name = "display_phone_number")
    private String displayPhoneNumber;

//    @Column(name = "verified_name")
//    private String verifiedName;

    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

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

//    public String getWabaId() {
//        return wabaId;
//    }
//
//    public void setWabaId(String wabaId) {
//        this.wabaId = wabaId;
//    }

    public String getPhoneNumberId() {
        return phoneNumberId;
    }

    public void setPhoneNumberId(String phoneNumberId) {
        this.phoneNumberId = phoneNumberId;
    }

    public String getDisplayPhoneNumber() {
        return displayPhoneNumber;
    }

    public void setDisplayPhoneNumber(String displayPhoneNumber) {
        this.displayPhoneNumber = displayPhoneNumber;
    }

//    public String getVerifiedName() {
//        return verifiedName;
//    }
//
//    public void setVerifiedName(String verifiedName) {
//        this.verifiedName = verifiedName;
//    }

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
