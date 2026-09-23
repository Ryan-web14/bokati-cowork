package com.sni.bokaticowork.features.payment.cash.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.billing.service.support.BillingReceivables;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.payment.cash.dto.request.ConfirmCashPaymentRequest;
import com.sni.bokaticowork.features.payment.cash.dto.request.DeclareCashPaymentRequest;
import com.sni.bokaticowork.features.payment.cash.dto.response.CashPaymentDeclarationResponse;
import com.sni.bokaticowork.features.payment.cash.enums.CashDeclarationStatus;
import com.sni.bokaticowork.features.payment.cash.model.CashPaymentDeclaration;
import com.sni.bokaticowork.features.payment.cash.repository.CashPaymentDeclarationRepository;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentFromBillingDocumentRequest;
import com.sni.bokaticowork.features.payment.dto.request.RegisterCashPaymentRequest;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentTransactionResponse;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * Annoncer un paiement en especes, puis le confirmer quand l argent est la.
 *
 * <p>Le mobile money et le portefeuille aboutissent seuls. Les especes arrivent avec la personne ·
 * entre l annonce faite depuis l espace client et les billets comptes a la caisse, il n y a rien
 * d encaisse. Cette attente n etait representee nulle part : le client ne pouvait pas annoncer,
 * la caisse ne voyait rien venir, et personne ne savait qu une reservation etait tenue pour
 * quelqu un qui allait passer payer.</p>
 *
 * <p>Deux regles tiennent tout le reste :</p>
 * <ul>
 *   <li><b>Annoncer n eteint rien.</b> La facture reste due, la reservation reste en attente de
 *       paiement. Une annonce jamais honoree effacerait sinon une creance reelle.</li>
 *   <li><b>Seule la caisse confirme</b>, et seulement depuis une session de caisse ouverte · un
 *       encaissement qui n atterrit dans aucune caisse fausse le comptage du soir.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CashPaymentDeclarationService {

    private static final String SEQUENCE = "cash_payment_declaration";
    private static final String BILLABLE_SOURCE_TYPE = "BILLABLE_ITEM";

    private final CashPaymentDeclarationRepository repository;
    private final BillingDocumentService billingDocumentService;
    private final com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository billingDocumentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentService paymentService;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final CashDeclarationNotifier notifier;

    /** Combien de temps une annonce tient · au-dela, la reservation est rendue. */
    @Value("${bokati.payment.cash-declaration.validity-hours:4}")
    private int validityHours;

    // ---------------------------------------------------------------------------------------
    // Annonce
    // ---------------------------------------------------------------------------------------

    /**
     * Le client annonce qu il reglera en especes.
     *
     * <p>Une seule annonce en cours par facture · deux annonces sur la meme facture finissent par
     * deux encaissements, et le client paie deux fois. La seconde demande rend la premiere.</p>
     */
    @Transactional
    public CashPaymentDeclarationResponse declare(String documentNumber, DeclareCashPaymentRequest request,
                                                  String declaredBy) {
        BillingDocumentResponse document = billingDocumentService.get(documentNumber);
        assertPayable(document);

        CashPaymentDeclaration existing = repository
                .findFirstByDocumentNumberAndStatusOrderByDeclaredAtDesc(
                        document.documentNumber(), CashDeclarationStatus.AWAITING_CONFIRMATION)
                .orElse(null);
        if (existing != null) {
            log.info("Annonce d especes {} deja en cours sur {} · demande rendue telle quelle",
                    existing.getDeclarationNumber(), document.documentNumber());
            return CashPaymentDeclarationResponse.of(existing);
        }

        BigDecimal amount = amountToDeclare(request == null ? null : request.amount(), document);
        String bookingNumber = bookingOf(document);

        CashPaymentDeclaration declaration = repository.save(CashPaymentDeclaration.builder()
                .declarationNumber(sequenceGenerator.next(SEQUENCE))
                .documentNumber(document.documentNumber())
                .bookingNumber(bookingNumber)
                .customerType(document.customerType())
                .customerCode(document.customerCode())
                .customerName(document.customerName())
                .amount(amount)
                .currency(document.currency())
                .status(CashDeclarationStatus.AWAITING_CONFIRMATION)
                .note(request == null ? null : StringUtils.trimWhitespace(request.note()))
                .declaredAt(Instant.now())
                .declaredBy(declaredBy)
                .expiresAt(Instant.now().plus(Duration.ofHours(Math.max(1, validityHours))))
                .build());

        log.info("Especes annoncees · {} pour {} {} sur la facture {}{}",
                declaration.getDeclarationNumber(), amount.toPlainString(), document.currency(),
                document.documentNumber(),
                bookingNumber == null ? "" : " (reservation " + bookingNumber + ")");
        notifier.declared(declaration);
        return CashPaymentDeclarationResponse.of(declaration);
    }

    /**
     * Le montant annonce · le solde de la facture par defaut.
     *
     * <p>Un montant superieur au solde est refuse ici plutot qu a la caisse, ou le client serait
     * deja devant le guichet avec ses billets.</p>
     */
    private BigDecimal amountToDeclare(BigDecimal requested, BillingDocumentResponse document) {
        BigDecimal balance = document.balanceDue() == null ? BigDecimal.ZERO : document.balanceDue();
        if (requested == null) {
            return balance;
        }
        if (requested.signum() <= 0) {
            throw new BadRequestException("Le montant annoncé doit être positif.");
        }
        if (requested.compareTo(balance) > 0) {
            throw new BadRequestException("Le montant annoncé dépasse le solde de la facture ("
                    + balance.toPlainString() + " " + document.currency() + ").");
        }
        return requested;
    }

    private void assertPayable(BillingDocumentResponse document) {
        if (!BillingReceivables.receivable(document.documentType(), document.status())) {
            throw new BadRequestException("La facture " + document.documentNumber()
                    + " n'est pas réglable (" + document.status() + ").");
        }
        if (document.balanceDue() == null || document.balanceDue().signum() <= 0) {
            throw new BadRequestException("La facture " + document.documentNumber() + " est déjà soldée.");
        }
        if (!"XAF".equalsIgnoreCase(document.currency())) {
            // La caisse ne tient que du XAF · annoncer autre chose ne pourrait pas etre encaisse.
            throw new BadRequestException("Les paiements en espèces ne sont acceptés qu'en XAF.");
        }
    }

    /** La reservation derriere cette facture, s il y en a une · c est elle qu on rendra a l expiration. */
    private String bookingOf(BillingDocumentResponse document) {
        if (!BILLABLE_SOURCE_TYPE.equalsIgnoreCase(document.sourceType())
                || !StringUtils.hasText(document.sourceCode())) {
            return null;
        }
        return bookingRepository.findByBillableNumber(document.sourceCode().trim())
                .map(booking -> booking.getBookingNumber())
                .orElse(null);
    }

    /** La facture d une reservation · ce que le client regle quand il reserve en paiement direct. */
    @Transactional(readOnly = true)
    public String documentOfBooking(String bookingNumber) {
        var booking = bookingRepository.findByBookingNumber(bookingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable : " + bookingNumber));
        if (!StringUtils.hasText(booking.getBillableNumber())) {
            throw new BadRequestException("La réservation " + bookingNumber
                    + " n'a rien à régler · elle est couverte par un abonnement ou un pass.");
        }
        return billingDocumentRepository.findFirstBySourceAndType(
                        BILLABLE_SOURCE_TYPE, booking.getBillableNumber(), BillingDocumentType.INVOICE.name())
                .map(com.sni.bokaticowork.features.billing.model.BillingDocument::getDocumentNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucune facture rattachée à la réservation " + bookingNumber));
    }

    // ---------------------------------------------------------------------------------------
    // Confirmation
    // ---------------------------------------------------------------------------------------

    /**
     * La caisse a compte l argent · c est seulement maintenant que le paiement existe.
     *
     * <p>L encaissement passe par le circuit habituel : intention de paiement, transaction en
     * especes adossee a une session de caisse, imputation sur la facture. La reservation, s il y
     * en a une, se confirme d elle-meme quand l intention est soldee.</p>
     */
    @Transactional
    public CashPaymentDeclarationResponse confirm(String declarationNumber, ConfirmCashPaymentRequest request,
                                                  String actor) {
        CashPaymentDeclaration declaration = required(declarationNumber);
        if (!declaration.open()) {
            throw new BadRequestException("L'annonce " + declarationNumber + " est déjà " + label(declaration.getStatus())
                    + " · elle ne peut plus être confirmée.");
        }

        BillingDocumentResponse document = billingDocumentService.get(declaration.getDocumentNumber());
        BigDecimal balance = document.balanceDue() == null ? BigDecimal.ZERO : document.balanceDue();
        if (balance.signum() <= 0) {
            // Regle entre-temps par un autre moyen · encaisser en plus ferait payer deux fois.
            close(declaration, CashDeclarationStatus.CANCELLED, actor,
                    "Facture déjà soldée par un autre moyen");
            throw new BadRequestException("La facture " + declaration.getDocumentNumber()
                    + " a été réglée entre-temps · rien à encaisser.");
        }

        BigDecimal received = request.amount() == null ? declaration.getAmount() : request.amount();
        if (received == null || received.signum() <= 0) {
            throw new BadRequestException("Le montant encaissé doit être positif.");
        }
        if (received.compareTo(balance) > 0) {
            throw new BadRequestException("Le montant encaissé dépasse le solde de la facture ("
                    + balance.toPlainString() + " " + document.currency() + ").");
        }

        String receivedBy = StringUtils.hasText(request.receivedBy()) ? request.receivedBy() : actor;
        PaymentIntentResponse intent = paymentService.createIntentFromBillingDocument(
                new CreatePaymentIntentFromBillingDocumentRequest(
                        declaration.getDocumentNumber(), received,
                        "CASH_DECLARATION:" + declaration.getDeclarationNumber(), null, null));
        PaymentTransactionResponse transaction = paymentService.registerCashPayment(
                intent.intentNumber(),
                new RegisterCashPaymentRequest(receivedBy, request.cashSessionNumber(), received, null, null));

        declaration.setStatus(CashDeclarationStatus.CONFIRMED);
        declaration.setConfirmedAt(Instant.now());
        declaration.setConfirmedBy(receivedBy);
        declaration.setConfirmedAmount(received);
        declaration.setCashSessionNumber(request.cashSessionNumber());
        declaration.setTransactionNumber(transaction.transactionNumber());
        declaration.setCloseReason(StringUtils.hasText(request.note()) ? request.note() : null);
        CashPaymentDeclaration saved = repository.save(declaration);

        log.info("Annonce {} confirmee · {} {} encaisses en caisse {} par {}",
                declarationNumber, received.toPlainString(), declaration.getCurrency(),
                request.cashSessionNumber(), receivedBy);
        notifier.confirmed(saved);
        return CashPaymentDeclarationResponse.of(saved);
    }

    // ---------------------------------------------------------------------------------------
    // Abandon
    // ---------------------------------------------------------------------------------------

    /** Le client se ravise, ou la caisse fait le menage · la facture reste due, simplement plus annoncee. */
    @Transactional
    public CashPaymentDeclarationResponse cancel(String declarationNumber, String reason, String actor) {
        CashPaymentDeclaration declaration = required(declarationNumber);
        if (!declaration.open()) {
            throw new BadRequestException("L'annonce " + declarationNumber + " est déjà " + label(declaration.getStatus()) + ".");
        }
        CashPaymentDeclaration saved = close(declaration, CashDeclarationStatus.CANCELLED, actor, reason);
        log.info("Annonce {} annulee par {} · {}", declarationNumber, actor, reason);
        notifier.cancelled(saved);
        return CashPaymentDeclarationResponse.of(saved);
    }

    /** Le delai est passe · l annonce tombe, et la reservation est rendue par le worker. */
    @Transactional
    public CashPaymentDeclaration expire(CashPaymentDeclaration declaration, String reason) {
        return close(declaration, CashDeclarationStatus.EXPIRED, "SYSTEM", reason);
    }

    private CashPaymentDeclaration close(CashPaymentDeclaration declaration, CashDeclarationStatus status,
                                         String actor, String reason) {
        declaration.setStatus(status);
        declaration.setClosedAt(Instant.now());
        declaration.setClosedBy(actor);
        declaration.setCloseReason(StringUtils.hasText(reason) ? reason.trim() : null);
        return repository.save(declaration);
    }

    // ---------------------------------------------------------------------------------------
    // Consultation
    // ---------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public CashPaymentDeclarationResponse get(String declarationNumber) {
        return CashPaymentDeclarationResponse.of(required(declarationNumber));
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<CashPaymentDeclarationResponse> list(String status, Pageable pageable) {
        return new PaginatedResponse<>((StringUtils.hasText(status)
                ? repository.findByStatusOrderByDeclaredAtAsc(parse(status), pageable)
                : repository.findAllByOrderByDeclaredAtDesc(pageable))
                .map(CashPaymentDeclarationResponse::of));
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<CashPaymentDeclarationResponse> listForCustomer(String customerType, String customerCode,
                                                                             Pageable pageable) {
        return new PaginatedResponse<>(repository
                .findByCustomerTypeAndCustomerCodeOrderByDeclaredAtDesc(customerType, customerCode, pageable)
                .map(CashPaymentDeclarationResponse::of));
    }

    /** L annonce, si elle appartient bien a ce client · sinon elle n existe pas pour lui. */
    @Transactional(readOnly = true)
    public CashPaymentDeclaration ownedBy(String declarationNumber, String customerType, String customerCode) {
        CashPaymentDeclaration declaration = required(declarationNumber);
        boolean owned = declaration.getCustomerType() != null
                && declaration.getCustomerType().equalsIgnoreCase(customerType)
                && declaration.getCustomerCode() != null
                && declaration.getCustomerCode().equalsIgnoreCase(customerCode);
        if (!owned) {
            // On ne distingue pas « inconnue » de « pas la votre » · la difference renseignerait
            // sur les paiements des autres.
            throw new ResourceNotFoundException("Annonce de paiement introuvable : " + declarationNumber);
        }
        return declaration;
    }

    private CashPaymentDeclaration required(String declarationNumber) {
        return repository.findByDeclarationNumber(declarationNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Annonce de paiement introuvable : " + declarationNumber));
    }

    private CashDeclarationStatus parse(String status) {
        try {
            return CashDeclarationStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("État inconnu : " + status
                    + ". Valeurs possibles : AWAITING_CONFIRMATION, CONFIRMED, CANCELLED, EXPIRED.");
        }
    }

    private String label(CashDeclarationStatus status) {
        return switch (status) {
            case AWAITING_CONFIRMATION -> "en attente";
            case CONFIRMED -> "confirmée";
            case CANCELLED -> "annulée";
            case EXPIRED -> "expirée";
        };
    }
}
