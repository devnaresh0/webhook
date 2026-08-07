//package whatsapp.webhook.entity;
//
//import javax.persistence.*;
//import java.time.LocalDateTime;
//@Entity
//@Table(name = "whatsapp_messages")
//public class WhatsAppMessage {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
////    @ManyToOne(fetch = FetchType.LAZY)
////    @JoinColumn(
////            name = "message_id",
////            referencedColumnName = "message_id"
////    )
////    private CustomerConversation conversation;
//
//    @Column(name = "message_id")
//    private String messageId;
////
////    @Column(name = "domain")
////    private String domain;
//
//    @Column(name = "phone_number_id")
//    private String phoneNumberId;
//
//    @Column(name = "recipient_phone")
//    private String recipientPhone;
////
////    @Column(name = "recipient_name")
////    private String recipientName;
//
//    @Column(name = "direction")
//    private String direction;
//
//    @Column(name = "message_type")
//    private String messageType;
//
////    @Column(name = "message_body", columnDefinition = "TEXT")
////    private String messageBody;
//
////    @Column(name = "media_url", columnDefinition = "TEXT")
////    private String mediaUrl;
//
//    @Column(name = "created_at")
//    private LocalDateTime createdAt = LocalDateTime.now();
//
//    // getters/setters
//
//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
////    public CustomerConversation getConversation() {
////        return conversation;
////    }
////
////    public void setConversation(CustomerConversation conversation) {
////        this.conversation = conversation;
////    }
//
//    public String getMessageId() {
//        return messageId;
//    }
//
//    public void setMessageId(String messageId) {
//        this.messageId = messageId;
//    }
//
////    public String getDomain() {
////        return domain;
////    }
//
////    public void setDomain(String domain) {
////        this.domain = domain;
////    }
//
//    public String getPhoneNumberId() {
//        return phoneNumberId;
//    }
//
//    public void setPhoneNumberId(String phoneNumberId) {
//        this.phoneNumberId = phoneNumberId;
//    }
//
//    public String getRecipientPhone() {
//        return recipientPhone;
//    }
//
//    public void setRecipientPhone(String recipientPhone) {
//        this.recipientPhone = recipientPhone;
//    }
//
////    public String getRecipientName() {
////        return recipientName;
////    }
////
////    public void setRecipientName(String recipientName) {
////        this.recipientName = recipientName;
////    }
//
//    public String getDirection() {
//        return direction;
//    }
//
//    public void setDirection(String direction) {
//        this.direction = direction;
//    }
//
//    public String getMessageType() {
//        return messageType;
//    }
//
//    public void setMessageType(String messageType) {
//        this.messageType = messageType;
//    }
//
////    public String getMessageBody() {
////        return messageBody;
////    }
////
////    public void setMessageBody(String messageBody) {
////        this.messageBody = messageBody;
////    }
//
////    public String getMediaUrl() {
////        return mediaUrl;
////    }
////
////    public void setMediaUrl(String mediaUrl) {
////        this.mediaUrl = mediaUrl;
////    }
//
//    public LocalDateTime getCreatedAt() {
//        return createdAt;
//    }
//
//    public void setCreatedAt(LocalDateTime createdAt) {
//        this.createdAt = createdAt;
//    }
//}