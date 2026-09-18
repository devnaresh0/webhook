package whatsapp.webhook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import whatsapp.webhook.entity.WhatsAppMessageLink;
import whatsapp.webhook.model.WhatsAppResponse;
import whatsapp.webhook.repository.BusinessCredentialRepository;
import whatsapp.webhook.repository.PendingWebhookMessageRepository;
import whatsapp.webhook.repository.WhatsAppMessageLinkRepository;
import whatsapp.webhook.repository.WhatsAppResponseRepository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 50 edge cases for WhatsApp message-link supersede + approval gating.
 */
class MessageLinkSupersedeEdgeCaseTest {

    private static final String DOMAIN = "naresh";
    private static final String PO = "2101205";
    private static final String PO_NUM = "PO00372";
    private static final String PHONE = "918580679564";
    private static final String TOKEN =
            "266966|351901|" + PO + "|" + PO_NUM + "|62|Cashier1|1|" + DOMAIN + "|2|1";

    @Mock
    private WhatsAppMessageLinkRepository linkRepository;
    @Mock
    private WhatsAppResponseRepository responseRepository;
    @Mock
    private BusinessCredentialRepository businessCredentialsRepository;
    @Mock
    private PendingWebhookMessageRepository pendingWebhookMessageRepository;
    @Mock
    private WhatsAppMessageLinkService messageLinkServiceMock;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private WhatsAppMessageLinkService linkService;
    private ApprovalService approvalService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        linkService = new WhatsAppMessageLinkService();
        // field inject for link service
        setField(linkService, "linkRepository", linkRepository);
        setField(linkService, "objectMapper", objectMapper);

        approvalService = new ApprovalService();
        setField(approvalService, "repository", responseRepository);
        setField(approvalService, "businessCredentialsRepository", businessCredentialsRepository);
        setField(approvalService, "objectMapper", objectMapper);
        setField(approvalService, "pendingWebhookMessageRepository", pendingWebhookMessageRepository);
        setField(approvalService, "messageLinkService", messageLinkServiceMock);

        when(businessCredentialsRepository.findByDomain(anyString()))
                .thenReturn(Optional.empty());
    }

    // ---------- EC01–EC10: Meta id & flow_token ----------

    @Test
    @DisplayName("EC01 extractMetaMessageId happy path")
    void ec01() {
        assertEquals("wamid.ABC",
                linkService.extractMetaMessageId(
                        "{\"messages\":[{\"id\":\"wamid.ABC\"}]}"));
    }

    @Test
    @DisplayName("EC02 extractMetaMessageId empty body")
    void ec02() {
        assertNull(linkService.extractMetaMessageId(""));
        assertNull(linkService.extractMetaMessageId(null));
    }

    @Test
    @DisplayName("EC03 extractMetaMessageId invalid json")
    void ec03() {
        assertNull(linkService.extractMetaMessageId("{not-json"));
    }

    @Test
    @DisplayName("EC04 extractMetaMessageId empty messages array")
    void ec04() {
        assertNull(linkService.extractMetaMessageId("{\"messages\":[]}"));
    }

    @Test
    @DisplayName("EC05 extractMetaMessageId blank id")
    void ec05() {
        assertNull(linkService.extractMetaMessageId(
                "{\"messages\":[{\"id\":\"  \"}]}"));
    }

    @Test
    @DisplayName("EC06 findFlowToken top-level")
    void ec06() {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("flow_token", TOKEN);
        assertEquals(TOKEN, linkService.findFlowToken(m));
    }

    @Test
    @DisplayName("EC07 findFlowToken nested in template")
    void ec07() {
        assertEquals(TOKEN, linkService.findFlowToken(buildPayload(TOKEN, PHONE)));
    }

    @Test
    @DisplayName("EC08 findFlowToken null / missing")
    void ec08() {
        assertNull(linkService.findFlowToken(null));
        assertNull(linkService.findFlowToken(new HashMap<String, Object>()));
    }

    @Test
    @DisplayName("EC09 findFlowToken ignores literal null string")
    void ec09() {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("flow_token", "null");
        assertNull(linkService.findFlowToken(m));
    }

    @Test
    @DisplayName("EC10 findFlowToken in list of maps")
    void ec10() {
        Map<String, Object> inner = new HashMap<String, Object>();
        inner.put("flow_token", TOKEN);
        Map<String, Object> outer = new HashMap<String, Object>();
        outer.put("items", Arrays.asList(inner));
        assertEquals(TOKEN, linkService.findFlowToken(outer));
    }

    // ---------- EC11–EC20: saveFromSend guards ----------

    @Test
    @DisplayName("EC11 saveFromSend skips when Meta id missing")
    void ec11() {
        linkService.saveFromSend(DOMAIN, buildPayload(TOKEN, PHONE), "{}");
        verify(linkRepository, never()).save(any(WhatsAppMessageLink.class));
    }

    @Test
    @DisplayName("EC12 saveFromSend skips duplicate message_id")
    void ec12() {
        when(linkRepository.findByMessageId("wamid.DUP"))
                .thenReturn(Optional.of(link(1L, "wamid.DUP", "351901", 1, true)));
        linkService.saveFromSend(DOMAIN, buildPayload(TOKEN, PHONE), meta("wamid.DUP"));
        verify(linkRepository, never()).save(any(WhatsAppMessageLink.class));
    }

    @Test
    @DisplayName("EC13 saveFromSend skips when flow_token missing")
    void ec13() {
        when(linkRepository.findByMessageId("wamid.X")).thenReturn(Optional.empty());
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("to", PHONE);
        linkService.saveFromSend(DOMAIN, payload, meta("wamid.X"));
        verify(linkRepository, never()).save(any(WhatsAppMessageLink.class));
    }

    @Test
    @DisplayName("EC14 saveFromSend skips short flow_token")
    void ec14() {
        when(linkRepository.findByMessageId("wamid.X")).thenReturn(Optional.empty());
        linkService.saveFromSend(DOMAIN, buildPayload("a|b|c", PHONE), meta("wamid.X"));
        verify(linkRepository, never()).save(any(WhatsAppMessageLink.class));
    }

    @Test
    @DisplayName("EC15 saveFromSend uses header domain over token domain")
    void ec15() {
        stubEmptyDeactivate();
        when(linkRepository.findByMessageId("wamid.N")).thenReturn(Optional.empty());
        linkService.saveFromSend("header-dom", buildPayload(TOKEN, PHONE), meta("wamid.N"));
        ArgumentCaptor<WhatsAppMessageLink> cap =
                ArgumentCaptor.forClass(WhatsAppMessageLink.class);
        verify(linkRepository).save(cap.capture());
        assertEquals("header-dom", cap.getValue().getDomain());
    }

    @Test
    @DisplayName("EC16 saveFromSend falls back to token domain")
    void ec16() {
        stubEmptyDeactivate();
        when(linkRepository.findByMessageId("wamid.N")).thenReturn(Optional.empty());
        linkService.saveFromSend("  ", buildPayload(TOKEN, PHONE), meta("wamid.N"));
        ArgumentCaptor<WhatsAppMessageLink> cap =
                ArgumentCaptor.forClass(WhatsAppMessageLink.class);
        verify(linkRepository).save(cap.capture());
        assertEquals(DOMAIN, cap.getValue().getDomain());
    }

    @Test
    @DisplayName("EC17 saveFromSend stores recipientUserId from token")
    void ec17() {
        stubEmptyDeactivate();
        when(linkRepository.findByMessageId("wamid.N")).thenReturn(Optional.empty());
        linkService.saveFromSend(DOMAIN, buildPayload(TOKEN, PHONE), meta("wamid.N"));
        ArgumentCaptor<WhatsAppMessageLink> cap =
                ArgumentCaptor.forClass(WhatsAppMessageLink.class);
        verify(linkRepository).save(cap.capture());
        assertEquals("351901", cap.getValue().getRecipientUserId());
        assertEquals(Integer.valueOf(1), cap.getValue().getLevel());
        assertTrue(cap.getValue().isActive());
    }

    @Test
    @DisplayName("EC18 saveFromSend blank to → phone null")
    void ec18() {
        stubEmptyDeactivate();
        when(linkRepository.findByMessageId("wamid.N")).thenReturn(Optional.empty());
        linkService.saveFromSend(DOMAIN, buildPayload(TOKEN, ""), meta("wamid.N"));
        ArgumentCaptor<WhatsAppMessageLink> cap =
                ArgumentCaptor.forClass(WhatsAppMessageLink.class);
        verify(linkRepository).save(cap.capture());
        assertNull(cap.getValue().getPhone());
    }

    @Test
    @DisplayName("EC19 saveFromSend DataIntegrityViolation ignored")
    void ec19() {
        stubEmptyDeactivate();
        when(linkRepository.findByMessageId("wamid.N")).thenReturn(Optional.empty());
        when(linkRepository.save(any(WhatsAppMessageLink.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("dup"));
        linkService.saveFromSend(DOMAIN, buildPayload(TOKEN, PHONE), meta("wamid.N"));
        verify(linkRepository, times(1)).save(any(WhatsAppMessageLink.class));
    }

    @Test
    @DisplayName("EC20 saveFromSend trims Meta message id")
    void ec20() {
        stubEmptyDeactivate();
        when(linkRepository.findByMessageId("wamid.TRIM")).thenReturn(Optional.empty());
        linkService.saveFromSend(
                DOMAIN,
                buildPayload(TOKEN, PHONE),
                "{\"messages\":[{\"id\":\"  wamid.TRIM  \"}]}");
        ArgumentCaptor<WhatsAppMessageLink> cap =
                ArgumentCaptor.forClass(WhatsAppMessageLink.class);
        verify(linkRepository).save(cap.capture());
        assertEquals("wamid.TRIM", cap.getValue().getMessageId());
    }

    // ---------- EC21–EC30: multi-user / update / levels ----------

    @Test
    @DisplayName("EC21 same-level resend same user deactivates old")
    void ec21() {
        WhatsAppMessageLink older = link(10L, "wamid.OLD", "351901", 1, true);
        when(linkRepository.findByMessageId("wamid.NEW")).thenReturn(Optional.empty());
        when(linkRepository.findByDomainAndPoIdAndLevelAndRecipientUserIdAndActiveTrue(
                DOMAIN, PO, 1, "351901"))
                .thenReturn(Collections.singletonList(older));
        when(linkRepository.findByDomainAndPoIdAndActiveTrueAndLevelLessThan(
                anyString(), anyString(), anyInt()))
                .thenReturn(Collections.emptyList());

        linkService.saveFromSend(DOMAIN, buildPayload(TOKEN, PHONE), meta("wamid.NEW"));

        assertFalse(older.isActive());
        verify(linkRepository, times(2)).save(any(WhatsAppMessageLink.class));
    }

    @Test
    @DisplayName("EC22 different user same phone does NOT deactivate peer")
    void ec22() {
        when(linkRepository.findByMessageId("wamid.B")).thenReturn(Optional.empty());
        when(linkRepository.findByDomainAndPoIdAndLevelAndRecipientUserIdAndActiveTrue(
                DOMAIN, PO, 1, "489151"))
                .thenReturn(Collections.emptyList());
        when(linkRepository.findByDomainAndPoIdAndActiveTrueAndLevelLessThan(
                anyString(), anyString(), anyInt()))
                .thenReturn(Collections.emptyList());

        String tokenB =
                "266966|489151|" + PO + "|" + PO_NUM + "|62|ary1|1|" + DOMAIN + "|2|1";
        linkService.saveFromSend(DOMAIN, buildPayload(tokenB, PHONE), meta("wamid.B"));

        verify(linkRepository, times(1)).save(any(WhatsAppMessageLink.class));
        verify(linkRepository).findByDomainAndPoIdAndLevelAndRecipientUserIdAndActiveTrue(
                DOMAIN, PO, 1, "489151");
    }

    @Test
    @DisplayName("EC23 L2 send deactivates active L1 links")
    void ec23() {
        WhatsAppMessageLink l1a = link(1L, "wamid.L1A", "351901", 1, true);
        WhatsAppMessageLink l1b = link(2L, "wamid.L1B", "489151", 1, true);
        when(linkRepository.findByMessageId("wamid.L2")).thenReturn(Optional.empty());
        when(linkRepository.findByDomainAndPoIdAndLevelAndRecipientUserIdAndActiveTrue(
                DOMAIN, PO, 2, "351902"))
                .thenReturn(Collections.emptyList());
        when(linkRepository.findByDomainAndPoIdAndActiveTrueAndLevelLessThan(
                DOMAIN, PO, 2))
                .thenReturn(Arrays.asList(l1a, l1b));

        String tokenL2 =
                "266966|351902|" + PO + "|" + PO_NUM + "|62|Sup|2|" + DOMAIN + "|2|1";
        linkService.saveFromSend(DOMAIN, buildPayload(tokenL2, PHONE), meta("wamid.L2"));

        assertFalse(l1a.isActive());
        assertFalse(l1b.isActive());
        verify(linkRepository, times(3)).save(any(WhatsAppMessageLink.class));
    }

    @Test
    @DisplayName("EC24 L1 send does not query lower-level links")
    void ec24() {
        when(linkRepository.findByMessageId("wamid.L1")).thenReturn(Optional.empty());
        when(linkRepository.findByDomainAndPoIdAndLevelAndRecipientUserIdAndActiveTrue(
                DOMAIN, PO, 1, "351901"))
                .thenReturn(Collections.emptyList());

        linkService.saveFromSend(DOMAIN, buildPayload(TOKEN, PHONE), meta("wamid.L1"));

        verify(linkRepository, never())
                .findByDomainAndPoIdAndActiveTrueAndLevelLessThan(
                        anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("EC25 deactivateOlderLinks skips blank recipientUserId")
    void ec25() {
        assertEquals(0, linkService.deactivateOlderLinks(DOMAIN, PO, 1, "  "));
        assertEquals(0, linkService.deactivateOlderLinks(DOMAIN, PO, 1, null));
        verify(linkRepository, never()).save(any(WhatsAppMessageLink.class));
    }

    @Test
    @DisplayName("EC26 deactivateOlderLinks empty list → 0")
    void ec26() {
        when(linkRepository.findByDomainAndPoIdAndLevelAndRecipientUserIdAndActiveTrue(
                DOMAIN, PO, 1, "351901"))
                .thenReturn(Collections.emptyList());
        assertEquals(0, linkService.deactivateOlderLinks(DOMAIN, PO, 1, "351901"));
    }

    @Test
    @DisplayName("EC27 deactivateLowerLevelLinks level 1 → 0")
    void ec27() {
        assertEquals(0, linkService.deactivateLowerLevelLinks(DOMAIN, PO, 1));
        verify(linkRepository, never())
                .findByDomainAndPoIdAndActiveTrueAndLevelLessThan(
                        anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("EC28 L3 send deactivates L1 and L2")
    void ec28() {
        WhatsAppMessageLink l1 = link(1L, "wamid.L1", "1", 1, true);
        WhatsAppMessageLink l2 = link(2L, "wamid.L2", "2", 2, true);
        when(linkRepository.findByDomainAndPoIdAndActiveTrueAndLevelLessThan(
                DOMAIN, PO, 3))
                .thenReturn(Arrays.asList(l1, l2));
        assertEquals(2, linkService.deactivateLowerLevelLinks(DOMAIN, PO, 3));
        assertFalse(l1.isActive());
        assertFalse(l2.isActive());
    }

    @Test
    @DisplayName("EC29 update same user new task_id supersedes prior L2")
    void ec29() {
        WhatsAppMessageLink oldL2 = link(65L, "wamid.OLD_L2", "351902", 2, true);
        when(linkRepository.findByMessageId("wamid.NEW_L2")).thenReturn(Optional.empty());
        when(linkRepository.findByDomainAndPoIdAndLevelAndRecipientUserIdAndActiveTrue(
                DOMAIN, PO, 2, "351902"))
                .thenReturn(Collections.singletonList(oldL2));
        when(linkRepository.findByDomainAndPoIdAndActiveTrueAndLevelLessThan(
                DOMAIN, PO, 2))
                .thenReturn(Collections.emptyList());

        String token =
                "266972|351902|" + PO + "|" + PO_NUM + "|62|Cashier1|2|" + DOMAIN + "|2|1";
        linkService.saveFromSend(DOMAIN, buildPayload(token, PHONE), meta("wamid.NEW_L2"));

        assertFalse(oldL2.isActive());
        ArgumentCaptor<WhatsAppMessageLink> cap =
                ArgumentCaptor.forClass(WhatsAppMessageLink.class);
        verify(linkRepository, atLeastOnce()).save(cap.capture());
        WhatsAppMessageLink newest = null;
        for (WhatsAppMessageLink saved : cap.getAllValues()) {
            if ("wamid.NEW_L2".equals(saved.getMessageId())) {
                newest = saved;
            }
        }
        assertTrue(newest != null && newest.isActive());
        assertEquals("266972", newest.getTaskId());
    }

    @Test
    @DisplayName("EC30 findByMessageId blank → empty")
    void ec30() {
        assertFalse(linkService.findByMessageId(null).isPresent());
        assertFalse(linkService.findByMessageId("  ").isPresent());
    }

    // ---------- EC31–EC40: isMessageSuperseded ----------

    @Test
    @DisplayName("EC31 isMessageSuperseded null/blank → false")
    void ec31() {
        assertFalse(linkService.isMessageSuperseded(null));
        assertFalse(linkService.isMessageSuperseded(""));
    }

    @Test
    @DisplayName("EC32 unknown message_id → false (legacy allow)")
    void ec32() {
        when(linkRepository.findByMessageId("wamid.UNKNOWN"))
                .thenReturn(Optional.empty());
        assertFalse(linkService.isMessageSuperseded("wamid.UNKNOWN"));
    }

    @Test
    @DisplayName("EC33 inactive link → true")
    void ec33() {
        when(linkRepository.findByMessageId("wamid.OLD"))
                .thenReturn(Optional.of(link(1L, "wamid.OLD", "351901", 1, false)));
        assertTrue(linkService.isMessageSuperseded("wamid.OLD"));
    }

    @Test
    @DisplayName("EC34 active L1 but active L2 exists → true")
    void ec34() {
        WhatsAppMessageLink l1 = link(1L, "wamid.L1", "351901", 1, true);
        when(linkRepository.findByMessageId("wamid.L1")).thenReturn(Optional.of(l1));
        when(linkRepository.existsByDomainAndPoIdAndActiveTrueAndLevelGreaterThan(
                DOMAIN, PO, 1)).thenReturn(true);
        assertTrue(linkService.isMessageSuperseded("wamid.L1"));
    }

    @Test
    @DisplayName("EC35 active latest L1 no higher → false")
    void ec35() {
        WhatsAppMessageLink l1 = link(1L, "wamid.L1", "351901", 1, true);
        when(linkRepository.findByMessageId("wamid.L1")).thenReturn(Optional.of(l1));
        when(linkRepository.existsByDomainAndPoIdAndActiveTrueAndLevelGreaterThan(
                DOMAIN, PO, 1)).thenReturn(false);
        when(linkRepository.findByDomainAndPoIdAndLevelAndRecipientUserIdAndActiveTrue(
                DOMAIN, PO, 1, "351901"))
                .thenReturn(Collections.singletonList(l1));
        assertFalse(linkService.isMessageSuperseded("wamid.L1"));
    }

    @Test
    @DisplayName("EC36 older active when newer id exists for same user → true")
    void ec36() {
        WhatsAppMessageLink older = link(10L, "wamid.OLD", "351901", 1, true);
        WhatsAppMessageLink newer = link(20L, "wamid.NEW", "351901", 1, true);
        when(linkRepository.findByMessageId("wamid.OLD")).thenReturn(Optional.of(older));
        when(linkRepository.existsByDomainAndPoIdAndActiveTrueAndLevelGreaterThan(
                DOMAIN, PO, 1)).thenReturn(false);
        when(linkRepository.findByDomainAndPoIdAndLevelAndRecipientUserIdAndActiveTrue(
                DOMAIN, PO, 1, "351901"))
                .thenReturn(Arrays.asList(older, newer));
        assertTrue(linkService.isMessageSuperseded("wamid.OLD"));
    }

    @Test
    @DisplayName("EC37 newer by createdAt when same id collision path → true")
    void ec37() {
        LocalDateTime t1 = LocalDateTime.of(2026, 9, 18, 3, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 9, 18, 4, 0);
        WhatsAppMessageLink older = link(10L, "wamid.OLD", "351901", 1, true);
        older.setCreatedAt(t1);
        WhatsAppMessageLink newer = link(10L, "wamid.NEW", "351901", 1, true);
        newer.setCreatedAt(t2);
        when(linkRepository.findByMessageId("wamid.OLD")).thenReturn(Optional.of(older));
        when(linkRepository.existsByDomainAndPoIdAndActiveTrueAndLevelGreaterThan(
                DOMAIN, PO, 1)).thenReturn(false);
        when(linkRepository.findByDomainAndPoIdAndLevelAndRecipientUserIdAndActiveTrue(
                DOMAIN, PO, 1, "351901"))
                .thenReturn(Arrays.asList(older, newer));
        assertTrue(linkService.isMessageSuperseded("wamid.OLD"));
    }

    @Test
    @DisplayName("EC38 peer same level different user does not supersede")
    void ec38() {
        WhatsAppMessageLink a = link(1L, "wamid.A", "351901", 1, true);
        when(linkRepository.findByMessageId("wamid.A")).thenReturn(Optional.of(a));
        when(linkRepository.existsByDomainAndPoIdAndActiveTrueAndLevelGreaterThan(
                DOMAIN, PO, 1)).thenReturn(false);
        when(linkRepository.findByDomainAndPoIdAndLevelAndRecipientUserIdAndActiveTrue(
                DOMAIN, PO, 1, "351901"))
                .thenReturn(Collections.singletonList(a));
        assertFalse(linkService.isMessageSuperseded("wamid.A"));
    }

    @Test
    @DisplayName("EC39 active L2 is current — not superseded")
    void ec39() {
        WhatsAppMessageLink l2 = link(66L, "wamid.L2", "351902", 2, true);
        when(linkRepository.findByMessageId("wamid.L2")).thenReturn(Optional.of(l2));
        when(linkRepository.existsByDomainAndPoIdAndActiveTrueAndLevelGreaterThan(
                DOMAIN, PO, 2)).thenReturn(false);
        when(linkRepository.findByDomainAndPoIdAndLevelAndRecipientUserIdAndActiveTrue(
                DOMAIN, PO, 2, "351902"))
                .thenReturn(Collections.singletonList(l2));
        assertFalse(linkService.isMessageSuperseded("wamid.L2"));
    }

    @Test
    @DisplayName("EC40 trims message id before lookup")
    void ec40() {
        WhatsAppMessageLink inactive = link(1L, "wamid.X", "1", 1, false);
        when(linkRepository.findByMessageId("wamid.X"))
                .thenReturn(Optional.of(inactive));
        assertTrue(linkService.isMessageSuperseded("  wamid.X  "));
    }

    // ---------- EC41–EC50: ApprovalService ----------

    @Test
    @DisplayName("EC41 invalid flow_token does not save")
    void ec41() {
        Map<String, Object> json = new HashMap<String, Object>();
        json.put("flow_token", "bad");
        approvalService.processApproval(PHONE, "APPROVE", json, "u", "r", "wamid.X");
        verify(responseRepository, never()).save(any(WhatsAppResponse.class));
    }

    @Test
    @DisplayName("EC42 missing flow_token does not save")
    void ec42() {
        approvalService.processApproval(
                PHONE, "APPROVE", new HashMap<String, Object>(), "u", "r", null);
        verify(responseRepository, never()).save(any(WhatsAppResponse.class));
    }

    @Test
    @DisplayName("EC43 already approved blocks before supersede")
    void ec43() {
        stubAlreadyApproved();
        when(messageLinkServiceMock.isMessageSuperseded(anyString())).thenReturn(true);

        approvalService.processApproval(
                PHONE, "APPROVE", responseJson(TOKEN), "u", "r", "wamid.ANY");

        verify(responseRepository, never()).save(any(WhatsAppResponse.class));
        verify(messageLinkServiceMock, never()).isMessageSuperseded(anyString());
    }

    @Test
    @DisplayName("EC44 peer second click → already approved path")
    void ec44() {
        stubAlreadyApproved();
        approvalService.processApproval(
                PHONE, "APPROVE", responseJson(TOKEN), "peer", "r", "wamid.PEER");
        verify(responseRepository, never()).save(any(WhatsAppResponse.class));
    }

    @Test
    @DisplayName("EC45 superseded inactive message does not save")
    void ec45() {
        stubNotApprovedYet();
        when(messageLinkServiceMock.isMessageSuperseded("wamid.OLD")).thenReturn(true);

        approvalService.processApproval(
                PHONE, "APPROVE", responseJson(TOKEN), "u", "r", "wamid.OLD");

        verify(responseRepository, never()).save(any(WhatsAppResponse.class));
    }

    @Test
    @DisplayName("EC46 active current message saves approval")
    void ec46() {
        stubNotApprovedYet();
        when(messageLinkServiceMock.isMessageSuperseded("wamid.NEW")).thenReturn(false);

        approvalService.processApproval(
                PHONE, "APPROVE", responseJson(TOKEN), "u", "Accept", "wamid.NEW");

        verify(responseRepository, times(1)).save(any(WhatsAppResponse.class));
    }

    @Test
    @DisplayName("EC47 null contextMessageId still allows approve if not duplicate")
    void ec47() {
        stubNotApprovedYet();
        approvalService.processApproval(
                PHONE, "APPROVE", responseJson(TOKEN), "u", "r", null);
        verify(responseRepository, times(1)).save(any(WhatsAppResponse.class));
        verify(messageLinkServiceMock, never()).isMessageSuperseded(anyString());
    }

    @Test
    @DisplayName("EC48 blank contextMessageId skips supersede check")
    void ec48() {
        stubNotApprovedYet();
        approvalService.processApproval(
                PHONE, "APPROVE", responseJson(TOKEN), "u", "r", "  ");
        verify(responseRepository, times(1)).save(any(WhatsAppResponse.class));
    }

    @Test
    @DisplayName("EC49 REJECT on superseded message blocked")
    void ec49() {
        stubNotApprovedYet();
        when(messageLinkServiceMock.isMessageSuperseded("wamid.OLD")).thenReturn(true);
        approvalService.processApproval(
                PHONE, "REJECT", responseJson(TOKEN), "u", "no", "wamid.OLD");
        verify(responseRepository, never()).save(any(WhatsAppResponse.class));
    }

    @Test
    @DisplayName("EC50 L1 after L2 flagged superseded — no save")
    void ec50() {
        stubNotApprovedYet();
        when(messageLinkServiceMock.isMessageSuperseded("wamid.L1")).thenReturn(true);
        approvalService.processApproval(
                PHONE, "APPROVE", responseJson(TOKEN), "u", "r", "wamid.L1");
        verify(responseRepository, never()).save(any(WhatsAppResponse.class));
        verify(messageLinkServiceMock).isMessageSuperseded("wamid.L1");
    }

    // ---------- helpers ----------

    private void stubEmptyDeactivate() {
        when(linkRepository.findByDomainAndPoIdAndLevelAndRecipientUserIdAndActiveTrue(
                anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(Collections.emptyList());
        when(linkRepository.findByDomainAndPoIdAndActiveTrueAndLevelLessThan(
                anyString(), anyString(), anyInt()))
                .thenReturn(Collections.emptyList());
    }

    private void stubAlreadyApproved() {
        when(responseRepository.existsByDomainAndTaskIdAndLevel(DOMAIN, "266966", 1))
                .thenReturn(true);
        WhatsAppResponse existing = new WhatsAppResponse();
        existing.setUserName("Cashier1");
        when(responseRepository.findByDomainAndTaskIdAndLevel(DOMAIN, "266966", 1))
                .thenReturn(Collections.singletonList(existing));
    }

    private void stubNotApprovedYet() {
        when(responseRepository.existsByDomainAndTaskIdAndLevel(anyString(), anyString(), anyInt()))
                .thenReturn(false);
        when(responseRepository.existsByTaskIdAndLevel(anyString(), anyInt())).thenReturn(false);
        when(responseRepository.findByPoIdAndLevel(anyString(), anyInt()))
                .thenReturn(Collections.emptyList());
    }

    private static Map<String, Object> responseJson(String token) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("flow_token", token);
        return m;
    }

    private static String meta(String id) {
        return "{\"messaging_product\":\"whatsapp\",\"messages\":[{\"id\":\"" + id + "\"}]}";
    }

    private static Map<String, Object> buildPayload(String flowToken, String to) {
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

    private static WhatsAppMessageLink link(
            Long id, String messageId, String userId, int level, boolean active) {
        WhatsAppMessageLink l = new WhatsAppMessageLink();
        l.setId(id);
        l.setMessageId(messageId);
        l.setDomain(DOMAIN);
        l.setTaskId("266966");
        l.setPoId(PO);
        l.setPoNumber(PO_NUM);
        l.setLevel(level);
        l.setPhone(PHONE);
        l.setRecipientUserId(userId);
        l.setActive(active);
        l.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        return l;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
