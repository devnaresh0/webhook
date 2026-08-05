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
    private void processStatus(Map<String, Object> statusEvent,
                               Map<String, Object> value) {

        String messageId = String.valueOf(statusEvent.get("id"));
        String status = String.valueOf(statusEvent.get("status"));

        Map<String, Object> pricing =
                (Map<String, Object>) statusEvent.get("pricing");

        if (pricing == null) {
            return;
        }

        boolean billable =
                Boolean.parseBoolean(
                        String.valueOf(pricing.get("billable")));

        String category =
                String.valueOf(pricing.get("category"));

        String type =
                String.valueOf(pricing.get("type"));

        Map<String, Object> metadata =
                (Map<String, Object>) value.get("metadata");

        String phoneNumberId = "";
        String displayPhone = "";

        if (metadata != null) {

            if (metadata.get("phone_number_id") != null) {
                phoneNumberId =
                        String.valueOf(metadata.get("phone_number_id"));
            }

            if (metadata.get("display_phone_number") != null) {
                displayPhone =
                        String.valueOf(metadata.get("display_phone_number"));
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

        if (!shouldCharge(status, billable, type)) {

            System.out.println("======================================");
            System.out.println("No Balance Deduction");
            System.out.println("Reason : Not a chargeable conversation");
            System.out.println("Status   : " + status);
            System.out.println("Billable : " + billable);
            System.out.println("Type     : " + type);
            System.out.println("======================================");

            return;
        }

        deductBalance(statusEvent, phoneNumberId, messageId);

    }

    private boolean shouldCharge(String status,
                                 boolean billable,
                                 String type) {

        return "delivered".equalsIgnoreCase(status)
                && billable
                && "regular".equalsIgnoreCase(type);
    }

    private void deductBalance(Map<String, Object> statusEvent,
                               String phoneNumberId,
                               String messageId) {

        try {

            String recipientNumber =
                    String.valueOf(statusEvent.get("recipient_id"));

            Map<String, Object> pricing =
                    (Map<String, Object>) statusEvent.get("pricing");

            String category =
                    String.valueOf(pricing.get("category"));

            System.out.println("Charging MessageId = " + messageId +
                    ", Status = " + statusEvent +
                    ", Category = " + category);
            licenseService.processConversationCharge(
                    phoneNumberId,
                    category


            );

            System.out.println("======================================");
            System.out.println("Balance Deducted Successfully");
        //    System.out.println("Amount      : ₹" + amount);
            System.out.println("Message Id  : " + messageId);
            System.out.println("======================================");

        } catch (Exception ex) {

            System.out.println("======================================");
            System.out.println("Balance Deduction Failed");
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            System.out.println("======================================");

        }

    }

}
