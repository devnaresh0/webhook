package whatsapp.webhook.model;

public class ActivationRequest {

    private String token;
    private String activationKey;
    private String domain;
    private String serialNumber;
    private Boolean staticIpEnabled;
    private String staticIpUrl;

    public Boolean getStaticIpEnabled() {
        return staticIpEnabled;
    }

    public void setStaticIpEnabled(Boolean staticIpEnabled) {
        this.staticIpEnabled = staticIpEnabled;
    }

    public String getStaticIpUrl() {
        return staticIpUrl;
    }

    public void setStaticIpUrl(String staticIpUrl) {
        this.staticIpUrl = staticIpUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getActivationKey() {
        return activationKey;
    }

    public void setActivationKey(String activationKey) {
        this.activationKey = activationKey;
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
}