package com.sni.bokaticowork.features.document.documentMaster.service.support;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Codes metier des proprietaires de document qui ne sont ni membre, ni client, ni entreprise
 * (lot KG4) : contrat ({@code contractCode}), facture ({@code documentNumber}), paiement
 * ({@code transactionNumber}), proposition commerciale ({@code proposalNumber}), actif
 * ({@code assetCode}).
 *
 * <p>Avant, ces proprietaires s'indiquaient par leur identifiant interne en texte, et la reponse ne
 * rendait que cet identifiant. L'identifiant numerique reste accepte (anciens ecrans).</p>
 */
@Component
public class DocumentOwnerCodes {

    /** Table, colonne du code, colonne du libelle (ou nulle). */
    private record Source(String table, String codeColumn, String labelColumn) {
    }

    private static final Map<DocumentOwnerType, Source> SOURCES = Map.of(
            DocumentOwnerType.CONTRACT, new Source("contract_record", "contract_code", "title"),
            DocumentOwnerType.INVOICE, new Source("billing_document", "document_number", "title"),
            DocumentOwnerType.PAYMENT, new Source("payment_transaction", "transaction_number", null),
            DocumentOwnerType.PROPOSAL, new Source("crm_commercial_proposal", "proposal_number", "title"),
            DocumentOwnerType.ASSET, new Source("asset", "asset_code", null));

    /** Pour la reponse seulement : code et nom des membres, clients et entreprises. */
    private static final Map<DocumentOwnerType, Source> PARTIES = Map.of(
            DocumentOwnerType.MEMBER, new Source("member", "member_id", "trim(coalesce(firstname, '') || ' ' || coalesce(lastname, ''))"),
            DocumentOwnerType.CUSTOMER, new Source("customer", "customer_id",
                    "coalesce(nullif(company_name, ''), trim(coalesce(firstname, '') || ' ' || coalesce(lastname, '')))"),
            DocumentOwnerType.BUSINESS, new Source("business_entity", "entity_code", "name"));

    @PersistenceContext
    private EntityManager entityManager;

    public boolean handles(DocumentOwnerType type) {
        return type != null && SOURCES.containsKey(type);
    }

    /** Identifiant interne du proprietaire d'apres son code metier (ou son identifiant numerique). */
    public Optional<Long> idOf(DocumentOwnerType type, String code) {
        Source source = SOURCES.get(type);
        if (source == null || !StringUtils.hasText(code)) {
            return Optional.empty();
        }
        String value = code.trim();
        List<?> rows = entityManager.createNativeQuery("SELECT id FROM " + source.table() + " WHERE " + source.codeColumn() + " = :code")
                .setParameter("code", value).setMaxResults(1).getResultList();
        if (!rows.isEmpty()) {
            return Optional.of(((Number) rows.get(0)).longValue());
        }
        if (value.chars().allMatch(Character::isDigit)) {
            long id = Long.parseLong(value);
            List<?> byId = entityManager.createNativeQuery("SELECT id FROM " + source.table() + " WHERE id = :id")
                    .setParameter("id", id).setMaxResults(1).getResultList();
            if (!byId.isEmpty()) {
                return Optional.of(id);
            }
        }
        return Optional.empty();
    }

    /** Code metier et libelle du proprietaire, pour la reponse. */
    public Optional<String[]> codeAndLabel(DocumentOwnerType type, Long id) {
        Source source = SOURCES.containsKey(type) ? SOURCES.get(type) : PARTIES.get(type);
        if (source == null || id == null || entityManager == null) {
            return Optional.empty();
        }
        String label = source.labelColumn() == null ? "NULL" : source.labelColumn();
        try {
            List<?> rows = entityManager.createNativeQuery("SELECT " + source.codeColumn() + ", " + label + " FROM " + source.table()
                    + " WHERE id = :id").setParameter("id", id).setMaxResults(1).getResultList();
            if (rows.isEmpty()) {
                return Optional.empty();
            }
            Object[] row = (Object[]) rows.get(0);
            return Optional.of(new String[]{row[0] == null ? null : row[0].toString(), row[1] == null ? null : row[1].toString()});
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }
}
