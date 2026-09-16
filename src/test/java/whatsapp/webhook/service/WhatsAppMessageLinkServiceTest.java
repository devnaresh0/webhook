package whatsapp.webhook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import whatsapp.webhook.repository.WhatsAppMessageLinkRepository;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WhatsAppMessageLinkServiceTest {

    @Mock
    private WhatsAppMessageLinkRepository linkRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private WhatsAppMessageLinkService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void extractsMetaMessageId() {
        String body = "{\"messaging_product\":\"whatsapp\",\"messages\":[{\"id\":\"wamid.ABC123\"}]}";
        assertEquals("wamid.ABC123", service.extractMetaMessageId(body));
    }

    @Test
    void findsNestedFlowToken() {
        Map<String, Object> action = new HashMap<String, Object>();
        action.put("flow_token", "1|2|3|PO1|75|u|1|naresh|2|1");
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("action", action);
        Map<String, Object> component = new HashMap<String, Object>();
        component.put("parameters", Arrays.asList(param));
        Map<String, Object> template = new HashMap<String, Object>();
        template.put("components", Arrays.asList(component));
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("template", template);
        payload.put("to", "918580679564");

        assertEquals(
                "1|2|3|PO1|75|u|1|naresh|2|1",
                service.findFlowToken(payload)
        );
    }

    @Test
    void missingMetaIdReturnsNull() {
        assertNull(service.extractMetaMessageId("{}"));
    }
}
