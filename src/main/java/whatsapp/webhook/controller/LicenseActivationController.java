package whatsapp.webhook.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import whatsapp.webhook.model.ActivationRequest;
import whatsapp.webhook.model.ActivationResponse;
import whatsapp.webhook.service.LicenseService;

@RestController
@RequestMapping("/api/license")
public class LicenseActivationController {

    @Autowired
    private LicenseService licenseService;

    @PostMapping("/activate")
    public ResponseEntity<ActivationResponse> activate(
            @RequestBody ActivationRequest request) {

        System.out.println("========== WEBHOOK REQUEST ==========");
        System.out.println("Token          : " + request.getToken());
        System.out.println("Activation Key : " + request.getActivationKey());
        System.out.println("Serial Number  : " + request.getSerialNumber());
        System.out.println("Domain         : " + request.getDomain());

        ActivationResponse response = licenseService.activate(request);

        System.out.println("========== WEBHOOK RESPONSE ==========");
        System.out.println("Success : " + response.isSuccess());
        System.out.println("Message : " + response.getMessage());

        // Always return the response while debugging
        return ResponseEntity.ok(response);
    }

    @GetMapping("/balance")
    public ResponseEntity<Double> getBalance(@RequestParam String domain) {
        return ResponseEntity.ok(licenseService.getBalance(domain));
    }
    @GetMapping("/status")
    public ResponseEntity<Boolean> getStatus(@RequestParam String domain) {

        System.out.println("Controller reached");
        System.out.println("Domain = [" + domain + "]");

        boolean active = licenseService.isLicenseActive(domain);

        System.out.println("License Active = " + active);

        return ResponseEntity.ok(active);
    }
}