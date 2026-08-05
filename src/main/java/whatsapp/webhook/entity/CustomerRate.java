//package whatsapp.webhook.entity;
//
//import javax.persistence.*;
//import java.math.BigDecimal;
//
//@Entity
//@Table(name = "customer_rates")
//public class CustomerRate {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private String domain;
//
//    @Column(name = "pricing_category")
//    private String pricingCategory;
//
//    @Column(name = "price_per_conversation")
//    private BigDecimal pricePerConversation;
//
//    private String currency;
//
//    // effectiveFrom
//    // effectiveTo
//    // active
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
//    public BigDecimal getPricePerConversation() {
//        return pricePerConversation;
//    }
//
//    public void setPricePerConversation(BigDecimal pricePerConversation) {
//        this.pricePerConversation = pricePerConversation;
//    }
//
//    public String getCurrency() {
//        return currency;
//    }
//
//    public void setCurrency(String currency) {
//        this.currency = currency;
//    }
//}
