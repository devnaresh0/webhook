package whatsapp.webhook;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_responses")
public class WhatsAppResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String phone;

    @Column(name = "po_id")
    private String poId;

    private String action;

    @Column(name = "response_json", columnDefinition = "TEXT")
    private String responseJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "user_name")
    private String userName;

    public WhatsAppResponse() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setPoId(String poId) {
        this.poId = poId;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setResponseJson(String responseJson) {
        this.responseJson = responseJson;
    }
    // ✅ ADD THESE METHODS

    public String getPhone() {
        return phone;
    }

    public String getPoId() {
        return poId;
    }

    public String getAction() {
        return action;
    }

    public String getResponseJson() {
        return responseJson;
    }
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}