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


    // =====================================================
    // DOMAIN
    // =====================================================

    @Column(name = "domain")
    private String domain;


    // =====================================================
    // TRANSACTION DATE
    // =====================================================

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;


    // =====================================================
    // DATA TYPE
    // =====================================================

    @Column(name = "data_type")
    private String dataType;


    // =====================================================
    // PRICING CATEGORY
    // =====================================================

    @Column(name = "pricing_category")
    private String pricingCategory;


    // =====================================================
    // PRICING TYPE
    // =====================================================

    @Column(name = "pricing_type")
    private String pricingType;


    // =====================================================
    // MESSAGES
    // =====================================================

    @Column(name = "messages")
    private Integer messages;


    // =====================================================
    // LOAD AMOUNT
    // =====================================================

    @Column(name = "load_amount")
    private BigDecimal loadAmount;


    // =====================================================
    // COST
    // =====================================================

    @Column(name = "cost")
    private BigDecimal cost;


    // =====================================================
    // BALANCE
    // =====================================================

    @Column(name = "balance")
    private BigDecimal balance;


    // =====================================================
    // REFERENCE ID
    // =====================================================

    @Column(name = "reference_id")
    private String referenceId;


    // =====================================================
    // STATUS
    //
    // PENDING
    // SUCCESS
    // FAILED
    // =====================================================

    @Column(name = "status")
    private String status;


    // =====================================================
    // CREATED AT
    // =====================================================

    @Column(name = "created_at")
    private LocalDateTime createdAt;


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


    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(
            LocalDateTime transactionDate) {

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

    public void setPricingCategory(
            String pricingCategory) {

        this.pricingCategory = pricingCategory;
    }


    public String getPricingType() {
        return pricingType;
    }

    public void setPricingType(
            String pricingType) {

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

    public void setLoadAmount(
            BigDecimal loadAmount) {

        this.loadAmount = loadAmount;
    }


    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(
            BigDecimal cost) {

        this.cost = cost;
    }


    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(
            BigDecimal balance) {

        this.balance = balance;
    }


    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(
            String referenceId) {

        this.referenceId = referenceId;
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

    public void setCreatedAt(
            LocalDateTime createdAt) {

        this.createdAt = createdAt;
    }
}