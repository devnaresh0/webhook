package whatsapp.webhook;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebhookStatusController {

    @GetMapping("/webhook-status")
    public ResponseEntity<String> webhookStatus() {

        return ResponseEntity.ok("OK");

    }

}