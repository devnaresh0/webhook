package whatsapp.webhook.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import whatsapp.webhook.model.ActivationRequest;
import whatsapp.webhook.model.ActivationResponse;
import whatsapp.webhook.service.LicenseService;

import java.util.Map;

@RestController
@RequestMapping("/api/license")
public class LicenseActivationController {

    private final RestTemplate restTemplate =
            new RestTemplate();

    private static final String WHATSAPP_API_BASE_URL =
            "https://graph.facebook.com/v25.0/";


    @Autowired
    private LicenseService licenseService;


    // =========================================================
    // ACTIVATE LICENSE
    // =========================================================

    @PostMapping("/activate")
    public ResponseEntity<ActivationResponse> activate(
            @RequestBody ActivationRequest request) {

        System.out.println(
                "========== WEBHOOK REQUEST =========="
        );

        System.out.println(
                "Token          : "
                        + request.getToken()
        );

        System.out.println(
                "Activation Key : "
                        + request.getActivationKey()
        );
        System.out.println(
                "Static IP Enabled : "
                        + request.getStaticIpEnabled()
        );

        System.out.println(
                "Static IP URL     : "
                        + request.getStaticIpUrl()
        );

        System.out.println(
                "Serial Number  : "
                        + request.getSerialNumber()
        );

        System.out.println(
                "Domain         : "
                        + request.getDomain()
        );


        ActivationResponse response =
                licenseService.activate(request);


        System.out.println(
                "========== WEBHOOK RESPONSE =========="
        );

        System.out.println(
                "Success : "
                        + response.isSuccess()
        );

        System.out.println(
                "Message : "
                        + response.getMessage()
        );


        return ResponseEntity.ok(response);
    }


    // =========================================================
    // GET BALANCE
    // =========================================================

    @GetMapping("/balance")
    public ResponseEntity<Double> getBalance(
            @RequestParam String domain) {

        return ResponseEntity.ok(
                licenseService.getBalance(domain)
        );
    }


    // =========================================================
    // GET LICENSE STATUS
    // =========================================================

    @GetMapping("/status")
    public ResponseEntity<Boolean> getStatus(
            @RequestParam String domain) {

        System.out.println(
                "Controller reached"
        );

        System.out.println(
                "Domain = [" + domain + "]"
        );


        boolean active =
                licenseService.isLicenseActive(
                        domain
                );


        System.out.println(
                "License Active = "
                        + active
        );


        return ResponseEntity.ok(active);
    }


    // =========================================================
    // SEND WHATSAPP MESSAGE
    // =========================================================

    @PostMapping("/whatsapp/send")
    public ResponseEntity<?> sendMessage(
            @RequestBody Map<String, Object> requestBody) {

        System.out.println(
                "========== WHATSAPP REQUEST RECEIVED =========="
        );


        try {

            // =================================================
            // 1. GET DOMAIN
            // =================================================

            String domain =
                    (String) requestBody.get(
                            "domain"
                    );


            // =================================================
            // 2. GET PHONE NUMBER ID
            // =================================================

            String phoneNumberId =
                    (String) requestBody.get(
                            "phoneNumberId"
                    );


            // =================================================
            // 3. GET ACCESS TOKEN
            // =================================================

            String accessToken =
                    (String) requestBody.get(
                            "accessToken"
                    );


            // =================================================
            // 4. GET ORIGINAL WHATSAPP PAYLOAD
            // =================================================

            @SuppressWarnings("unchecked")
            Map<String, Object> payload =
                    (Map<String, Object>)
                            requestBody.get(
                                    "payload"
                            );


            // =================================================
            // 5. BASIC VALIDATION
            // =================================================

            if (domain == null ||
                    domain.trim().isEmpty()) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                "Domain is required"
                        );
            }


            if (phoneNumberId == null ||
                    phoneNumberId.trim().isEmpty()) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                "phoneNumberId is required"
                        );
            }


            if (accessToken == null ||
                    accessToken.trim().isEmpty()) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                "accessToken is required"
                        );
            }


            if (payload == null ||
                    payload.isEmpty()) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                "WhatsApp payload is required"
                        );
            }


            // =================================================
            // 6. CHECK LICENSE
            // =================================================

            boolean licenseActive =
                    licenseService.isLicenseActive(
                            domain
                    );


            if (!licenseActive) {

                System.out.println(
                        "❌ License is inactive for domain: "
                                + domain
                );


                return ResponseEntity
                        .status(
                                HttpStatus.FORBIDDEN
                        )
                        .body(
                                "License is inactive for domain: "
                                        + domain
                        );
            }


            System.out.println(
                    "✅ License is active for domain: "
                            + domain
            );


            // =================================================
            // 7. VERIFY BOTH WHATSAPP CREDENTIALS
            // =================================================
            //
            // IMPORTANT:
            //
            // This checks BOTH:
            //
            // activation_key   == phoneNumberId
            //
            // activation_token == accessToken
            //
            // for the SAME domain.
            //
            // Nothing is sent to Meta before this check.
            // =================================================

            boolean credentialsValid =
                    licenseService
                            .areWhatsAppCredentialsValid(
                                    domain,
                                    phoneNumberId,
                                    accessToken
                            );


            if (!credentialsValid) {

                System.out.println(
                        "❌ WhatsApp credentials "
                                + "do not belong to domain."
                );

                System.out.println(
                        "Domain = "
                                + domain
                );

                System.out.println(
                        "Phone Number ID = "
                                + phoneNumberId
                );

                // DO NOT PRINT ACCESS TOKEN


                return ResponseEntity
                        .status(
                                HttpStatus.FORBIDDEN
                        )
                        .body(
                                "WhatsApp Phone Number ID "
                                        + "and Access Token "
                                        + "do not belong to this domain"
                        );
            }


            // =================================================
            // 8. BOTH CREDENTIALS VERIFIED
            // =================================================

            System.out.println(
                    "=========================================="
            );

            System.out.println(
                    "✅ WHATSAPP CREDENTIALS VERIFIED"
            );

            System.out.println(
                    "Domain = "
                            + domain
            );

            System.out.println(
                    "Phone Number ID belongs to domain = YES"
            );

            System.out.println(
                    "Access Token belongs to domain = YES"
            );

            System.out.println(
                    "=========================================="
            );


            // =================================================
            // 9. CREATE META URL
            // =================================================

            String url =
                    WHATSAPP_API_BASE_URL
                            + phoneNumberId
                            + "/messages";


            System.out.println(
                    "Meta URL = "
                            + url
            );


            // =================================================
            // 10. CREATE META HEADERS
            // =================================================

            HttpHeaders metaHeaders =
                    new HttpHeaders();

            metaHeaders.setContentType(
                    MediaType.APPLICATION_JSON
            );


            metaHeaders.set(
                    "Authorization",
                    "Bearer " + accessToken
            );


            // =================================================
            // 11. SEND ORIGINAL PAYLOAD TO META
            // =================================================

            HttpEntity<Map<String, Object>> metaRequest =
                    new HttpEntity<>(
                            payload,
                            metaHeaders
                    );
            System.out.println(
                    "========== FINAL META PAYLOAD =========="
            );

            System.out.println(
                    "Payload = " + payload
            );

            System.out.println(
                    "TO = " + payload.get("to")
            );

            System.out.println(
                    "TYPE = " + payload.get("type")
            );

            System.out.println(
                    "MESSAGING PRODUCT = "
                            + payload.get("messaging_product")
            );

            System.out.println(
                    "========================================"
            );


            System.out.println(
                    "📤 Sending WhatsApp payload to Meta..."
            );


            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            url,
                            metaRequest,
                            String.class
                    );


            // =================================================
            // 12. META RESPONSE
            // =================================================

            System.out.println(
                    "✅ WhatsApp Meta Response: "
                            + response.getBody()
            );


            return ResponseEntity
                    .status(
                            response.getStatusCode()
                    )
                    .body(
                            response.getBody()
                    );


        } catch (HttpClientErrorException e) {

            // =================================================
            // META ERROR
            // =================================================

            System.out.println(
                    "❌ META ERROR RESPONSE:"
            );

            System.out.println(
                    "HTTP Status = "
                            + e.getStatusCode()
            );

            System.out.println(
                    e.getResponseBodyAsString()
            );


            return ResponseEntity
                    .status(
                            e.getStatusCode()
                    )
                    .body(
                            e.getResponseBodyAsString()
                    );


        } catch (Exception e) {

            // =================================================
            // GENERAL ERROR
            // =================================================

            System.out.println(
                    "❌ ERROR WHILE SENDING WHATSAPP:"
            );


            e.printStackTrace();


            return ResponseEntity
                    .status(
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .body(
                            "Unable to send WhatsApp message: "
                                    + e.getMessage()
                    );
        }
    }
}