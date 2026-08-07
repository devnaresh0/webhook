//package whatsapp.webhook.model;
//
//import javax.persistence.*;
//import java.util.Date;
//
//@Entity
//@Table(name = "whatsapp_license")
//public class License {
//
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(name = "token")
//    private String token;
//    @Column(name = "phone_number_id")
//    private String phoneNumberId;
//
//    @Column(name = "activation_key")
//    private String activationKey;
//    @Column(name = "balance")
//    private Double balance;
//
//
//    @Column(name = "serial_number")
//    private String serialNumber;
//
//    @Column(name = "domain")
//    private String domain;
//
//    @Column(name = "company_name")
//    private String companyName;
//
//    @Column(name = "plan")
//    private String plan;
//
//    @Column(name = "expiry_date")
//    @Temporal(TemporalType.TIMESTAMP)
//    private Date expiryDate;
//
//    @Column(name = "active")
//    private boolean active;
//
//    // Getters and Setters
//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public String getToken() {
//        return token;
//    }
//
//    public void setToken(String token) {
//        this.token = token;
//    }
//
//    public String getActivationKey() {
//        return activationKey;
//    }
//
//    public void setActivationKey(String activationKey) {
//        this.activationKey = activationKey;
//    }
//
//    public String getSerialNumber() {
//        return serialNumber;
//    }
//
//    public void setSerialNumber(String serialNumber) {
//        this.serialNumber = serialNumber;
//    }
//
//    public String getDomain() {
//        return domain;
//    }
//
//    public void setDomain(String domain) {
//        this.domain = domain;
//    }
//
//    public String getCompanyName() {
//        return companyName;
//    }
//
//    public void setCompanyName(String companyName) {
//        this.companyName = companyName;
//    }
//
//    public String getPlan() {
//        return plan;
//    }
//
//    public void setPlan(String plan) {
//        this.plan = plan;
//    }
//
//    public Date getExpiryDate() {
//        return expiryDate;
//    }
//
//    public void setExpiryDate(Date expiryDate) {
//        this.expiryDate = expiryDate;
//    }
//
//    public boolean isActive() {
//        return active;
//    }
//
//    public void setActive(boolean active) {
//        this.active = active;
//    }
//    public Double getBalance() {
//        return balance;
//    }
//
//    public void setBalance(Double balance) {
//        this.balance = balance;
//    }
//    public String getPhoneNumberId() {
//        return phoneNumberId;
//    }
//
//    public void setPhoneNumberId(String phoneNumberId) {
//        this.phoneNumberId = phoneNumberId;
//    }
//}