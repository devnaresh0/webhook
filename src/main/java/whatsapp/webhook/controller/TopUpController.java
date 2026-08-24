package whatsapp.webhook.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import whatsapp.webhook.service.TopUpService;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/whatsapp")
public class TopUpController {

    @Autowired
    private TopUpService topUpService;


    // =====================================================
    // RECEIVE TOP-UP RESULT FROM MAIN APPLICATION
    // =====================================================

    @PostMapping("/topup")
    public ResponseEntity<?> receiveTopUp(
            @RequestBody Map<String, Object> payload) {

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "        WHATSAPP TOP-UP RECEIVED"
        );

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "Payload = " + payload
        );


        try {

            // =================================================
            // 1. GET DOMAIN
            // =================================================

            String domain =
                    payload.get("domain") != null
                            ? String.valueOf(
                            payload.get("domain")
                    )
                            : null;


            // =================================================
            // 2. GET AMOUNT
            // =================================================

            BigDecimal amount = null;

            if (payload.get("amount") != null) {

                amount =
                        new BigDecimal(
                                String.valueOf(
                                        payload.get("amount")
                                )
                        );
            }


            // =================================================
            // 3. GET MOBILE NUMBER
            // =================================================

            String mobileNumber =
                    payload.get("mobileNumber") != null
                            ? String.valueOf(
                            payload.get("mobileNumber")
                    )
                            : null;


            // =================================================
            // 4. GET REFERENCE ID
            // =================================================

            String referenceId =
                    payload.get("referenceId") != null
                            ? String.valueOf(
                            payload.get("referenceId")
                    )
                            : null;


            // =================================================
            // 5. GET STATUS
            // =================================================

            String status =
                    payload.get("status") != null
                            ? String.valueOf(
                            payload.get("status")
                    ).toUpperCase()
                            : null;


            // =================================================
            // 6. VALIDATE DOMAIN
            // =================================================

            if (domain == null ||
                    domain.trim().isEmpty()) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                "domain is required"
                        );
            }


            // =================================================
            // 7. VALIDATE AMOUNT
            // =================================================

            if (amount == null ||
                    amount.compareTo(
                            BigDecimal.ZERO
                    ) <= 0) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                "amount must be greater than 0"
                        );
            }


            // =================================================
            // 8. VALIDATE REFERENCE ID
            // =================================================

            if (referenceId == null ||
                    referenceId.trim().isEmpty()) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                "referenceId is required"
                        );
            }


            // =================================================
            // 9. VALIDATE STATUS
            // =================================================

            if (!"SUCCESS".equals(status) &&
                    !"FAILED".equals(status)) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                "status must be SUCCESS or FAILED"
                        );
            }


            System.out.println(
                    "Domain       = " + domain
            );

            System.out.println(
                    "Amount       = " + amount
            );

            System.out.println(
                    "Mobile       = " + mobileNumber
            );

            System.out.println(
                    "Reference ID = " + referenceId
            );

            System.out.println(
                    "Status       = " + status
            );


            // =================================================
            // 10. PROCESS TOP-UP
            // =================================================

            Map<String, Object> result =
                    topUpService.processTopUp(
                            domain,
                            amount,
                            mobileNumber,
                            referenceId,
                            status
                    );


            System.out.println(
                    "✅ TOP-UP PROCESSED"
            );

            System.out.println(
                    "Result = " + result
            );


            return ResponseEntity.ok(
                    result
            );


        } catch (Exception e) {

            System.out.println(
                    "❌ ERROR PROCESSING TOP-UP"
            );

            e.printStackTrace();

            return ResponseEntity
                    .status(
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .body(
                            "Unable to process top-up: "
                                    + e.getMessage()
                    );
        }
    }
}