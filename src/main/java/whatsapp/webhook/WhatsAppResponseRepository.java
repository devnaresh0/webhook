package whatsapp.webhook;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WhatsAppResponseRepository
        extends JpaRepository<WhatsAppResponse, Long> {
    boolean existsByPoId(String poId);
    boolean existsByPoIdAndPhone(String poId, String phone);
    List<WhatsAppResponse> findByPoId(String poId);
    WhatsAppResponse findTopByPoIdOrderByCreatedAtAsc(String poId);
}
