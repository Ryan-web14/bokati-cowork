package com.sni.bokaticowork.features.billing.element.service;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.billing.element.dto.ElementDtos;
import com.sni.bokaticowork.features.billing.element.model.BillingDocumentElement;
import com.sni.bokaticowork.features.billing.element.model.ElementRef;
import com.sni.bokaticowork.features.billing.element.repository.BillingDocumentElementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Lien entre les factures et les elements qu'elles reglent (lot R1).
 *
 * <p>Une facture regle un element de deux facons : elle a ete produite par lui (ses lignes facturables
 * portent son numero dans {@code billable_item.source_id}), ou l'equipe l'y a rattachee (table
 * {@code billing_document_element}, ou {@code linkedInvoiceNumber} d'un abonnement). Une domiciliation
 * est facturee par son abonnement parent : ses factures sont celles de cet abonnement, plus celles
 * rattachees a la domiciliation elle-meme.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillingElementResolver {

    static final String OWN = "OWN";
    static final String LINKED = "LINKED";
    /** Etats ou un element ne se regle plus. */
    static final List<String> CLOSED = List.of("CANCELLED", "REJECTED", "EXPIRED", "TERMINATED");

    private static final String INVOICE_COLUMNS = "d.document_number, d.document_type, d.status, d.customer_type, d.customer_code, d.currency, "
            + "d.total_amount, d.balance_due, d.due_date";

    private final JdbcTemplate jdbc;
    private final BillingDocumentElementRepository links;

    // ------------------------------------------------------------------ element vers factures

    /** Factures d'un element, produites par lui puis rattachees, sans les factures supprimees. */
    public List<ElementDtos.Invoice> invoices(ElementRef ref) {
        Map<String, ElementDtos.Invoice> found = new LinkedHashMap<>();
        String billedCode = ref.code();
        if (ElementRef.DOMICILIATION.equals(ref.type())) {
            billedCode = domiciliationSubscription(ref.code())
                    .orElseThrow(() -> new ResourceNotFoundException("Domiciliation introuvable : " + ref.code()));
        }
        for (Map<String, Object> row : jdbc.queryForList("SELECT " + INVOICE_COLUMNS + " FROM billing_document d "
                + "WHERE d.deleted_at IS NULL AND d.id IN (SELECT b.invoice_id FROM billable_item b WHERE b.source_id = ? AND b.invoice_id IS NOT NULL) "
                + "ORDER BY d.due_date NULLS LAST, d.document_number", billedCode)) {
            ElementDtos.Invoice invoice = invoice(row, OWN, null);
            found.putIfAbsent(invoice.documentNumber(), invoice);
        }
        if (ElementRef.SUBSCRIPTION.equals(ref.type()) || ElementRef.DOMICILIATION.equals(ref.type())) {
            List<String> linked = jdbc.queryForList("SELECT metadata_json ->> 'linkedInvoiceNumber' FROM subscription WHERE subscription_number = ? "
                    + "AND metadata_json ->> 'linkedInvoiceNumber' IS NOT NULL", String.class, billedCode);
            for (String number : linked) {
                document(number).ifPresent(row -> found.putIfAbsent(number, invoice(row, LINKED, ElementRef.FULL_SETTLEMENT)));
            }
        }
        List<BillingDocumentElement> manual = new ArrayList<>(links.findByElementTypeAndElementCodeAndRemovedAtIsNullOrderByLinkedAtAsc(ref.type(), ref.code()));
        if (ElementRef.DOMICILIATION.equals(ref.type())) {
            manual.addAll(links.findByElementTypeAndElementCodeAndRemovedAtIsNullOrderByLinkedAtAsc(ElementRef.SUBSCRIPTION, billedCode));
        }
        for (BillingDocumentElement link : manual) {
            document(link.getDocumentNumber()).ifPresent(row -> found.putIfAbsent(link.getDocumentNumber(),
                    invoice(row, LINKED, link.getActivationMode())));
        }
        return List.copyOf(found.values());
    }

    /** Factures ouvertes d'un element (reste du positif, ni brouillon ni annulee), echeance la plus ancienne d'abord. */
    public List<ElementDtos.Invoice> openInvoices(ElementRef ref) {
        return invoices(ref).stream()
                .filter(invoice -> invoice.balanceDue() != null && invoice.balanceDue().signum() > 0)
                .filter(invoice -> !List.of("DRAFT", "CANCELLED", "PAID").contains(invoice.status()))
                .filter(invoice -> List.of("INVOICE", "CORRECTIVE_INVOICE", "DEBIT_NOTE").contains(invoice.documentType()))
                .sorted(java.util.Comparator.comparing(ElementDtos.Invoice::dueDate, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .toList();
    }

    // ------------------------------------------------------------------ facture vers elements

    /** Elements regles par une facture : produits par ses lignes, puis rattaches. */
    public List<ElementDtos.Settled> elements(String documentNumber) {
        Map<String, ElementDtos.Settled> found = new LinkedHashMap<>();
        for (Map<String, Object> row : jdbc.queryForList("SELECT DISTINCT b.source_type, b.source_id FROM billable_item b "
                + "JOIN billing_document d ON d.id = b.invoice_id WHERE d.document_number = ? AND b.source_id IS NOT NULL", documentNumber)) {
            String type = ElementRef.typeOfBillableSource((String) row.get("source_type"));
            if (type != null) {
                String code = (String) row.get("source_id");
                found.putIfAbsent(type + ":" + code, new ElementDtos.Settled(type, code, OWN, ElementRef.FULL_SETTLEMENT));
            }
        }
        for (String subscription : jdbc.queryForList("SELECT subscription_number FROM subscription WHERE metadata_json ->> 'linkedInvoiceNumber' = ?",
                String.class, documentNumber)) {
            found.putIfAbsent(ElementRef.SUBSCRIPTION + ":" + subscription,
                    new ElementDtos.Settled(ElementRef.SUBSCRIPTION, subscription, LINKED, ElementRef.FULL_SETTLEMENT));
        }
        for (BillingDocumentElement link : links.findByDocumentNumberAndRemovedAtIsNullOrderByLinkedAtAsc(documentNumber)) {
            // Le mode choisi par l'administrateur vaut aussi sur une facture produite par l'element.
            String key = link.getElementType() + ":" + link.getElementCode();
            ElementDtos.Settled own = found.get(key);
            found.put(key, new ElementDtos.Settled(link.getElementType(), link.getElementCode(), own == null ? LINKED : own.origin(),
                    link.getActivationMode()));
        }
        return List.copyOf(found.values());
    }

    // ------------------------------------------------------------------ elements et tiers

    /** L'element, ou une erreur 404 s'il n'existe pas. */
    public ElementDtos.Element element(ElementRef ref) {
        String sql = switch (ref.type()) {
            case ElementRef.SUBSCRIPTION -> "SELECT 'SUBSCRIPTION' AS type, s.subscription_number AS code, s.subscription_number AS label, s.status, "
                    + "s.subscriber_type AS holder_type, s.subscriber_code AS holder_code, s.start_date AS start_date, s.current_period_end AS end_date, "
                    + "s.total_amount FROM subscription s WHERE s.subscription_number = ?";
            case ElementRef.PASS -> "SELECT 'PASS' AS type, p.pass_number AS code, p.pass_number AS label, p.status, p.owner_type AS holder_type, "
                    + "p.owner_code AS holder_code, CAST(p.valid_from AS date) AS start_date, CAST(p.valid_until AS date) AS end_date, p.total_amount "
                    + "FROM subscription_pass p WHERE p.pass_number = ?";
            case ElementRef.BOOKING -> "SELECT 'BOOKING' AS type, b.booking_number AS code, COALESCE(r.name, b.booking_number) AS label, b.status, "
                    + "b.owner_type AS holder_type, b.owner_code AS holder_code, CAST(b.started_at AS date) AS start_date, "
                    + "CAST(b.ended_at AS date) AS end_date, b.total_amount FROM booking b LEFT JOIN resource r ON r.id = b.resource_id "
                    + "WHERE b.booking_number = ?";
            default -> "SELECT 'DOMICILIATION' AS type, c.contract_number AS code, c.contract_number AS label, c.status, s.subscriber_type AS holder_type, "
                    + "s.subscriber_code AS holder_code, s.start_date AS start_date, s.current_period_end AS end_date, s.total_amount "
                    + "FROM domiciliation_contract c JOIN subscription s ON s.id = c.subscription_id WHERE c.contract_number = ?";
        };
        return jdbc.queryForList(sql, ref.code()).stream().findFirst().map(BillingElementResolver::element)
                .orElseThrow(() -> new ResourceNotFoundException("Élément introuvable : " + ref.type() + " " + ref.code()));
    }

    /** Numero de l'abonnement parent d'une domiciliation. */
    public Optional<String> domiciliationSubscription(String contractNumber) {
        return jdbc.queryForList("SELECT s.subscription_number FROM domiciliation_contract c JOIN subscription s ON s.id = c.subscription_id "
                + "WHERE c.contract_number = ?", String.class, contractNumber).stream().findFirst();
    }

    /** Elements rattachables d'un tiers (ni annules, ni termines), les plus recents d'abord. */
    public List<ElementDtos.Element> elementsOf(String partyType, String partyCode) {
        List<String> codes = new ArrayList<>(List.of(partyCode));
        if ("MEMBER".equalsIgnoreCase(partyType)) {
            // Un membre est aussi facture sous son client personne.
            codes.addAll(jdbc.queryForList("SELECT c.customer_id FROM member m JOIN customer c ON c.id = m.customer_id WHERE m.member_id = ?",
                    String.class, partyCode));
        }
        List<ElementDtos.Element> result = new ArrayList<>();
        String closed = "('" + String.join("', '", CLOSED) + "')";
        for (String code : codes) {
            jdbc.queryForList("SELECT subscription_number FROM subscription WHERE subscriber_code = ? AND status NOT IN " + closed
                    + " ORDER BY start_date DESC LIMIT 50", String.class, code)
                    .forEach(number -> result.add(element(new ElementRef(ElementRef.SUBSCRIPTION, number))));
            jdbc.queryForList("SELECT c.contract_number FROM domiciliation_contract c JOIN subscription s ON s.id = c.subscription_id "
                    + "WHERE s.subscriber_code = ? AND c.status NOT IN " + closed + " ORDER BY c.id DESC LIMIT 20", String.class, code)
                    .forEach(number -> result.add(element(new ElementRef(ElementRef.DOMICILIATION, number))));
            jdbc.queryForList("SELECT pass_number FROM subscription_pass WHERE owner_code = ? AND status NOT IN " + closed
                    + " ORDER BY valid_from DESC NULLS LAST LIMIT 50", String.class, code)
                    .forEach(number -> result.add(element(new ElementRef(ElementRef.PASS, number))));
            jdbc.queryForList("SELECT booking_number FROM booking WHERE owner_code = ? AND status NOT IN " + closed
                    + " ORDER BY started_at DESC LIMIT 50", String.class, code)
                    .forEach(number -> result.add(element(new ElementRef(ElementRef.BOOKING, number))));
        }
        return result;
    }

    /**
     * Tiers dont le nom, le courriel, le telephone ou le code contient la recherche : membres, clients
     * (personnes, entreprises, clients externes), entites, et contacts de reservation sans compte.
     */
    public List<ElementDtos.Party> search(String query, int limit) {
        if (!StringUtils.hasText(query) || query.trim().length() < 2) {
            throw com.sni.bokaticowork.core.exception.customs.BadRequestException.coded("RECHERCHE_TROP_COURTE",
                    "Saisissez au moins deux caractères");
        }
        String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
        int max = Math.max(1, Math.min(limit, 50));
        List<ElementDtos.Party> parties = new ArrayList<>();
        for (Map<String, Object> row : jdbc.queryForList("SELECT member_id AS code, TRIM(COALESCE(firstname, '') || ' ' || COALESCE(lastname, '')) AS name, "
                + "email, COALESCE(phone, whatsapp_phone) AS phone FROM member WHERE deleted IS NOT TRUE AND (LOWER(member_id) LIKE ? "
                + "OR LOWER(COALESCE(firstname, '') || ' ' || COALESCE(lastname, '')) LIKE ? OR LOWER(COALESCE(email, '')) LIKE ? "
                + "OR COALESCE(phone, '') LIKE ? OR COALESCE(whatsapp_phone, '') LIKE ?) LIMIT ?", like, like, like, like, like, max)) {
            parties.add(party("MEMBER", row, false));
        }
        // Un client sans membre ni entite est un client externe (saisi au comptoir, sans compte).
        for (Map<String, Object> row : jdbc.queryForList("SELECT c.customer_id AS code, COALESCE(NULLIF(c.company_name, ''), "
                + "TRIM(COALESCE(c.firstname, '') || ' ' || COALESCE(c.lastname, ''))) AS name, c.email, COALESCE(c.phone, c.whatsapp_phone) AS phone, "
                + "(NOT EXISTS (SELECT 1 FROM member m WHERE m.customer_id = c.id) AND c.business_entity_id IS NULL) AS external "
                + "FROM customer c WHERE c.deleted IS NOT TRUE AND (LOWER(c.customer_id) LIKE ? OR LOWER(COALESCE(c.company_name, '')) LIKE ? "
                + "OR LOWER(COALESCE(c.firstname, '') || ' ' || COALESCE(c.lastname, '')) LIKE ? OR LOWER(COALESCE(c.email, '')) LIKE ? "
                + "OR COALESCE(c.phone, '') LIKE ? OR COALESCE(c.whatsapp_phone, '') LIKE ?) LIMIT ?", like, like, like, like, like, like, max)) {
            parties.add(party("CUSTOMER", row, Boolean.TRUE.equals(row.get("external"))));
        }
        for (Map<String, Object> row : jdbc.queryForList("SELECT entity_code AS code, name, email, phone FROM business_entity "
                + "WHERE deleted IS NOT TRUE AND (LOWER(entity_code) LIKE ? OR LOWER(COALESCE(name, '')) LIKE ? OR LOWER(COALESCE(email, '')) LIKE ? "
                + "OR COALESCE(phone, '') LIKE ?) LIMIT ?", like, like, like, like, max)) {
            parties.add(party("BUSINESS_ENTITY", row, false));
        }
        // Contacts de reservation : un client externe peut n'exister que par le contact saisi.
        java.util.Set<String> known = new java.util.HashSet<>();
        parties.forEach(party -> known.add(party.code()));
        for (Map<String, Object> row : jdbc.queryForList("SELECT DISTINCT b.owner_type AS type, b.owner_code AS code, b.contact_name AS name, "
                + "b.contact_email AS email, b.contact_phone AS phone FROM booking b WHERE LOWER(COALESCE(b.contact_name, '')) LIKE ? "
                + "OR LOWER(COALESCE(b.contact_email, '')) LIKE ? OR COALESCE(b.contact_phone, '') LIKE ? LIMIT ?", like, like, like, max)) {
            String code = (String) row.get("code");
            if (code != null && known.add(code)) {
                parties.add(party((String) row.get("type"), row, true));
            }
        }
        return parties;
    }

    // ------------------------------------------------------------------ lectures

    /** Ligne de la facture (numero, type, statut, client, montants), si elle existe et n'est pas supprimee. */
    public Optional<Map<String, Object>> document(String documentNumber) {
        return jdbc.queryForList("SELECT " + INVOICE_COLUMNS + " FROM billing_document d WHERE d.document_number = ? AND d.deleted_at IS NULL",
                documentNumber).stream().findFirst();
    }

    private ElementDtos.Party party(String type, Map<String, Object> row, boolean external) {
        String code = (String) row.get("code");
        return new ElementDtos.Party(type, code, (String) row.get("name"), (String) row.get("email"), (String) row.get("phone"), external,
                elementsOf(type, code));
    }

    private static ElementDtos.Invoice invoice(Map<String, Object> row, String origin, String mode) {
        return new ElementDtos.Invoice((String) row.get("document_number"), (String) row.get("document_type"), (String) row.get("status"),
                (String) row.get("customer_type"), (String) row.get("customer_code"), (String) row.get("currency"),
                (BigDecimal) row.get("total_amount"), (BigDecimal) row.get("balance_due"), date(row.get("due_date")), origin, mode);
    }

    private static ElementDtos.Element element(Map<String, Object> row) {
        return new ElementDtos.Element((String) row.get("type"), (String) row.get("code"), (String) row.get("label"), (String) row.get("status"),
                (String) row.get("holder_type"), (String) row.get("holder_code"), date(row.get("start_date")), date(row.get("end_date")),
                (BigDecimal) row.get("total_amount"));
    }

    private static java.time.LocalDate date(Object value) {
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof java.time.LocalDate localDate) {
            return localDate;
        }
        return null;
    }
}
