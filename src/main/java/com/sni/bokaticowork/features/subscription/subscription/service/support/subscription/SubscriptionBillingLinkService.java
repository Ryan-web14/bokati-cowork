package com.sni.bokaticowork.features.subscription.subscription.service.support.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.model.PaymentAllocation;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.repository.PaymentAllocationRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionEventWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * Rattache a un abonnement une facture ou un paiement deja faits hors de l'abonnement (facturation
 * passee avant sa creation) : l'abonnement ne refacture pas sa premiere periode, et il est valide
 * (active) des que la facture rattachee est soldee.
 *
 * <p>Controles : la facture est emise (ni brouillon, ni annulee), c'est une facture (pas un devis
 * ni un avoir), elle est au nom du souscripteur, et elle n'est pas deja rattachee a un autre
 * abonnement ; un paiement est reussi, au nom du souscripteur, et impute sur la facture choisie.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionBillingLinkService {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<BillingDocumentType> INVOICES = EnumSet.of(BillingDocumentType.INVOICE, BillingDocumentType.CORRECTIVE_INVOICE,
            BillingDocumentType.DEBIT_NOTE);
    private static final Set<BillingDocumentStatus> CLOSED = EnumSet.of(BillingDocumentStatus.DRAFT, BillingDocumentStatus.CANCELLED,
            BillingDocumentStatus.VOIDED);

    private final BillingDocumentRepository documentRepository;
    private final BillingDocumentService documentService;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentAllocationRepository allocationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BillableItemRepository billableItemRepository;
    private final SubscriptionEventWriter eventWriter;
    private final SubscriptionLifecycleOperator lifecycleOperator;

    /** Facture et paiement rattaches, avec ce qu'il faut savoir avant de valider. */
    public record Link(String invoiceNumber, String paymentNumber, BigDecimal invoiceTotal, BigDecimal balanceDue, String currency, boolean paid,
                       List<String> warnings) {
    }

    /** Resultat d'un rattachement sur un abonnement existant. */
    public record LinkResult(String subscriptionNumber, SubscriptionStatus status, Link link, List<String> cancelledInvoices, List<String> warnings) {
    }

    /**
     * Verifie la facture ou le paiement indiques. Sans l'un ni l'autre : {@code null}.
     *
     * @param subscriberCodes les codes sous lesquels le souscripteur peut figurer sur la facture
     * @param expectedTotal   montant de la premiere periode, pour avertir d'une facture plus faible
     */
    @Transactional(readOnly = true)
    public Link resolve(Collection<String> subscriberCodes, String invoiceNumber, String paymentNumber, String currentSubscriptionNumber,
                        BigDecimal expectedTotal) {
        if (!StringUtils.hasText(invoiceNumber) && !StringUtils.hasText(paymentNumber)) {
            return null;
        }
        Set<String> codes = new HashSet<>();
        subscriberCodes.stream().filter(StringUtils::hasText).forEach(code -> codes.add(code.trim()));
        List<String> warnings = new ArrayList<>();
        String invoice = StringUtils.hasText(invoiceNumber) ? invoiceNumber.trim() : null;
        String payment = StringUtils.hasText(paymentNumber) ? paymentNumber.trim() : null;
        List<String> allocated = List.of();
        if (payment != null) {
            PaymentTransaction transaction = transactionRepository.findByTransactionNumber(payment)
                    .orElseThrow(() -> new ResourceNotFoundException("Paiement introuvable : " + payment));
            if (transaction.getStatus() != PaymentTransactionStatus.SUCCEEDED) {
                throw new BadRequestException("Le paiement " + payment + " n'est pas réussi (" + transaction.getStatus() + ")");
            }
            String payer = transaction.getPaymentIntent() == null ? null : transaction.getPaymentIntent().getCustomerCode();
            if (payer != null && !codes.contains(payer)) {
                throw new BadRequestException("Le paiement " + payment + " a été fait par un autre client (" + payer + ")");
            }
            allocated = allocationRepository.findAllByPaymentTransactionId(transaction.getId()).stream()
                    .map(PaymentAllocation::getBillingDocumentNumber).filter(Objects::nonNull).distinct().toList();
            if (invoice == null) {
                if (allocated.size() != 1) {
                    throw new BadRequestException("Le paiement " + payment + " est imputé sur " + allocated.size()
                            + " facture(s) : indiquez celle qui couvre l'abonnement");
                }
                invoice = allocated.get(0);
            } else if (!allocated.contains(invoice)) {
                throw new BadRequestException("Le paiement " + payment + " n'est pas imputé sur la facture " + invoice);
            }
        }
        String number = invoice;
        BillingDocument document = documentRepository.findByDocumentNumber(number)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable : " + number));
        if (!INVOICES.contains(document.getDocumentType())) {
            throw new BadRequestException("Le document " + number + " n'est pas une facture (" + document.getDocumentType() + ")");
        }
        if (CLOSED.contains(document.getStatus())) {
            throw new BadRequestException("La facture " + number + " n'est pas émise ou a été annulée (" + document.getStatus() + ")");
        }
        if (!codes.contains(nz(document.getCustomerCode())) && !codes.contains(nz(document.getCompanyCustomerCode()))) {
            throw new BadRequestException("La facture " + number + " est au nom d'un autre client ("
                    + (document.getCompanyCustomerCode() != null ? document.getCompanyCustomerCode() : document.getCustomerCode()) + ")");
        }
        List<String> already = subscriptionRepository.findNumbersLinkedToInvoice(number).stream()
                .filter(other -> !other.equals(currentSubscriptionNumber)).toList();
        if (!already.isEmpty()) {
            throw new ConflictException("subscription", "la facture " + number + " est déjà rattachée à l'abonnement " + already.get(0));
        }
        BigDecimal balance = document.getBalanceDue() == null ? BigDecimal.ZERO : document.getBalanceDue();
        boolean paid = balance.signum() <= 0;
        if (!paid) {
            warnings.add("La facture " + number + " n'est pas soldée (reste dû " + plain(balance) + " " + document.getCurrency()
                    + ") : l'abonnement sera validé à son règlement complet");
        }
        if (expectedTotal != null && document.getTotalAmount() != null && document.getTotalAmount().compareTo(expectedTotal) < 0) {
            warnings.add("La facture " + number + " (" + plain(document.getTotalAmount()) + ") est inférieure au montant de la première période ("
                    + plain(expectedTotal) + ")");
        }
        return new Link(number, payment, document.getTotalAmount(), balance, document.getCurrency(), paid, warnings);
    }

    /** Inscrit le rattachement dans l'abonnement (metadonnees et historique). */
    @Transactional
    public void attach(Subscription subscription, Link link, String actor) {
        ObjectNode metadata;
        try {
            metadata = StringUtils.hasText(subscription.getMetadataJson()) && JSON.readTree(subscription.getMetadataJson()).isObject()
                    ? (ObjectNode) JSON.readTree(subscription.getMetadataJson()) : JSON.createObjectNode();
        } catch (Exception exception) {
            metadata = JSON.createObjectNode();
        }
        metadata.put("linkedInvoiceNumber", link.invoiceNumber());
        if (link.paymentNumber() != null) {
            metadata.put("linkedPaymentNumber", link.paymentNumber());
        }
        metadata.put("linkedBy", actor);
        subscription.setMetadataJson(metadata.toString());
        subscriptionRepository.save(subscription);
        eventWriter.writeHistory(subscription, subscription.getStatus(), subscription.getStatus(),
                "Facture existante " + link.invoiceNumber() + (link.paymentNumber() == null ? "" : " (paiement " + link.paymentNumber() + ")")
                        + " rattachée : la première période n'est pas refacturée", actor);
    }

    /**
     * Rattache une facture ou un paiement a un abonnement deja cree, annule sa propre facture
     * initiale encore impayee, puis le valide si la facture rattachee est soldee. Un administrateur
     * peut forcer la validation d'une facture non soldee ({@code force}), avec un avertissement.
     */
    @Transactional
    public LinkResult linkAndValidate(Subscription subscription, Collection<String> subscriberCodes, String invoiceNumber, String paymentNumber,
                                      boolean activate, boolean force, boolean admin, String actor) {
        if (subscription.getStatus() != SubscriptionStatus.PENDING_ACTIVATION && subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new BadRequestException("Seul un abonnement en attente d'activation ou actif reçoit une facture existante ("
                    + subscription.getStatus() + ")");
        }
        Link link = resolve(subscriberCodes, invoiceNumber, paymentNumber, subscription.getSubscriptionNumber(), subscription.getTotalAmount());
        if (link == null) {
            throw new BadRequestException("Indiquez la facture ou le paiement à rattacher");
        }
        List<String> warnings = new ArrayList<>(link.warnings());
        List<String> cancelled = new ArrayList<>();
        // La facture initiale que l'abonnement a lui-meme emise ferait double emploi.
        Set<Long> ownInvoices = new LinkedHashSet<>();
        for (BillableItem item : billableItemRepository.findAllBySourceId(subscription.getSubscriptionNumber())) {
            if (item.getInvoiceId() != null) {
                ownInvoices.add(item.getInvoiceId());
            }
        }
        for (Long id : ownInvoices) {
            BillingDocument own = documentRepository.findById(id).orElse(null);
            if (own == null || own.getDocumentNumber() == null || own.getDocumentNumber().equals(link.invoiceNumber()) || CLOSED.contains(own.getStatus())) {
                continue;
            }
            boolean untouched = (own.getPaidAmount() == null || own.getPaidAmount().signum() == 0) && !Boolean.TRUE.equals(own.getLocked());
            if (untouched) {
                documentService.cancelAndArchive(own.getDocumentNumber(), "Remplacée par la facture existante " + link.invoiceNumber());
                cancelled.add(own.getDocumentNumber());
            } else {
                warnings.add("La facture initiale " + own.getDocumentNumber() + " est déjà réglée ou scellée : elle n'a pas été annulée, à traiter par un avoir");
            }
        }
        attach(subscription, link, actor);
        if (activate && subscription.getStatus() == SubscriptionStatus.PENDING_ACTIVATION) {
            if (link.paid()) {
                lifecycleOperator.activate(subscription, "Validation sur la facture existante " + link.invoiceNumber() + " soldée", actor);
            } else if (force && admin) {
                lifecycleOperator.activate(subscription, "Validation forcée par un administrateur, facture " + link.invoiceNumber() + " non soldée", actor);
                warnings.add("Abonnement validé alors que la facture " + link.invoiceNumber() + " n'est pas soldée");
            } else {
                warnings.add("Abonnement laissé en attente : il sera validé quand la facture " + link.invoiceNumber() + " sera soldée"
                        + (admin ? " (ou forcez la validation)" : ""));
            }
        }
        log.info("Abonnement {} : facture existante {} rattachee par {}", subscription.getSubscriptionNumber(), link.invoiceNumber(), actor);
        return new LinkResult(subscription.getSubscriptionNumber(), subscription.getStatus(), link, cancelled, warnings);
    }

    /** Variante par numero d'abonnement, pour l'API. */
    @Transactional
    public LinkResult linkAndValidate(String subscriptionNumber, String invoiceNumber, String paymentNumber, boolean activate, boolean force,
                                      boolean admin, String actor) {
        Subscription subscription = subscriptionRepository.findBySubscriptionNumber(subscriptionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement introuvable : " + subscriptionNumber));
        List<String> codes = new ArrayList<>();
        codes.add(subscription.getSubscriberCode());
        if (subscription.getCustomer() != null && subscription.getCustomer().getCustomerId() != null) {
            codes.add(subscription.getCustomer().getCustomerId());
        }
        return linkAndValidate(subscription, codes, invoiceNumber, paymentNumber, activate, force, admin, actor);
    }

    /** Abonnement en attente rattache a une facture desormais soldee : il est valide (appele apres un paiement). */
    @Transactional
    public boolean activateIfLinkedInvoicePaid(String invoiceNumber, String actor) {
        boolean activated = false;
        for (String number : subscriptionRepository.findNumbersLinkedToInvoice(invoiceNumber)) {
            Subscription subscription = subscriptionRepository.findBySubscriptionNumber(number).orElse(null);
            BillingDocument document = documentRepository.findByDocumentNumber(invoiceNumber).orElse(null);
            if (subscription == null || document == null || subscription.getStatus() != SubscriptionStatus.PENDING_ACTIVATION
                    || (document.getBalanceDue() != null && document.getBalanceDue().signum() > 0)) {
                continue;
            }
            lifecycleOperator.activate(subscription, "Validation : facture rattachée " + invoiceNumber + " soldée", actor);
            activated = true;
        }
        return activated;
    }

    private static String nz(String value) {
        return value == null ? "" : value.trim();
    }

    private static String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
