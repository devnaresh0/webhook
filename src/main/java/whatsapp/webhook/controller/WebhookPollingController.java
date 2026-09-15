package whatsapp.webhook.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import whatsapp.webhook.entity.BusinessCredentials;
import whatsapp.webhook.entity.PendingWebhookMessage;
import whatsapp.webhook.repository.BusinessCredentialRepository;
import whatsapp.webhook.repository.PendingWebhookMessageRepository;

import java.util.List;

@RestController
@RequestMapping("/webhook")
public class WebhookPollingController {

    @Autowired
    private BusinessCredentialRepository businessCredentialsRepository;

    @Autowired
    private PendingWebhookMessageRepository pendingWebhookMessageRepository;


    // =====================================================
    // POLL PENDING MESSAGES
    // =====================================================

    @GetMapping("/poll")
    public ResponseEntity<?> pollMessages(

            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorization,

            @RequestParam("whatsappId")
            String whatsappId,

            @RequestParam("domain")
            String domain
    ) {

        try {

            // 1. Validate token
            if (authorization == null ||
                    !authorization.startsWith("Bearer ")) {

                return ResponseEntity
                        .status(401)
                        .body("Missing access token");
            }

            String accessToken =
                    authorization.substring(7).trim();


            // 2. Find business using activation_token
            BusinessCredentials credentials =
                    businessCredentialsRepository
                            .findByActivationToken(accessToken)
                            .orElse(null);

            if (credentials == null) {

                return ResponseEntity
                        .status(401)
                        .body("Invalid access token");
            }


            // 3. Verify WhatsApp ID
            if (credentials.getActivationKey() == null ||
                    !credentials.getActivationKey()
                            .equals(whatsappId)) {

                return ResponseEntity
                        .status(403)
                        .body("Invalid WhatsApp ID");
            }


            // 4. Validate domain
            String credentialDomain =
                    credentials.getDomain();

            if (domain == null ||
                    domain.trim().isEmpty()) {

                return ResponseEntity
                        .status(400)
                        .body("Domain is required");
            }

            if (credentialDomain == null ||
                    !credentialDomain.equals(domain)) {

                return ResponseEntity
                        .status(403)
                        .body("Invalid domain for WhatsApp credentials");
            }


            // 5. Debug
            System.out.println(
                    "========== POLL DOMAIN DEBUG =========="
            );

            System.out.println(
                    "WhatsApp ID       = " + whatsappId
            );

            System.out.println(
                    "Credential Domain = [" + credentialDomain + "]"
            );

            System.out.println(
                    "Requested Domain  = [" + domain + "]"
            );

            System.out.println(
                    "======================================="
            );


            // 6. Get pending messages ONLY for this domain
            List<PendingWebhookMessage> messages =
                    pendingWebhookMessageRepository
                            .findByDomainAndWhatsappIdAndStatusOrderByCreatedAtAsc(
                                    domain,
                                    whatsappId,
                                    "PENDING"
                            );


            // 7. Return JSON
            return ResponseEntity.ok(messages);

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(500)
                    .body("Error polling messages");
        }
    }


    // =====================================================
    // ACKNOWLEDGE MESSAGE
    // =====================================================

    @PostMapping("/ack")
    public ResponseEntity<?> acknowledgeMessage(

            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorization,

            @RequestParam("messageId")
            Long messageId) {

        try {

            // =================================================
            // 1. CHECK AUTHORIZATION
            // =================================================

            if (authorization == null ||
                    !authorization.startsWith("Bearer ")) {

                return ResponseEntity
                        .status(401)
                        .body("Missing access token");
            }


            // =================================================
            // 2. GET ACCESS TOKEN
            // =================================================

            String accessToken =
                    authorization.substring(7).trim();


            // =================================================
            // 3. FIND BUSINESS
            // =================================================

            BusinessCredentials credentials =
                    businessCredentialsRepository
                            .findByActivationToken(accessToken)
                            .orElse(null);


            if (credentials == null) {

                return ResponseEntity
                        .status(401)
                        .body("Invalid access token");
            }


            // =================================================
            // 4. FIND MESSAGE
            // =================================================

            PendingWebhookMessage message =
                    pendingWebhookMessageRepository
                            .findById(messageId)
                            .orElse(null);


            if (message == null) {

                return ResponseEntity
                        .status(404)
                        .body("Message not found");
            }


            // =================================================
            // 5. VERIFY MESSAGE BELONGS TO THIS TENANT
            // =================================================

            if (!credentials.getDomain()
                    .equals(message.getDomain())) {

                return ResponseEntity
                        .status(403)
                        .body("Message does not belong to this tenant");
            }


            // =================================================
            // 6. MARK AS DELIVERED
            // =================================================

            message.setStatus("DELIVERED");

            pendingWebhookMessageRepository.save(message);


            // =================================================
            // 7. RESPONSE
            // =================================================

            return ResponseEntity.ok(
                    "Message acknowledged successfully"
            );


        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(500)
                    .body("Error acknowledging message");
        }
    }
}
