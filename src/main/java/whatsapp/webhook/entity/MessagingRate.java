package whatsapp.webhook.entity;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "messaging_rates")
public class MessagingRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "pricing_category")
    private String pricingCategory;

    @Column(name = "pricing_model")
    private String pricingModel;

    @Column(name = "price_per_conversation")
    private BigDecimal pricePerConversation;

    private String currency;

    // effectiveFrom
    // effectiveTo
    // active

    // Getters & Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getPricingCategory() {
        return pricingCategory;
    }

    public void setPricingCategory(String pricingCategory) {
        this.pricingCategory = pricingCategory;
    }

    public String getPricingModel() {
        return pricingModel;
    }

    public void setPricingModel(String pricingModel) {
        this.pricingModel = pricingModel;
    }

    public BigDecimal getPricePerConversation() {
        return pricePerConversation;
    }

    public void setPricePerConversation(BigDecimal pricePerConversation) {
        this.pricePerConversation = pricePerConversation;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}