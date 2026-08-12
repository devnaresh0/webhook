package whatsapp.webhook.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import whatsapp.webhook.service.LicenseService;
import whatsapp.webhook.service.ApprovalService;
import whatsapp.webhook.service.UsageService;
import whatsapp.webhook.service.WebhookService;

@RestController
@RequestMapping("/webhook")
public class WhatsAppWebhookController {
    @Autowired
    private LicenseService licenseService;
    @Autowired
    private WebhookService webhookService;
    @Autowired
    private ApprovalService responseService;
    @Autowired
    private UsageService usageService;

    private static final String VERIFY_TOKEN = "1234";

    // ================= VERIFY =================
    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(value = "hub.mode", required = false) String mode,
            @RequestParam(value = "hub.verify_token", required = false) String token,
            @RequestParam(value = "hub.challenge", required = false) String challenge) {

        if ("subscribe".equals(mode) && VERIFY_TOKEN.equals(token)) {
            return ResponseEntity.ok(challenge);
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
    }

    // ================= RECEIVE =================
    @PostMapping
    public ResponseEntity<String> receiveMessage(
            @RequestBody Map<String,Object> payload){

        webhookService.processWebhook(payload);

        return ok();
    }
    @GetMapping("/api/usage")
    public ResponseEntity<?> getUsage(
            @RequestParam String phoneNumberId,
            @RequestParam String fromDate,
            @RequestParam String toDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        System.out.println("========== USAGE API ==========");
        System.out.println("Phone Number ID : " + phoneNumberId);
        System.out.println("From Date       : " + fromDate);
        System.out.println("To Date         : " + toDate);
        System.out.println("Page            : " + page);
        System.out.println("Page Size       : " + pageSize);

        Object result =
                usageService.getUsage(
                        phoneNumberId,
                        fromDate,
                        toDate,
                        page,
                        pageSize);

        return ResponseEntity.ok(result);
    }

    private ResponseEntity<String> ok() {
        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}