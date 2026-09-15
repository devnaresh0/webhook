package whatsapp.webhook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class WebhookApplication extends SpringBootServletInitializer {

    static {
        // Prevent LocalDateTime.now(UTC) from being re-interpreted as IST on persist
        // (was storing sent_at 5h30m early so Sep-15 IST usage looked empty).
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        return application.sources(WebhookApplication.class);
    }

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(WebhookApplication.class, args);
    }
}
