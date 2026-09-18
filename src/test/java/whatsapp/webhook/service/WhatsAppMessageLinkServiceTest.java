package whatsapp.webhook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import whatsapp.webhook.entity.WhatsAppMessageLink;
import whatsapp.webhook.repository.WhatsAppMessageLinkRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void saveFromSendSupersedesOlderActiveLinkForSameRecipientUser() {
        WhatsAppMessageLink older = new WhatsAppMessageLink();
        older.setId(10L);
        older.setMessageId("wamid.OLD");
        older.setRecipientUserId("2");
        older.setActive(Boolean.TRUE);

        when(linkRepository.findByMessageId("wamid.NEW")).thenReturn(Optional.empty());
        when(linkRepository.findByDomainAndPoIdAndLevelAndRecipientUserIdAndActiveTrue(
                eq("naresh"), eq("3"), eq(1), eq("2")
        )).thenReturn(Collections.singletonList(older));

        Map<String, Object> payload = buildPayload(
                "1|2|3|PO1|75|u|1|naresh|2|1",
                "918580679564"
        );
        String metaBody =
                "{\"messaging_product\":\"whatsapp\",\"messages\":[{\"id\":\"wamid.NEW\"}]}";

        service.saveFromSend("naresh", payload, metaBody);

        assertFalse(older.isActive());
        verify(linkRepository, times(2)).save(any(WhatsAppMessageLink.class));

        ArgumentCaptor<WhatsAppMessageLink> captor =
                ArgumentCaptor.forClass(WhatsAppMessageLink.class);
        verify(linkRepository, times(2)).save(captor.capture());
        WhatsAppMessageLink newest = captor.getAllValues().get(1);
        assertEquals("wamid.NEW", newest.getMessageId());
        assertEquals("2", newest.getRecipientUserId());
        assertTrue(newest.isActive());
    }

    @Test
    void saveFromSendDoesNotSupersedeDifferentRecipientOnSamePhone() {
        when(linkRepository.findByMessageId("wamid.NEW")).thenReturn(Optional.empty());
        when(linkRepository.findByDomainAndPoIdAndLevelAndRecipientUserIdAndActiveTrue(
                eq("naresh"), eq("3"), eq(1), eq("99")
        )).thenReturn(Collections.emptyList());

        Map<String, Object> payload = buildPayload(
                "1|99|3|PO1|75|u|1|naresh|2|1",
                "918580679564"
        );
        service.saveFromSend(
                "naresh",
                payload,
                "{\"messages\":[{\"id\":\"wamid.NEW\"}]}"
        );

        ArgumentCaptor<WhatsAppMessageLink> captor =
                ArgumentCaptor.forClass(WhatsAppMessageLink.class);
        verify(linkRepository, times(1)).save(captor.capture());
        assertEquals("99", captor.getValue().getRecipientUserId());
        assertTrue(captor.getValue().isActive());
    }

    @Test
    void saveFromSendSkipsWhenMessageIdAlreadyExists() {
        when(linkRepository.findByMessageId("wamid.DUP"))
                .thenReturn(Optional.of(new WhatsAppMessageLink()));

        Map<String, Object> payload = buildPayload(
                "1|2|3|PO1|75|u|1|naresh|2|1",
                "918580679564"
        );
        service.saveFromSend(
                "naresh",
                payload,
                "{\"messages\":[{\"id\":\"wamid.DUP\"}]}"
        );

        verify(linkRepository, never()).save(any(WhatsAppMessageLink.class));
    }

    private Map<String, Object> buildPayload(String flowToken, String to) {
        Map<String, Object> action = new HashMap<String, Object>();
        action.put("flow_token", flowToken);
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("action", action);
        Map<String, Object> component = new HashMap<String, Object>();
        component.put("parameters", Arrays.asList(param));
        Map<String, Object> template = new HashMap<String, Object>();
        template.put("components", Arrays.asList(component));
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("template", template);
        payload.put("to", to);
        return payload;
    }
}
