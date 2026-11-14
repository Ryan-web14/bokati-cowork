package com.sni.bokaticowork.features.billing.element.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.billing.element.dto.ElementDtos;
import com.sni.bokaticowork.features.billing.element.model.ElementRef;
import com.sni.bokaticowork.features.billing.element.service.BillingElementLinkService;
import com.sni.bokaticowork.features.billing.element.service.BillingElementResolver;
import com.sni.bokaticowork.core.audit.aop.Audited;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Elements factures (abonnement, pass, reservation, domiciliation) et leurs factures, pour rattacher
 * un paiement ou une facture (lot R1). Droits de la facturation ({@code AdminApiAuthorizationManager}).
 */
@RestController
@RequestMapping(ApiPath.V1 + "/billing")
@RequiredArgsConstructor
public class BillingElementController {

    private final BillingElementResolver resolver;
    private final BillingElementLinkService linkService;
    private final com.sni.bokaticowork.features.billing.element.service.AllocationReversalService reversals;
    private final com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository transactions;
    private final com.sni.bokaticowork.features.payment.repository.PaymentAllocationRepository allocations;

    /** Tiers (membre, client, client externe, entite, contact de reservation) et leurs elements rattachables. */
    @GetMapping("/elements/search")
    public ResponseEntity<List<ElementDtos.Party>> search(@RequestParam String query, @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(resolver.search(query, limit));
    }

    /** Elements rattachables d'un tiers connu. */
    @GetMapping("/elements")
    public ResponseEntity<List<ElementDtos.Element>> elementsOf(@RequestParam String partyType, @RequestParam String partyCode) {
        return ResponseEntity.ok(resolver.elementsOf(partyType.trim().toUpperCase(java.util.Locale.ROOT), partyCode.trim()));
    }

    @GetMapping("/elements/{type}/{code}")
    public ResponseEntity<ElementDtos.Element> element(@PathVariable String type, @PathVariable String code) {
        return ResponseEntity.ok(resolver.element(ElementRef.of(type, code)));
    }

    /** Factures de l'element ; {@code openOnly=true} pour celles qui restent a regler. */
    @GetMapping("/elements/{type}/{code}/invoices")
    public ResponseEntity<List<ElementDtos.Invoice>> invoices(@PathVariable String type, @PathVariable String code,
                                                             @RequestParam(defaultValue = "false") boolean openOnly) {
        ElementRef ref = ElementRef.of(type, code);
        resolver.element(ref);
        return ResponseEntity.ok(openOnly ? resolver.openInvoices(ref) : resolver.invoices(ref));
    }

    /**
     * Rattache une facture a un element (geste C) : sa facture propre sans paiement est annulee, et les
     * effets partent aussitot si la facture a deja recu un paiement, sinon a son paiement.
     */
    @PostMapping("/documents/{documentNumber}/elements")
    @Audited(module = "BILLING", action = "LINK_ELEMENT", ressource = "billing_document_element")
    public ResponseEntity<List<ElementDtos.Settled>> link(@PathVariable String documentNumber, @RequestBody ElementDtos.LinkRequest request) {
        return ResponseEntity.ok(linkService.link(documentNumber, request));
    }

    /** Defait le lien tant que la facture n'a recu aucun paiement. */
    @DeleteMapping("/documents/{documentNumber}/elements/{type}/{code}")
    @Audited(module = "BILLING", action = "UNLINK_ELEMENT", ressource = "billing_document_element")
    public ResponseEntity<Void> unlink(@PathVariable String documentNumber, @PathVariable String type, @PathVariable String code) {
        linkService.unlink(documentNumber, type, code);
        return ResponseEntity.noContent().build();
    }

    /** Affectations d'un paiement : en cours (avec leur identifiant pour les annuler) et annulees. */
    @GetMapping("/transactions/{transactionNumber}/allocations")
    public ResponseEntity<ElementDtos.Allocations> allocations(@PathVariable String transactionNumber) {
        var transaction = transactions.findByTransactionNumber(transactionNumber.trim())
                .orElseThrow(() -> new com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException("Paiement introuvable : " + transactionNumber));
        List<ElementDtos.Allocation> current = allocations.findAllByPaymentTransactionId(transaction.getId()).stream()
                .map(a -> new ElementDtos.Allocation(a.getId(), a.getBillingDocumentNumber(), a.getAllocatedAmount(), a.getAllocationMode(),
                        a.getAllocatedAt(), a.getAllocatedBy())).toList();
        java.math.BigDecimal used = current.stream().map(ElementDtos.Allocation::amount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        List<ElementDtos.Reversal> reversed = reversals.ofTransaction(transaction.getTransactionNumber()).stream()
                .map(r -> new ElementDtos.Reversal(r.getId(), r.getAllocationId(), r.getDocumentNumber(), r.getAmount(), r.getInvoiceOutcome(),
                        r.getReason(), r.getReversedAt(), r.getReversedBy(), r.getElementsJson())).toList();
        return ResponseEntity.ok(new ElementDtos.Allocations(transaction.getTransactionNumber(), transaction.getAmount(),
                transaction.getAmount().subtract(used), current, reversed));
    }

    /**
     * Annule une affectation (lot R4) : la somme revient en attente, la facture est rouverte
     * ({@code REOPEN}) ou neutralisee ({@code NEUTRALIZE}), l'element qui n'est plus regle est remis en
     * attente ; refus si l'element a deja ete consomme.
     */
    @PostMapping("/allocations/{allocationId}/cancel")
    @Audited(module = "BILLING", action = "CANCEL_ALLOCATION", ressource = "payment_allocation")
    public ResponseEntity<com.sni.bokaticowork.features.billing.element.service.AllocationReversalService.Result> cancelAllocation(
            @PathVariable Long allocationId,
            @RequestBody com.sni.bokaticowork.features.billing.element.service.AllocationReversalService.CancelRequest request) {
        return ResponseEntity.ok(reversals.cancel(allocationId, request));
    }

    /** Elements regles par une facture. */
    @GetMapping("/documents/{documentNumber}/elements")
    public ResponseEntity<List<ElementDtos.Settled>> elementsOfInvoice(@PathVariable String documentNumber) {
        return ResponseEntity.ok(resolver.elements(documentNumber));
    }
}
