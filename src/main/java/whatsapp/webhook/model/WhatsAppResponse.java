package whatsapp.webhook.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(
        name = "whatsapp_responses",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_wr_domain_task_level",
                columnNames = {"domain", "task_id", "level"}
        )
)
public class WhatsAppResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String phone;

    @Column(name = "domain")
    private String domain;

    @Column(name = "po_id")
    private String poId;

    @Column(name = "level")
    private Integer level;

    private String action;

    @Column(name = "response_json", columnDefinition = "TEXT")
    private String responseJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "task_id")
    private String taskId;

    public WhatsAppResponse() {
        this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setDomain(String domain) {
        this.domain = domain;
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

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getPhone() {
        return phone;
    }

    public String getDomain() {
        return domain;
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

    public String getTaskId() {
        return taskId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
