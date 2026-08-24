package whatsapp.webhook.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "business_balance_transactions")
public class BusinessBalanceTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String domain;

    private LocalDateTime transactionDate;

    private String dataType;

    private String pricingCategory;

    private String pricingType;

    private Integer messages;

    private BigDecimal loadAmount;

    private BigDecimal cost;

    private BigDecimal balance;

    private String referenceId;

    private LocalDateTime createdAt;

    // getters/setters

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

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getPricingCategory() {
        return pricingCategory;
    }

    public void setPricingCategory(String pricingCategory) {
        this.pricingCategory = pricingCategory;
    }

    public String getPricingType() {
        return pricingType;
    }

    public void setPricingType(String pricingType) {
        this.pricingType = pricingType;
    }

    public Integer getMessages() {
        return messages;
    }

    public void setMessages(Integer messages) {
        this.messages = messages;
    }

    public BigDecimal getLoadAmount() {
        return loadAmount;
    }

    public void setLoadAmount(BigDecimal loadAmount) {
        this.loadAmount = loadAmount;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
