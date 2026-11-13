package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.communication.whatsapp.WhatsAppMessageRepository;
import com.sni.bokaticowork.core.communication.whatsapp.WhatsAppMessageSender;
import com.sni.bokaticowork.core.utils.validation.EmailRecipients;
import com.sni.bokaticowork.features.billing.dto.request.BillingRecipientPreferenceRequest;
import com.sni.bokaticowork.features.billing.dto.request.SendDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingSendingResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentSending;
import com.sni.bokaticowork.features.billing.model.BillingRecipientPreference;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentSendingRepository;
import com.sni.bokaticowork.features.billing.repository.BillingRecipientPreferenceRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingEmailService;
import com.sni.bokaticowork.features.billing.service.support.MemberBillingContact;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lot B4 : une facture d'organisation part a deux adresses en copie, plus une adresse saisie ; le
 * journal des envois montre les trois ; le statut de la facture ne change pas.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillingSendingB4Test {

    @Mock private BillingDocumentServiceImpl billing;
    @Mock private BillingEmailService emails;
    @Mock private BillingDocumentSendingRepository sendings;
    @Mock private BillingRecipientPreferenceRepository preferences;
    @Mock private MemberBillingContact memberContact;
    @Mock private StaffProfileService staffProfiles;
    @Mock private ObjectProvider<WhatsAppMessageSender> whatsApp;
    @Mock private ObjectProvider<WhatsAppMessageRepository> whatsAppMessages;
    @Mock private WhatsAppMessageSender sender;

    private BillingSendingService service;

    private BillingDocument invoice;

    @BeforeEach
    void setUp() {
        // Deux fournisseurs du meme type brut : construction explicite plutot que @InjectMocks.
        service = new BillingSendingService(billing, emails, sendings, preferences, memberContact, staffProfiles, whatsApp, whatsAppMessages);
        invoice = BillingDocument.builder().id(9L).documentNumber("FAC-9").documentType(BillingDocumentType.INVOICE)
                .status(BillingDocumentStatus.VALIDATED).customerType("CUSTOMER").customerCode("ORG-1").customerName("Awa Mbemba")
                .customerEmail("contact@org.cg").customerPhone("+242061135836").balanceDue(new BigDecimal("150000"))
                .totalAmount(new BigDecimal("150000")).paidAmount(BigDecimal.ZERO).dueDate(LocalDate.of(2026, 11, 30)).locked(true).build();
        when(billing.serviceByNumber("FAC-9")).thenReturn(invoice);
        when(staffProfiles.currentAuthor()).thenReturn(new StaffProfileService.Author("awa@elleaose.com", "Awa Mbemba", "Commerciale"));
        when(sendings.save(any(BillingDocumentSending.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(preferences.findByCustomerTypeAndCustomerCode(anyString(), anyString())).thenReturn(Optional.empty());
        when(emails.sendDocumentTo(anyString(), anyList(), anyList(), anyList(), any())).thenReturn("Votre facture FAC-9");
        when(whatsApp.getIfAvailable()).thenReturn(sender);
    }

    @Test
    void shouldSendToTheTypedAddressWithTwoCopiesAndLogTheThree() {
        List<BillingSendingResponse> sent = service.send("FAC-9", new SendDocumentRequest(List.of("dg@org.cg"),
                List.of("compta@org.cg", "achats@org.cg"), List.of(), List.of("EMAIL"), null, "Bonne réception"));

        assertThat(sent).singleElement().satisfies(sending -> {
            assertThat(sending.to()).containsExactly("dg@org.cg");
            assertThat(sending.cc()).containsExactly("compta@org.cg", "achats@org.cg");
            assertThat(sending.status()).isEqualTo("QUEUED");
            assertThat(sending.sentByName()).isEqualTo("Awa Mbemba");
        });
        verify(emails).sendDocumentTo(eq("FAC-9"), eq(List.of("dg@org.cg")), eq(List.of("compta@org.cg", "achats@org.cg")), eq(List.of()),
                eq("Bonne réception"));
        // L'envoi est trace sur la piece sans changer son statut.
        verify(billing).send("FAC-9");
        assertThat(invoice.getStatus()).isEqualTo(BillingDocumentStatus.VALIDATED);
    }

    @Test
    void shouldUseTheCustomerPreferenceWhenNothingIsChosen() {
        when(preferences.findByCustomerTypeAndCustomerCode("CUSTOMER", "ORG-1")).thenReturn(Optional.of(BillingRecipientPreference.builder()
                .toAddresses("compta@org.cg").ccAddresses("dg@org.cg").channels("EMAIL").build()));

        List<BillingSendingResponse> sent = service.send("FAC-9", null);

        assertThat(sent.get(0).to()).containsExactly("compta@org.cg");
        assertThat(sent.get(0).cc()).containsExactly("dg@org.cg");
    }

    @Test
    void shouldSendAPaymentReminderByWhatsAppForAnUnpaidInvoice() {
        when(sender.send(anyString(), anyString(), anyList(), any(), anyString(), anyString()))
                .thenReturn(new WhatsAppMessageSender.Result("msg-1", "TEMPLATE", "rappel_paiement", "PENDING", "PENDING_ENROUTE"));

        List<BillingSendingResponse> sent = service.send("FAC-9", new SendDocumentRequest(null, null, null, List.of("WHATSAPP"), null, null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> values = ArgumentCaptor.forClass(List.class);
        verify(sender).send(eq("+242061135836"), eq("PAYMENT_REMINDER"), values.capture(), any(), eq("FAC-9"), eq("factures"));
        assertThat(values.getValue()).containsExactly("Awa", "FAC-9", "150 000", "30/11/2026");
        assertThat(sent.get(0).providerReference()).isEqualTo("msg-1");
        assertThat(sent.get(0).channel()).isEqualTo("WHATSAPP");
    }

    @Test
    void shouldSaveTheRecipientPreference() {
        when(preferences.save(any(BillingRecipientPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.saveRecipients("CUSTOMER", "ORG-1", new BillingRecipientPreferenceRequest(List.of("compta@org.cg"), List.of("dg@org.cg"),
                null, List.of("+242055645569"), List.of("email", "whatsapp")));

        ArgumentCaptor<BillingRecipientPreference> saved = ArgumentCaptor.forClass(BillingRecipientPreference.class);
        verify(preferences).save(saved.capture());
        assertThat(saved.getValue().getChannels()).isEqualTo("EMAIL,WHATSAPP");
        assertThat(saved.getValue().getCcAddresses()).isEqualTo("dg@org.cg");
    }

    @Test
    void shouldEncodeCopiesForTheMailQueue() {
        String encoded = EmailRecipients.encode(List.of("dg@org.cg"), List.of("compta@org.cg", "achats@org.cg"), List.of("archive@elleaose.com"));

        EmailRecipients parsed = EmailRecipients.parse(encoded);

        assertThat(parsed.to()).isEqualTo("dg@org.cg");
        assertThat(parsed.cc()).containsExactly("compta@org.cg", "achats@org.cg");
        assertThat(parsed.bcc()).containsExactly("archive@elleaose.com");
        assertThat(EmailRecipients.parse("a@x.cg, b@y.cg").to()).isEqualTo("a@x.cg, b@y.cg");
    }
}
