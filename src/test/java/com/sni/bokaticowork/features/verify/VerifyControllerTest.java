package com.sni.bokaticowork.features.verify;

import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentPdfService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentReceiptService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VerifyControllerTest {

    private static final String SIGNATURE = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6";
    private static final String PREFIX = "a1b2c3d4e5f6a7b8";

    private final BillingDocumentService billingDocumentService = mock(BillingDocumentService.class);
    private final PaymentReceiptService paymentReceiptService = mock(PaymentReceiptService.class);

    private final BillingDocumentPdfService pdfService = mock(BillingDocumentPdfService.class);

    private final VerifyController controller =
            new VerifyController(billingDocumentService, paymentReceiptService, pdfService, Locale.FRANCE);

    @Test
    void shouldRevealNothingBeyondExistenceWithoutTheSignaturePrefix() {
        // Regression : la page rendait le document complet, lignes, coordonnees du client et
        // paiements compris, sur une cle enumerable et sans jeton. Une boucle sur les numeros
        // suffisait a lire la facturation de tous les clients.
        documentExists();

        VerificationView view = render(null);

        assertThat(view.authentic()).isTrue();
        assertThat(view.detailed()).isFalse();
        assertThat(view.reference()).isEqualTo("INV-001");
        assertThat(view.documentLabel()).isEqualTo("Facture");
        // Rien qui ne soit deja connu de celui qui tient le document.
        assertThat(view.partyName()).isNull();
        assertThat(view.totalAmount()).isNull();
        assertThat(view.currency()).isNull();
    }

    @Test
    void shouldRevealTheDetailToWhoeverHoldsTheDocument() {
        documentExists();

        VerificationView view = render(PREFIX);

        assertThat(view.detailed()).isTrue();
        assertThat(view.partyName()).isEqualTo("Hope MADHOND");
        assertThat(view.totalAmount()).isNotBlank();
    }

    @Test
    void shouldIgnoreCaseInTheSignaturePrefix() {
        // Un QR relu par un lecteur qui normalise la casse ne doit pas faire echouer la
        // verification.
        documentExists();

        assertThat(render(PREFIX.toUpperCase(Locale.ROOT)).detailed()).isTrue();
    }

    @Test
    void shouldRefuseAWrongPrefix() {
        documentExists();

        assertThat(render("0000000000000000").detailed()).isFalse();
    }

    @Test
    void shouldRefuseATruncatedPrefix() {
        // Un prefixe plus court ne doit pas passer par correspondance partielle.
        documentExists();

        assertThat(render("a1b2c3d4").detailed()).isFalse();
    }

    @Test
    void shouldStayMinimalOnAnUnsealedDocumentEvenWithAPrefix() {
        // Sans signature, aucun prefixe ne peut correspondre · un devis reste en reponse minimale.
        BillingDocumentResponse quote = document(BillingDocumentType.QUOTE, BillingDocumentStatus.SENT, null);
        when(billingDocumentService.get(anyString())).thenReturn(quote);

        VerificationView view = render(PREFIX);

        assertThat(view.detailed()).isFalse();
        assertThat(view.documentLabel()).isEqualTo("Devis");
    }

    @Test
    void shouldReportACancelledDocumentAsSuch() {
        BillingDocumentResponse cancelled =
                document(BillingDocumentType.INVOICE, BillingDocumentStatus.CANCELLED, SIGNATURE);
        when(billingDocumentService.get(anyString())).thenReturn(cancelled);

        assertThat(render(null).statusLabel()).isEqualTo("Annulé");
    }

    @Test
    void shouldNotLeakTheCommercialStateOfADocument() {
        // PARTIALLY_PAID, OVERDUE, NEGOTIATION... renseignent sur la relation client et n'ont
        // rien a faire sur une page publique · tout ce qui n'est ni annule ni brouillon est Emis.
        BillingDocumentResponse overdue =
                document(BillingDocumentType.INVOICE, BillingDocumentStatus.OVERDUE, SIGNATURE);
        when(billingDocumentService.get(anyString())).thenReturn(overdue);

        assertThat(render(null).statusLabel()).isEqualTo("Émis");
    }

    @Test
    void shouldRenderNotFoundForAnUnknownReference() {
        when(billingDocumentService.get(anyString())).thenThrow(new RuntimeException("absent"));
        Model model = new ConcurrentModel();

        String view = controller.verifyDocument("INV-999", null, model);

        assertThat(view).isEqualTo("verify/not-found");
        assertThat(model.getAttribute("view")).isNull();
    }

    // =================================================================================

    private void documentExists() {
        // Le simulacre est construit avant d'etre pose : imbriquer un when() dans l'argument d'un
        // autre when() laisse Mockito sur un stubbing inacheve.
        BillingDocumentResponse document =
                document(BillingDocumentType.INVOICE, BillingDocumentStatus.ISSUED, SIGNATURE);
        when(billingDocumentService.get(anyString())).thenReturn(document);
    }

    private VerificationView render(String prefix) {
        Model model = new ConcurrentModel();
        controller.verifyDocument("INV-001", prefix, model);
        return (VerificationView) model.getAttribute("view");
    }

    /**
     * Le record compte une soixantaine de composants · le simuler evite d'en construire un
     * exemplaire complet, et rend explicites les seuls champs que le controleur consulte.
     */
    private BillingDocumentResponse document(BillingDocumentType type, BillingDocumentStatus status,
                                             String signature) {
        BillingDocumentResponse document = mock(BillingDocumentResponse.class);
        when(document.documentNumber()).thenReturn("INV-001");
        when(document.documentType()).thenReturn(type);
        when(document.status()).thenReturn(status);
        when(document.fiscalSignature()).thenReturn(signature);
        when(document.customerName()).thenReturn("Hope MADHOND");
        when(document.currency()).thenReturn("XAF");
        when(document.issueDate()).thenReturn(java.time.LocalDate.of(2026, 8, 30));
        when(document.totalAmount()).thenReturn(new java.math.BigDecimal("118900"));
        return document;
    }
}
