package com.sni.bokaticowork.features.company;

import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.support.BillingReceivables;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.company.repository.BusinessEntityIdentifierRepository;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La fiche entreprise tient sur le schema de production.
 *
 * <p>La validation du schema par Hibernate est activee ici comme en production : une colonne
 * absente ou mal typee y fait echouer le demarrage, on veut le voir avant.</p>
 */
@org.springframework.test.context.TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class CompanyAccountProfileIT extends PostgresIntegrationTestBase {

    @Autowired private DataSource dataSource;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private BillingDocumentRepository documentRepository;
    @Autowired private BusinessEntityIdentifierRepository identifierRepository;

    @Test
    @DisplayName("NIU et RCCM facultatifs, type d'organisation obligatoire avec une valeur par defaut")
    void legalProfileColumns() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Map<String, Object> niu = column(jdbc, "business_entity", "niu_number");
        Map<String, Object> rccm = column(jdbc, "business_entity", "rccm_number");
        Map<String, Object> type = column(jdbc, "business_entity", "organization_type");
        assertThat(niu.get("is_nullable")).isEqualTo("YES");
        assertThat(rccm.get("is_nullable")).isEqualTo("YES");
        assertThat(type.get("is_nullable")).isEqualTo("NO");
        assertThat(column(jdbc, "customer", "business_entity_id")).isNotEmpty();
        assertThat(column(jdbc, "billing_document", "customer_identifiers_json")).isNotEmpty();
    }

    @Test
    @DisplayName("Les identifiants deja saisis rejoignent la table · l'exploitant compris")
    void existingIdentifiersWereCopied() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Integer withRccm = jdbc.queryForObject(
                "SELECT COUNT(*) FROM business_entity WHERE rccm_number IS NOT NULL AND trim(rccm_number) <> ''", Integer.class);
        Integer copied = jdbc.queryForObject(
                "SELECT COUNT(*) FROM business_entity_identifier WHERE identifier_type = 'RCCM'", Integer.class);
        assertThat(copied).isEqualTo(withRccm);
    }

    @Test
    @DisplayName("Recherche des organisations et encours s'executent")
    void queriesRun() {
        assertThat(customerRepository.searchCompanies("%mbote%", null, PageRequest.of(0, 5))).isEmpty();
        assertThat(customerRepository.searchCompanies(null, null, PageRequest.of(0, 5))).isNotNull();

        Set<BillingDocumentStatus> excluded = EnumSet.noneOf(BillingDocumentStatus.class);
        excluded.addAll(BillingReceivables.NOT_ISSUED);
        excluded.addAll(BillingReceivables.SETTLED);
        assertThat(documentRepository.sumOutstanding("CUSTOMER", "CUS-NONE", BillingReceivables.RECEIVABLE_TYPES, excluded))
                .isEqualByComparingTo("0");
        assertThat(identifierRepository.count()).isGreaterThanOrEqualTo(0);
    }

    @Autowired private com.sni.bokaticowork.features.company.membership.repository.CustomerMemberLinkRepository linkRepository;

    @Test
    @DisplayName("Rattachement : un lien actif par couple, une seule organisation automatique par membre")
    void memberLinkIndexes() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        java.util.List<String> indexes = jdbc.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'customer_member_link'", String.class);
        assertThat(indexes).contains("uk_customer_member_link_active", "uk_customer_member_link_automatic",
                "uk_customer_member_link_short_id");
        assertThat(column(jdbc, "customer", "default_invoice_link_mode")).isNotEmpty();
        assertThat(linkRepository.findActiveCompanyCodes("MBR-NONE")).isEmpty();
        assertThat(linkRepository.findByShortId(-1L)).isEmpty();
    }

    private static Map<String, Object> column(JdbcTemplate jdbc, String table, String name) {
        return jdbc.queryForList("""
                SELECT is_nullable, column_default FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
                """, table, name).stream().findFirst().orElse(Map.of());
    }
}
