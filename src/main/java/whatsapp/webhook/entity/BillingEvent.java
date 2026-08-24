//package whatsapp.webhook.entity;
//
//import javax.persistence.*;
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "billing_events")
//public class BillingEvent {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(name = "conversation_id")
//    private Long conversationId;
//
//    private String domain;
//
//    @Column(name = "phone_number_id")
//    private String phoneNumberId;
//
//    @Column(name = "pricing_category")
//    private String pricingCategory;
//
//    @Column(name = "meta_cost")
//    private BigDecimal metaCost;
//
//    @Column(name = "customer_price")
//    private BigDecimal customerPrice;
//
//    private BigDecimal profit;
//
//    private String currency;
//
//    @Column(name = "billed_at")
//    private LocalDateTime billedAt = LocalDateTime.now();
//
//    // Getters & Setters
//
//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public Long getConversationId() {
//        return conversationId;
//    }
//
//    public void setConversationId(Long conversationId) {
//        this.conversationId = conversationId;
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
//    public String getPricingCategory() {
//        return pricingCategory;
//    }
//
//    public void setPricingCategory(String pricingCategory) {
//        this.pricingCategory = pricingCategory;
//    }
//
//    public String getPhoneNumberId() {
//        return phoneNumberId;
//    }
//
//    public void setPhoneNumberId(String phoneNumberId) {
//        this.phoneNumberId = phoneNumberId;
//    }
//
//    public BigDecimal getMetaCost() {
//        return metaCost;
//    }
//
//    public void setMetaCost(BigDecimal metaCost) {
//        this.metaCost = metaCost;
//    }
//
//    public BigDecimal getCustomerPrice() {
//        return customerPrice;
//    }
//
//    public void setCustomerPrice(BigDecimal customerPrice) {
//        this.customerPrice = customerPrice;
//    }
//
//    public BigDecimal getProfit() {
//        return profit;
//    }
//
//    public void setProfit(BigDecimal profit) {
//        this.profit = profit;
//    }
//
//    public String getCurrency() {
//        return currency;
//    }
//
//    public void setCurrency(String currency) {
//        this.currency = currency;
//    }
//
//    public LocalDateTime getBilledAt() {
//        return billedAt;
//    }
//
//    public void setBilledAt(LocalDateTime billedAt) {
//        this.billedAt = billedAt;
//    }
//}
