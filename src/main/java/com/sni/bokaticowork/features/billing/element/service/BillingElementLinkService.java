package com.sni.bokaticowork.features.billing.element.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.billing.element.dto.ElementDtos;
import com.sni.bokaticowork.features.billing.element.model.BillingDocumentElement;
import com.sni.bokaticowork.features.billing.element.model.ElementRef;
import com.sni.bokaticowork.features.billing.element.repository.BillingDocumentElementRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rattachement d'une facture a un element (lot R3, geste C), et regles communes aux rattachements :
 * element ouvert, meme tiers, mode d'activation.
 *
 * <p>La facture propre de l'element, si elle n'a recu aucun paiement, est annulee (par avoir si elle
 * est certifiee) : la facture rattachee la remplace. Si la facture rattachee a deja recu un paiement,
 * les effets partent aussitot ; sinon ils partiront a son paiement, quel que soit le moyen.</p>
 */
@Slf4j
@Service
@Transactional
public class BillingElementLinkService {

    private static final List<String> INVOICE_TYPES = List.of("INVOICE", "CORRECTIVE_INVOICE", "DEBIT_NOTE");

    private final BillingElementResolver resolver;
    private final BillingDocumentElementRepository links;
    private final JdbcTemplate jdbc;
    private final ObjectProvider<BillingDocumentService> documents;
    private final ElementSettlementPublisher settlements;
    private final StaffProfileService staffProfiles;

    public BillingElementLinkService(BillingElementResolver resolver, BillingDocumentElementRepository links, JdbcTemplate jdbc,
                                     ObjectProvider<BillingDocumentService> documents, ElementSettlementPublisher settlements,
                                     StaffProfileService staffProfiles) {
        this.resolver = resolver;
        this.links = links;
        this.jdbc = jdbc;
        this.documents = documents;
        this.settlements = settlements;
        this.staffProfiles = staffProfiles;
    }

    /** Rattache la facture a l'element ; rend les elements desormais regles par la facture. */
    public List<ElementDtos.Settled> link(String documentNumber, ElementDtos.LinkRequest request) {
        if (request == null) {
            throw BadRequestException.coded("ELEMENT_CODE_REQUIS", "Indiquez l'élément : type et code");
        }
        ElementRef ref = ElementRef.of(request.type(), request.code());
        String mode = ElementRef.mode(request.activationMode());
        Map<String, Object> invoice = resolver.document(documentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable : " + documentNumber));
        if (!INVOICE_TYPES.contains((String) invoice.get("document_type"))) {
            throw BadRequestException.coded("RATTACHEMENT_INVALIDE", "Seule une facture, une facture rectificative ou une note de débit règle un élément");
        }
        if ("CANCELLED".equals(invoice.get("status"))) {
            throw BadRequestException.coded("RATTACHEMENT_INVALIDE", "La facture " + documentNumber + " est annulée");
        }
        ElementDtos.Element element = assertOpen(ref);
        assertSameParty(element, (String) invoice.get("customer_code"), documentNumber);
        if (links.findFirstByDocumentNumberAndElementTypeAndElementCodeAndRemovedAtIsNull(documentNumber, ref.type(), ref.code()).isPresent()) {
            throw BadRequestException.coded("DEJA_RATTACHEE", "La facture " + documentNumber + " est déjà rattachée à " + ref.type() + " " + ref.code());
        }
        if (!Boolean.FALSE.equals(request.replaceOwnInvoice())) {
            replaceOwnInvoices(ref, documentNumber);
        }
        links.save(BillingDocumentElement.builder().documentNumber(documentNumber).elementType(ref.type()).elementCode(ref.code())
                .activationMode(mode).linkedBy(actor()).linkedAt(Instant.now()).build());
        BigDecimal total = (BigDecimal) invoice.get("total_amount");
        BigDecimal balance = (BigDecimal) invoice.get("balance_due");
        if (total != null && balance != null && balance.compareTo(total) < 0) {
            settlements.publish(documentNumber, "Rattachement de " + documentNumber + " à " + ref.type() + " " + ref.code());
        }
        return resolver.elements(documentNumber);
    }

    /** Pose ou met a jour le mode d'activation d'un element sur une facture (affectation d'un paiement a un element). */
    public void setMode(String documentNumber, ElementRef ref, String mode) {
        BillingDocumentElement link = links.findFirstByDocumentNumberAndElementTypeAndElementCodeAndRemovedAtIsNull(documentNumber, ref.type(), ref.code())
                .orElseGet(() -> BillingDocumentElement.builder().documentNumber(documentNumber).elementType(ref.type()).elementCode(ref.code())
                        .linkedBy(actor()).linkedAt(Instant.now()).build());
        link.setActivationMode(mode);
        links.save(link);
    }

    /** Defait le lien tant que la facture n'a recu aucun paiement. */
    public void unlink(String documentNumber, String type, String code) {
        ElementRef ref = ElementRef.of(type, code);
        BillingDocumentElement link = links.findFirstByDocumentNumberAndElementTypeAndElementCodeAndRemovedAtIsNull(documentNumber, ref.type(), ref.code())
                .orElseThrow(() -> new ResourceNotFoundException("Aucun lien entre " + documentNumber + " et " + ref.type() + " " + ref.code()));
        Map<String, Object> invoice = resolver.document(documentNumber).orElse(null);
        if (invoice != null && ((BigDecimal) invoice.get("balance_due")).compareTo((BigDecimal) invoice.get("total_amount")) < 0
                && !"CANCELLED".equals(invoice.get("status"))) {
            throw BadRequestException.coded("LIEN_PAYE", "La facture " + documentNumber
                    + " a déjà reçu un paiement : annulez d'abord ses affectations");
        }
        link.setRemovedAt(Instant.now());
        link.setRemovedBy(actor());
        links.save(link);
    }

    /** L'element existe et se regle encore. */
    public ElementDtos.Element assertOpen(ElementRef ref) {
        ElementDtos.Element element = resolver.element(ref);
        if (BillingElementResolver.CLOSED.contains(element.status())) {
            throw BadRequestException.coded("ELEMENT_CLOS", ref.type() + " " + ref.code() + " est " + element.status() + " : il ne se règle plus");
        }
        return element;
    }

    /**
     * Le client de la facture est le titulaire de l'element, son client personne s'il est membre, ou
     * l'entreprise qui paie pour lui (entite du client, organisation facturee de la reservation).
     */
    public void assertSameParty(ElementDtos.Element element, String customerCode, String documentNumber) {
        Set<String> parties = parties(element);
        if (customerCode != null && !parties.contains(customerCode)) {
            throw BadRequestException.coded("TIERS_DIFFERENT", documentNumber + " est au nom de " + customerCode + ", "
                    + element.type() + " " + element.code() + " est à " + element.holderCode() + " : le tiers doit être le même, ou l'entreprise qui paie pour lui");
        }
    }

    Set<String> parties(ElementDtos.Element element) {
        Set<String> parties = new LinkedHashSet<>();
        parties.add(element.holderCode());
        parties.addAll(jdbc.queryForList("SELECT c.customer_id FROM member m JOIN customer c ON c.id = m.customer_id WHERE m.member_id = ?",
                String.class, element.holderCode()));
        parties.addAll(jdbc.queryForList("SELECT b.entity_code FROM customer c JOIN business_entity b ON b.id = c.business_entity_id "
                + "WHERE c.customer_id = ? OR c.id = (SELECT m.customer_id FROM member m WHERE m.member_id = ?)", String.class,
                element.holderCode(), element.holderCode()));
        if (ElementRef.BOOKING.equals(element.type())) {
            parties.addAll(jdbc.queryForList("SELECT billing_company_code FROM booking WHERE booking_number = ? AND billing_company_code IS NOT NULL",
                    String.class, element.code()));
        }
        return parties;
    }

    /** Factures propres de l'element sans aucun paiement : annulees, la facture rattachee les remplace. */
    private void replaceOwnInvoices(ElementRef ref, String keep) {
        for (ElementDtos.Invoice own : resolver.invoices(ref)) {
            if (!BillingElementResolver.OWN.equals(own.origin()) || own.documentNumber().equals(keep)
                    || List.of("CANCELLED", "PAID", "PARTIALLY_PAID").contains(own.status())
                    || own.balanceDue() == null || own.totalAmount() == null || own.balanceDue().compareTo(own.totalAmount()) < 0) {
                continue;
            }
            documents.getObject().cancelAndArchive(own.documentNumber(), "Remplacée par " + keep + ", rattachée à " + ref.type() + " " + ref.code());
            log.info("Facture {} de {} {} annulee, remplacee par {}", own.documentNumber(), ref.type(), ref.code(), keep);
        }
    }

    private String actor() {
        try {
            return staffProfiles.currentAuthor().login();
        } catch (RuntimeException ex) {
            return "SYSTEM";
        }
    }
}
