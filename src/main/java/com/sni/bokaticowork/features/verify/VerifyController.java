package com.sni.bokaticowork.features.verify;

import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentPdfService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.payment.dto.response.PaymentReceiptResponse;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Vérification publique d'un document émis par la plateforme.
 *
 * <p>Cette page répond à une question · « ce document est-il bien sorti de chez nous ? » · et à
 * rien d'autre. Elle exposait auparavant le document complet, lignes, coordonnées du client et
 * paiements compris, sur une clé énumérable et sans jeton : une boucle sur les numéros suffisait
 * à lire la facturation de tous les clients.
 *
 * <p>Le détail n'est rendu que sur présentation de {@code k}, le préfixe de la signature fiscale
 * encodé dans le QR code du document. Il prouve la détention du document et ne se devine pas : le
 * produire exige la clé HMAC du serveur.
 */
@Controller
@RequestMapping("/verify")
@RequiredArgsConstructor
public class VerifyController {

    /** Longueur du préfixe de signature transporté par le QR · 64 bits. */
    public static final int SIGNATURE_PREFIX_LENGTH = 16;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BillingDocumentService billingDocumentService;
    private final PaymentReceiptService paymentReceiptService;
    private final BillingDocumentPdfService billingDocumentPdfService;
    private final Locale appLocale;

    @GetMapping("/doc/{documentNumber}")
    public String verifyDocument(@PathVariable String documentNumber,
                                 @RequestParam(name = "k", required = false) String signaturePrefix,
                                 Model model) {
        BillingDocumentResponse document;
        try {
            document = billingDocumentService.get(documentNumber);
        } catch (Exception ex) {
            return notFound(model, documentNumber, "document");
        }

        String label = documentLabel(document);
        String issueDate = format(document.issueDate());
        String status = statusLabel(document.status() == null ? null : document.status().name());

        model.addAttribute("view", matches(document.fiscalSignature(), signaturePrefix)
                ? VerificationView.detailed(document.documentNumber(), label, issueDate, status,
                        document.customerName(), amount(document.totalAmount()), document.currency())
                : VerificationView.minimal(document.documentNumber(), label, issueDate, status));
        model.addAttribute("generatedAt", LocalDate.now().format(DATE_FMT));
        return "verify/document";
    }

    /**
     * Compare un fichier depose a la version canonique du document.
     *
     * <p>C'est l'outil qui tranche un litige : le client presente le PDF qu'il detient, la page
     * dit s'il s'agit bien de celui qui a ete emis. Aucune donnee du document n'est revelee au
     * passage · la reponse se limite a « conforme » ou « different ».
     */
    @PostMapping("/doc/{documentNumber}/compare")
    public String compareDocument(@PathVariable String documentNumber,
                                  @RequestParam("file") MultipartFile file,
                                  Model model) {
        BillingDocumentPdfService.PdfComparison comparison;
        try {
            comparison = billingDocumentPdfService.compare(documentNumber, file.getBytes());
        } catch (IOException | RuntimeException ex) {
            return notFound(model, documentNumber, "document");
        }

        model.addAttribute("comparison", comparison);
        model.addAttribute("reference", documentNumber);
        model.addAttribute("generatedAt", LocalDate.now().format(DATE_FMT));
        return "verify/comparison";
    }

    @GetMapping("/receipt/{receiptNumber}")
    public String verifyReceipt(@PathVariable String receiptNumber,
                                @RequestParam(name = "k", required = false) String signaturePrefix,
                                Model model) {
        PaymentReceiptResponse receipt;
        try {
            receipt = paymentReceiptService.getByReceiptNumber(receiptNumber);
        } catch (Exception ex) {
            return notFound(model, receiptNumber, "reçu");
        }

        // Le reçu ne porte pas de signature fiscale · la vérification reste donc minimale, ce qui
        // est suffisant : elle confirme qu'un encaissement de ce numero a bien ete enregistre.
        model.addAttribute("view", VerificationView.minimal(
                receipt.receiptNumber(), "Reçu de paiement", format(receipt.receiptIssuedAt()), "Encaissé"));
        model.addAttribute("generatedAt", LocalDate.now().format(DATE_FMT));
        return "verify/document";
    }

    // =================================================================================

    private String notFound(Model model, String reference, String type) {
        model.addAttribute("ref", reference);
        model.addAttribute("type", type);
        return "verify/not-found";
    }

    /**
     * Le préfixe fourni correspond-il à la signature du document ?
     *
     * <p>Comparaison à temps constant : une comparaison qui s'arrête au premier caractère
     * divergent laisse mesurer la progression et permet de reconstituer le préfixe caractère par
     * caractère.
     */
    private boolean matches(String signature, String candidate) {
        if (signature == null || signature.isBlank() || candidate == null || candidate.isBlank()) {
            return false;
        }
        String expected = signature.length() <= SIGNATURE_PREFIX_LENGTH
                ? signature
                : signature.substring(0, SIGNATURE_PREFIX_LENGTH);
        String provided = candidate.trim();
        if (provided.length() != expected.length()) {
            return false;
        }
        int difference = 0;
        for (int i = 0; i < expected.length(); i++) {
            difference |= Character.toLowerCase(expected.charAt(i)) ^ Character.toLowerCase(provided.charAt(i));
        }
        return difference == 0;
    }

    private String documentLabel(BillingDocumentResponse document) {
        if (document.documentType() == null) {
            return "Document";
        }
        return switch (document.documentType()) {
            case INVOICE -> "Facture";
            case PROFORMA_INVOICE -> "Facture proforma";
            case CORRECTIVE_INVOICE -> "Facture rectificative";
            case CREDIT_NOTE -> "Avoir";
            case DEBIT_NOTE -> "Note de débit";
            case QUOTE -> "Devis";
            default -> "Document";
        };
    }

    /**
     * Libellé de statut volontairement grossier · l'état commercial exact d'un document n'a pas à
     * transiter par une page publique.
     */
    private String statusLabel(String status) {
        if (status == null) {
            return "Émis";
        }
        return switch (status) {
            case "CANCELLED", "VOIDED" -> "Annulé";
            case "DRAFT" -> "Brouillon";
            default -> "Émis";
        };
    }

    private String format(Object date) {
        if (date instanceof LocalDate localDate) {
            return localDate.format(DATE_FMT);
        }
        if (date instanceof java.time.Instant instant) {
            return instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(DATE_FMT);
        }
        return date == null ? "" : date.toString();
    }

    private String amount(BigDecimal value) {
        return value == null ? "" : String.format(appLocale, "%,.0f", value);
    }
}
