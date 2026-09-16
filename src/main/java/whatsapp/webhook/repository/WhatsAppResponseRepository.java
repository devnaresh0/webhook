package whatsapp.webhook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import whatsapp.webhook.model.WhatsAppResponse;

import java.util.List;
import java.util.Optional;

public interface WhatsAppResponseRepository
        extends JpaRepository<WhatsAppResponse, Long> {
    boolean existsByPoId(String poId);
    boolean existsByPoIdAndPhone(String poId, String phone);
    List<WhatsAppResponse> findByPoId(String poId);
    WhatsAppResponse findTopByPoIdOrderByCreatedAtAsc(String poId);
    boolean existsByTaskId(String taskId);

    WhatsAppResponse findTopByTaskIdOrderByCreatedAtAsc(String taskId);

    List<WhatsAppResponse> findByTaskId(String taskId);
    List<WhatsAppResponse> findByTaskIdAndLevel(
            String taskId,
            int level
    );

    WhatsAppResponse findTopByPoIdAndLevelOrderByCreatedAtAsc(String poId, int level);

    List<WhatsAppResponse> findByPoIdAndLevel(String poId, int level);

    boolean existsByTaskIdAndLevel(String taskId, int level);

    boolean existsByDomainAndTaskIdAndLevel(
            String domain,
            String taskId,
            int level
    );

    List<WhatsAppResponse> findByDomainAndTaskIdAndLevel(
            String domain,
            String taskId,
            int level
    );

    Optional<WhatsAppResponse> findFirstByDomainAndTaskIdAndLevelOrderByIdAsc(
            String domain,
            String taskId,
            int level
    );
}
