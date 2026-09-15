package whatsapp.webhook.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "business_balance")
public class BusinessBalance {

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

    private String currency;

    private BigDecimal balance;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
