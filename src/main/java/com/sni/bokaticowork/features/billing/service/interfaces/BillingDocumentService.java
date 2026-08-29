package com.sni.bokaticowork.features.billing.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateCreditNoteRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateInvoiceFromBillableItemsRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateManualBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateReservationInvoiceRequest;
import com.sni.bokaticowork.features.billing.dto.request.SelectQuoteOptionsRequest;
import com.sni.bokaticowork.features.billing.dto.request.UpdateBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.request.UpdateBillingRecipientRequest;
import com.sni.bokaticowork.features.billing.dto.response.CustomerStatementResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.dto.response.SimulateBillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface BillingDocumentService {
    BillingDocumentResponse create(CreateBillingDocumentRequest request);

    /**
     * Calcule les totaux d'un document sans le creer : rien n'est ecrit, aucun numero de
     * sequence n'est consomme. Accepte le meme corps que {@link #create}, de sorte que
     * l'interface puisse simuler puis envoyer la meme charge utile.
     */
    SimulateBillingDocumentResponse simulate(CreateBillingDocumentRequest request);
    BillingDocumentResponse update(String documentNumber, UpdateBillingDocumentRequest request);

    /**
     * Corrige les coordonnees du destinataire imprimees sur le document sans changer le client
     * auquel il est rattache. Le proprietaire ({@code customerType} / {@code customerCode})
     * n'est jamais modifiable par ce chemin : le DTO ne le porte pas.
     * <p>
     * Sur un document <b>non scelle</b>, tous les champs du destinataire sont corrigeables.
     * <p>
     * Sur un document <b>scelle</b> ({@code locked = true}), seules les coordonnees de contact
     * le restent - email, telephone, categorie, reference, adresse de livraison. L'identite
     * legale ({@code customerName}, {@code customerNiu}, {@code billingAddressJson}) est figee
     * par le declencheur {@code trg_billing_document_immutable} et toute tentative leve une
     * {@code BadRequestException} orientant vers l'avoir ou la rectificative.
     * <p>
     * A noter : ce declencheur est plus strict que la chaine de hachage fiscale, qui ne couvre
     * que {@code fiscalNumber|fiscalDate|customerCode|totalAmount|previousHash}
     * (voir {@code FiscalHashService}). Se fier au seul hachage pour decider de ce qui est
     * modifiable est donc faux.
     * <p>
     * Chaque correction est tracee dans l'historique d'edition ({@code RECIPIENT_UPDATED}).
     */
    BillingDocumentResponse updateRecipient(String documentNumber, UpdateBillingRecipientRequest request);
    BillingDocumentResponse createManualInvoice(CreateManualBillingDocumentRequest request);
    BillingDocumentResponse createManualQuote(CreateManualBillingDocumentRequest request);
    BillingDocumentResponse createManualQuotation(CreateManualBillingDocumentRequest request);
    BillingDocumentResponse createInvoiceFromBillableItems(CreateInvoiceFromBillableItemsRequest request);
    BillingDocumentResponse createInvoiceFromReservation(CreateReservationInvoiceRequest request);
    BillingDocumentResponse duplicate(String documentNumber);
    BillingDocumentResponse issue(String documentNumber);
    BillingDocumentResponse send(String documentNumber);
    BillingDocumentResponse selectQuoteOptions(String quoteNumber, SelectQuoteOptionsRequest request);
    BillingDocumentResponse markViewed(String quoteNumber);
    BillingDocumentResponse startNegotiation(String quoteNumber);
    BillingDocumentResponse requestDeposit(String quoteNumber);
    BillingDocumentResponse markDepositPaid(String quoteNumber);
    BillingDocumentResponse acceptQuote(String quoteNumber);
    BillingDocumentResponse rejectQuote(String quoteNumber);
    BillingDocumentResponse convertQuoteToInvoice(String quoteNumber);
    BillingDocumentResponse acceptQuotation(String quotationNumber);
    BillingDocumentResponse rejectQuotation(String quotationNumber);
    BillingDocumentResponse convertQuotationToInvoice(String quotationNumber);
    BillingDocumentResponse createCreditNote(String invoiceNumber, CreateCreditNoteRequest request);
    BillingDocumentResponse applyCreditNote(String creditNoteNumber);
    CustomerStatementResponse customerStatement(String customerType, String customerCode, Pageable pageable);
    int markOverdueDocuments();
    BillingDocumentResponse get(String documentNumber);
    PaginatedResponse<BillingDocumentResponse> list(BillingDocumentType type,
                                                    BillingDocumentStatus status,
                                                    String customerType,
                                                    String customerCode,
                                                    String sourceType,
                                                    String sourceCode,
                                                    LocalDate fromDate,
                                                    LocalDate toDate,
                                                    String searchText,
                                                    Pageable pageable);
    BillingDocument serviceByNumber(String documentNumber);
    BillingDocument applyPayment(String documentNumber, BigDecimal amount);
    BillingDocument reversePayment(String documentNumber, BigDecimal amount);
    /** Variante interne : renvoie l'entite, utilisee par le workflow de paiement. */
    BillingDocument cancelAndArchive(String documentNumber, String reason);

    /** Variante exposee par l'API : meme comportement, renvoie la representation du document. */
    BillingDocumentResponse cancelAndArchiveDocument(String documentNumber, String reason);

    /** Valide fiscalement le document (SEFC) : assigne le numéro définitif et verrouille. */
    BillingDocumentResponse validate(String documentNumber);

    /**
     * Applique un avoir validé (SEFC) sur n'importe quelle facture du même client.
     * L'avoir doit être VALIDATED (locked). L'avoir est marqué ISSUED après application.
     */
    BillingDocumentResponse applyCreditNoteToInvoice(String creditNoteNumber, String targetInvoiceNumber);

    /**
     * Crée une facture rectificative (REC) référençant une facture validée.
     * La rectificative est créée en DRAFT et doit être validée séparément.
     */
    BillingDocumentResponse createCorrectiveInvoice(String originalInvoiceNumber, CreateManualBillingDocumentRequest request);
}
