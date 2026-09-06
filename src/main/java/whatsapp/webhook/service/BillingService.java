package whatsapp.webhook.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class BillingService {

    @Autowired
    private LicenseService licenseService;


    @SuppressWarnings("unchecked")
    public void processStatuses(Map<String, Object> value) {

        List<Map<String, Object>> statuses =
                (List<Map<String, Object>>) value.get("statuses");

        if (statuses == null || statuses.isEmpty()) {
            return;
        }

        for (Map<String, Object> statusEvent : statuses) {

            processStatus(statusEvent, value);

        }
    }


    @SuppressWarnings("unchecked")
    private void processStatus(
            Map<String, Object> statusEvent,
            Map<String, Object> value) {

        String messageId =
                String.valueOf(statusEvent.get("id"));

        String status =
                String.valueOf(statusEvent.get("status"));


        Map<String, Object> pricing =
                (Map<String, Object>) statusEvent.get("pricing");

        if (pricing == null) {
            return;
        }


        boolean billable =
                Boolean.parseBoolean(
                        String.valueOf(
                                pricing.get("billable")));


        String category =
                String.valueOf(
                        pricing.get("category"));


        String type =
                String.valueOf(
                        pricing.get("type"));


        Map<String, Object> metadata =
                (Map<String, Object>) value.get("metadata");


        String phoneNumberId = "";
        String displayPhone = "";


        if (metadata != null) {

            if (metadata.get("phone_number_id") != null) {

                phoneNumberId =
                        String.valueOf(
                                metadata.get(
                                        "phone_number_id"));
            }


            if (metadata.get("display_phone_number") != null) {

                displayPhone =
                        String.valueOf(
                                metadata.get(
                                        "display_phone_number"));
            }
        }


        System.out.println("======================================");
        System.out.println("Message Id      : " + messageId);
        System.out.println("Status          : " + status);
        System.out.println("Billable        : " + billable);
        System.out.println("Category        : " + category);
        System.out.println("Type            : " + type);
        System.out.println("Phone Number Id : " + phoneNumberId);
        System.out.println("Display Number  : " + displayPhone);
        System.out.println("======================================");


        /*
         * Only delivered messages are considered
         * for usage history / charging.
         */
        if (!"delivered".equalsIgnoreCase(status)) {

            System.out.println("======================================");
            System.out.println("No Usage Transaction");
            System.out.println("Reason : Message not delivered");
            System.out.println("Status : " + status);
            System.out.println("======================================");

            return;
        }


        /*
         * ======================================
         * REGULAR
         * ======================================
         *
         * Existing balance deduction logic.
         *
         * Only deduct when:
         * - delivered
         * - billable
         * - regular
         */
        if ("regular".equalsIgnoreCase(type)) {

            if (!billable) {

                System.out.println("======================================");
                System.out.println("No Balance Deduction");
                System.out.println(
                        "Reason : Regular conversation is not billable");
                System.out.println("Status   : " + status);
                System.out.println("Billable : " + billable);
                System.out.println("Type     : " + type);
                System.out.println("======================================");

                return;
            }


            deductBalance(
                    statusEvent,
                    phoneNumberId,
                    messageId
            );

            return;
        }


        /*
         * ======================================
         * FREE CUSTOMER SERVICE
         * ======================================
         *
         * Do NOT deduct balance.
         *
         * Only create a Usage history transaction
         * with:
         *
         * cost    = 0.00
         * balance = unchanged balance
         */
        if ("free_customer_service"
                .equalsIgnoreCase(type)) {

            try {

                licenseService.saveFreeUsageTransaction(
                        phoneNumberId,
                        category,
                        type,
                        1,
                        messageId
                );

                System.out.println(
                        "======================================");

                System.out.println(
                        "Free Usage Transaction Saved");

                System.out.println(
                        "Message Id      : " + messageId);

                System.out.println(
                        "Category        : " + category);

                System.out.println(
                        "Pricing Type    : " + type);

                System.out.println(
                        "Cost            : 0.00");

                System.out.println(
                        "======================================");

            } catch (Exception ex) {

                System.out.println(
                        "======================================");

                System.out.println(
                        "Free Usage Transaction Failed");

                System.out.println(
                        ex.getMessage());

                ex.printStackTrace();

                System.out.println(
                        "======================================");
                throw new RuntimeException(
                        "Free usage ledger failed for messageId="
                                + messageId
                                + ": "
                                + ex.getMessage(),
                        ex
                );
            }

            return;
        }


        /*
         * ======================================
         * OTHER / UNKNOWN TYPE
         * ======================================
         */
        System.out.println("======================================");
        System.out.println("No Usage Transaction");
        System.out.println("Reason : Unsupported pricing type");
        System.out.println("Type   : " + type);
        System.out.println("======================================");
    }


    private void deductBalance(
            Map<String, Object> statusEvent,
            String phoneNumberId,
            String messageId) {

        try {

            Map<String, Object> pricing =
                    (Map<String, Object>)
                            statusEvent.get("pricing");

            if (pricing == null) {
                return;
            }


            String category =
                    String.valueOf(
                            pricing.get("category"));


            String type =
                    String.valueOf(
                            pricing.get("type"));


            System.out.println(
                    "Charging MessageId = "
                            + messageId
                            + ", Category = "
                            + category
                            + ", Pricing Type = "
                            + type
            );


            /*
             * Existing balance deduction flow
             */
            licenseService.processConversationCharge(
                    phoneNumberId,
                    category,
                    type,
                    messageId
            );


            System.out.println(
                    "======================================");

            System.out.println(
                    "Balance Deducted Successfully");

            System.out.println(
                    "Message Id      : " + messageId);

            System.out.println(
                    "Category        : " + category);

            System.out.println(
                    "Pricing Type    : " + type);

            System.out.println(
                    "======================================");


        } catch (Exception ex) {

            System.out.println(
                    "======================================");

            System.out.println(
                    "Balance Deduction Failed");

            System.out.println(
                    ex.getMessage());

            ex.printStackTrace();

            System.out.println(
                    "======================================");
            throw new RuntimeException(
                    "Balance deduction failed for messageId="
                            + messageId
                            + ": "
                            + ex.getMessage(),
                    ex
            );
        }
    }
}