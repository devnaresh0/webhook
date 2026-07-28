package whatsapp.webhook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class LicenseService {

    @Autowired
    private LicenseRepository licenseRepository;

    public ActivationResponse activate(ActivationRequest request) {

        ActivationResponse response = new ActivationResponse();

        System.out.println("========== LICENSE ACTIVATION ==========");
        System.out.println("Request Token          : " + request.getToken());
        System.out.println("Request Activation Key : " + request.getActivationKey());
        System.out.println("Request Serial Number  : " + request.getSerialNumber());
        System.out.println("Request Domain         : " + request.getDomain());

        License license = licenseRepository.findByToken(request.getToken());

        if (license == null) {
            System.out.println("ERROR : Invalid Token");

            response.setSuccess(false);
            response.setMessage("Invalid Token");
            return response;
        }

        System.out.println("---------- DATABASE VALUES ----------");
        System.out.println("DB Token          : " + license.getToken());
        System.out.println("DB Activation Key : " + license.getActivationKey());
        System.out.println("DB Serial Number  : " + license.getSerialNumber());
        System.out.println("DB Domain         : " + license.getDomain());
        System.out.println("DB Expiry         : " + license.getExpiryDate());
        System.out.println("DB Active         : " + license.isActive());

        if (!license.getActivationKey().equals(request.getActivationKey())) {

            System.out.println("ERROR : Activation Key Mismatch");

            response.setSuccess(false);
            response.setMessage("Invalid Activation Key");
            return response;
        }

        if (!license.getSerialNumber().equalsIgnoreCase(request.getSerialNumber())) {

            System.out.println("ERROR : Serial Number Mismatch");

            response.setSuccess(false);
            response.setMessage("Invalid Serial Number");
            return response;
        }

        if (!license.getDomain().equalsIgnoreCase(request.getDomain())) {

            System.out.println("ERROR : Domain Mismatch");

            response.setSuccess(false);
            response.setMessage("Domain Mismatch");
            return response;
        }

        if (license.getExpiryDate() != null &&
                license.getExpiryDate().before(new Date())) {

            System.out.println("ERROR : License Expired");

            response.setSuccess(false);
            response.setMessage("License Expired");
            return response;
        }

        if (!license.isActive()) {

            System.out.println("ERROR : License Disabled");

            response.setSuccess(false);
            response.setMessage("License Disabled");
            return response;
        }

        System.out.println("SUCCESS : License Activated Successfully");

        response.setSuccess(true);
        response.setMessage("License Activated Successfully");
        response.setCompanyName(license.getCompanyName());
        response.setPlan(license.getPlan());

        return response;
    }

    public License getLicenseByDomain(String domain) {

        return licenseRepository.findByDomain(domain)
                .orElseThrow(() -> new RuntimeException("License not found"));
    }
    public Double getBalance(String domain) {

        License license = licenseRepository.findByDomain(domain)
                .orElseThrow(() -> new RuntimeException("License not found"));

        return license.getBalance();
    }
}
