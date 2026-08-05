package whatsapp.webhook.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import whatsapp.webhook.entity.BusinessCredentials;
import whatsapp.webhook.repository.BusinessCredentialRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;

@Component
public class LicenseInterceptor implements HandlerInterceptor {

    @Autowired
    private BusinessCredentialRepository credentialRepository;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String domain = request.getHeader("Domain");

        if (domain == null || domain.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Missing Domain Header");
            return false;
        }

        BusinessCredentials credentials = credentialRepository
                .findByDomain(domain)
                .orElse(null);

        if (credentials == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid Domain");
            return false;
        }

        if (!"ACTIVE".equalsIgnoreCase(credentials.getLicenseStatus())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "License Inactive");
            return false;
        }

        if (credentials.getExpiresAt() != null &&
                credentials.getExpiresAt().isBefore(LocalDateTime.now())) {

            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "License Expired");
            return false;
        }

        return true;
    }
}