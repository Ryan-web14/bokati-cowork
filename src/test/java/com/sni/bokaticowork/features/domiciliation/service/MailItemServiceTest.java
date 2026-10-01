package com.sni.bokaticowork.features.domiciliation.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.domiciliation.model.DomiciliationContract;
import com.sni.bokaticowork.features.domiciliation.model.MailItem;
import com.sni.bokaticowork.features.domiciliation.model.MailItemEvent;
import com.sni.bokaticowork.features.domiciliation.model.ServiceDefinition;
import com.sni.bokaticowork.features.domiciliation.repository.MailItemEventRepository;
import com.sni.bokaticowork.features.domiciliation.repository.MailItemRepository;
import com.sni.bokaticowork.features.domiciliation.repository.ServiceDefinitionRepository;
import com.sni.bokaticowork.features.payment.service.support.TransactionContextResolver;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.usage.dto.CreateUsageRecordRequest;
import com.sni.bokaticowork.features.subscription.usage.dto.UsageRecordResponse;
import com.sni.bokaticowork.features.subscription.usage.service.UsageRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le courrier · notification et preuve de remise.
 *
 * <p>Un recommandé se remet contre identité, jamais ne se détruit. Chaque passage laisse une ligne
 * au journal. Ce qui se facture passe par le module d'usage, au prix du catalogue.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MailItemServiceTest {

    @Mock private MailItemRepository mailRepository;
    @Mock private MailItemEventRepository eventRepository;
    @Mock private ServiceDefinitionRepository definitionRepository;
    @Mock private DomiciliationService domiciliationService;
    @Mock private UsageRecordService usageRecordService;
    @Mock private TransactionContextResolver contextResolver;
    @Mock private OutboxService outboxService;
    @Mock private SequenceGeneratorFacade sequenceGenerator;

    @InjectMocks
    private MailItemService service;

    private DomiciliationContract contract;
    private final List<MailItemEvent> journal = new ArrayList<>();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "storageDays", 30);
        Subscription subscription = Subscription.builder().id(1L).subscriptionNumber("SUB-1")
                .subscriberType(SubscriberType.MEMBER).subscriberCode("MBR-1").build();
        contract = DomiciliationContract.builder().id(3L).contractNumber("DOM-2026-00001").subscription(subscription)
                .status(DomiciliationContract.Status.ACTIVE)
                .mailForwardingMode(DomiciliationContract.MailForwardingMode.HOLD).build();
        when(domiciliationService.get("DOM-2026-00001")).thenReturn(contract);
        when(sequenceGenerator.next("mail_item")).thenReturn("MAIL-202609-000001");
        when(mailRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(any())).thenAnswer(invocation -> {
            journal.add(invocation.getArgument(0));
            return invocation.getArgument(0);
        });
        when(contextResolver.resolveParty(anyString(), anyString()))
                .thenReturn(new TransactionContextResolver.PartyView("MEMBER", "MBR-1", "Jane", "jane@example.test", null, null, true));
        when(definitionRepository.findByCode("SVC-MAIL-SCAN")).thenReturn(Optional.of(ServiceDefinition.builder()
                .code("SVC-MAIL-SCAN").unitPrice(new BigDecimal("500")).currency("XAF").usageEntitlementCode("MAIL_SCAN").build()));
        UsageRecordResponse usage = mock(UsageRecordResponse.class);
        when(usage.usageNumber()).thenReturn("USG-1");
        when(usageRecordService.record(any())).thenReturn(usage);
    }

    private MailItem received(MailItem.Type type) {
        MailItem item = service.receive("DOM-2026-00001", new MailItemService.Receipt(type, "Impôts", null, null, null, null), "guichet");
        when(mailRepository.findByItemNumber("MAIL-202609-000001")).thenReturn(Optional.of(item));
        return item;
    }

    @Test
    void recevoirNotifieAussitotEtLaisseDeuxLignesAuJournal() {
        MailItem item = received(MailItem.Type.REGISTERED_LETTER);

        assertEquals(MailItem.Status.NOTIFIED, item.getStatus());
        assertEquals(LocalDate.now().plusDays(30), item.getStorageDeadline());
        assertEquals(List.of("RECEIVED", "NOTIFIED"), journal.stream().map(MailItemEvent::getEventType).toList());
        ArgumentCaptor<String> type = ArgumentCaptor.forClass(String.class);
        verify(outboxService).publish(type.capture(), eq("DOMICILIATION"), eq("MAIL-202609-000001"), any());
        assertEquals("MAIL_RECEIVED", type.getValue());
    }

    @Test
    void unContratInactifNeRecoitPasDeCourrier() {
        contract.setStatus(DomiciliationContract.Status.PENDING_REGISTRATION);

        assertThrows(ConflictException.class, () -> service.receive("DOM-2026-00001",
                new MailItemService.Receipt(MailItem.Type.LETTER, null, null, null, null, null), "guichet"));
    }

    @Test
    void unRecommandeSeRemetContreIdentite() {
        received(MailItem.Type.REGISTERED_LETTER);

        BadRequestException thrown = assertThrows(BadRequestException.class, () -> service.collect("MAIL-202609-000001",
                new MailItemService.Collection("Jane Doe", null, null), "guichet"));
        assertTrue(thrown.getMessage().contains("pièce d'identité"));

        MailItem collected = service.collect("MAIL-202609-000001",
                new MailItemService.Collection("Jane Doe", "CNI 123456", "https://sig/1.png"), "guichet");
        assertEquals(MailItem.Status.COLLECTED, collected.getStatus());
        assertEquals("guichet", collected.getHandedOverBy());
        assertTrue(journal.get(journal.size() - 1).getDetails().contains("CNI 123456"), "La preuve est au journal");
    }

    @Test
    void uneLettreSimpleSeRemetContreUnNom() {
        received(MailItem.Type.LETTER);

        MailItem collected = service.collect("MAIL-202609-000001", new MailItemService.Collection("Jane Doe", null, null), "guichet");

        assertEquals(MailItem.Status.COLLECTED, collected.getStatus());
    }

    @Test
    void unRecommandeNeSeDetruitPas() {
        received(MailItem.Type.LEGAL_NOTICE);

        assertThrows(ConflictException.class, () -> service.destroy("MAIL-202609-000001", "délai dépassé", "guichet"));
    }

    @Test
    void laNumerisationSeFactureAuPrixDuCatalogue() {
        MailItem item = received(MailItem.Type.LETTER);

        service.scan("MAIL-202609-000001", "DOC-SCAN-1", "guichet");

        ArgumentCaptor<CreateUsageRecordRequest> usage = ArgumentCaptor.forClass(CreateUsageRecordRequest.class);
        verify(usageRecordService).record(usage.capture());
        assertEquals("MAIL_SCAN", usage.getValue().entitlementCode());
        assertEquals(0, usage.getValue().billableAmount().compareTo(new BigDecimal("500")));
        assertEquals(Boolean.TRUE, usage.getValue().billable());
        assertEquals("USG-1", item.getUsageNumber());
        assertEquals(0, item.getBilledAmount().compareTo(new BigDecimal("500")));
    }

    @Test
    void sansPrixAuCatalogueRienNEstFactureMaisLActeEstFait() {
        when(definitionRepository.findByCode("SVC-MAIL-SCAN")).thenReturn(Optional.empty());
        MailItem item = received(MailItem.Type.LETTER);

        service.scan("MAIL-202609-000001", "DOC-SCAN-1", "guichet");

        assertEquals(MailItem.Status.SCANNED, item.getStatus());
        verify(usageRecordService, never()).record(any());
    }

    @Test
    void laReexpeditionExigeUnModeEtUneAdresse() {
        received(MailItem.Type.LETTER);

        assertThrows(ConflictException.class, () -> service.forward("MAIL-202609-000001",
                new MailItemService.Forwarding("TRK-1", new BigDecimal("1500")), "guichet"));
    }

    @Test
    void unPliRemisNeBougePlus() {
        received(MailItem.Type.LETTER);
        service.collect("MAIL-202609-000001", new MailItemService.Collection("Jane Doe", null, null), "guichet");

        assertThrows(ConflictException.class, () -> service.scan("MAIL-202609-000001", "DOC-2", "guichet"));
    }
}
