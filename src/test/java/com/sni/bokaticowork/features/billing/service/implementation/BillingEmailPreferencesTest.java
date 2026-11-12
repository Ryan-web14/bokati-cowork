package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.enums.EventType;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentPdfService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.billing.service.support.BillingReceivables;
import com.sni.bokaticowork.features.billing.service.support.MemberBillingContact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.thymeleaf.context.IContext;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Les preferences du membre devant l'envoi d'une facture.
 *
 * <p>L'envoi automatique se coupe ; le renvoi demande par un administrateur et la relance partent
 * toujours ; l'adresse de facturation du membre passe avant celle du compte.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillingEmailPreferencesTest {

    @Mock private BillingDocumentService documentService;
    @Mock private BillingDocumentPdfService pdfService;
    @Mock private DefaultEmailSender emailSender;
    @Mock private SpringTemplateEngine templateEngine;
    @Mock private MemberBillingContact memberContact;

    private BillingEmailServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        service = new BillingEmailServiceImpl(documentService, pdfService, emailSender, templateEngine, Locale.FRENCH, memberContact);
        when(documentService.get("INV-1")).thenReturn(document());
        when(pdfService.generatePdf("INV-1")).thenReturn(new byte[]{1});
        when(templateEngine.process(anyString(), any(IContext.class))).thenReturn("<html/>");
        when(emailSender.sendHtmlEmailWithPdfAttachment(anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(true));
        when(emailSender.sendHtmlEmail(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(true));
        when(memberContact.recipient("MEMBER", "MBR-1", "joel@example.com")).thenReturn("compta@example.com");
    }

    @Test
    @DisplayName("Envoi automatique maintenu par defaut, a l'adresse de facturation du membre")
    void automaticSendByDefault() {
        when(memberContact.allowsEmail("MEMBER", "MBR-1", EventType.INVOICE_ISSUED)).thenReturn(true);

        assertThat(service.sendDocumentIfAllowed("INV-1", EventType.INVOICE_ISSUED)).isTrue();

        verify(emailSender).sendHtmlEmailWithPdfAttachment(eq("compta@example.com"), anyString(), anyString(), eq("INV-1.pdf"), any());
    }

    @Test
    @DisplayName("Envoi automatique coupe : rien ne part, pas d'erreur, pas d'echec")
    void automaticSendCut() {
        when(memberContact.allowsEmail("MEMBER", "MBR-1", EventType.INVOICE_ISSUED)).thenReturn(false);

        assertThat(service.sendDocumentIfAllowed("INV-1", EventType.INVOICE_ISSUED)).isFalse();
        service.sendDocumentAsync("INV-1");

        verify(emailSender, never()).sendHtmlEmailWithPdfAttachment(anyString(), anyString(), anyString(), anyString(), any());
        verify(pdfService, never()).generatePdf(anyString());
    }

    @Test
    @DisplayName("Renvoi par un administrateur ou relance : part meme si le membre a coupe")
    void explicitSendIgnoresPreference() {
        when(memberContact.allowsEmail(anyString(), anyString(), any())).thenReturn(false);

        assertThat(service.sendDocument("INV-1")).isTrue();

        verify(emailSender).sendHtmlEmailWithPdfAttachment(eq("compta@example.com"), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Confirmation de reglement coupee par PAYMENT_RECEIVED")
    void paymentConfirmationCut() throws Exception {
        when(memberContact.allowsEmail("MEMBER", "MBR-1", EventType.PAYMENT_RECEIVED)).thenReturn(false);
        service.sendPaymentConfirmation(document(), null);
        verify(emailSender, never()).sendHtmlEmail(anyString(), anyString(), anyString());

        when(memberContact.allowsEmail("MEMBER", "MBR-1", EventType.PAYMENT_RECEIVED)).thenReturn(true);
        service.sendPaymentConfirmation(document(), null);
        verify(emailSender).sendHtmlEmail(eq("compta@example.com"), anyString(), anyString());
    }

    private BillingDocumentResponse document() {
        BillingDocumentStatus status = BillingDocumentStatus.SENT;
        BigDecimal total = new BigDecimal("25000");
        return new BillingDocumentResponse(
                "INV-1", BillingDocumentType.INVOICE, status,
                "MEMBER", "MBR-1", "Joël Bikindou", "joel@example.com", null, null, true,
                "BILLABLE_ITEM", "BIL-1", null, null, null, false,
                "Facture", null, null, "XAF",
                total, null, null, null, null, null,
                total, BigDecimal.ZERO, total, null,
                null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(),
                null, null, null, null, List.of(),
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null,
                null, null, null,
                BillingReceivables.receivable(BillingDocumentType.INVOICE, status),
                BillingReceivables.customerImpact(BillingDocumentType.INVOICE, status, total, total),
                null, null, null);
    }
}
