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

    @Test
    @DisplayName("Facture reliee : nom de l'organisation fige une fois scellee, lien modifiable pour rattacher")
    void companyLinkOnInvoices() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String trigger = jdbc.queryForObject(
                "SELECT prosrc FROM pg_proc WHERE proname = 'fn_billing_document_check_immutable'", String.class);
        assertThat(trigger).contains("company_name").contains("company_identifiers_json")
                .doesNotContain("company_customer_code").doesNotContain("company_link_source");
        assertThat(column(jdbc, "booking", "billing_company_code")).isNotEmpty();
        assertThat(column(jdbc, "billable_item", "billing_company_code")).isNotEmpty();

        Set<BillingDocumentStatus> excluded = EnumSet.noneOf(BillingDocumentStatus.class);
        excluded.addAll(BillingReceivables.NOT_ISSUED);
        excluded.addAll(BillingReceivables.SETTLED);
        assertThat(documentRepository.sumCompanyOutstanding("CUS-NONE", BillingReceivables.RECEIVABLE_TYPES, excluded))
                .isEqualByComparingTo("0");
        assertThat(documentRepository.findMemberInvoicesBetween("MBR-NONE", java.time.LocalDate.of(2026, 1, 1),
                java.time.LocalDate.of(2026, 12, 31), BillingReceivables.RECEIVABLE_TYPES)).isEmpty();
    }

    @Autowired private com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository memberRepository;

    @Test
    @DisplayName("Etats de compte : les requetes de l'organisation, du membre et des avoirs s'executent")
    void statementsRun() {
        var types = EnumSet.of(com.sni.bokaticowork.features.billing.enums.BillingDocumentType.INVOICE,
                com.sni.bokaticowork.features.billing.enums.BillingDocumentType.CREDIT_NOTE);
        Set<BillingDocumentStatus> closed = EnumSet.noneOf(BillingDocumentStatus.class);
        closed.addAll(BillingReceivables.NOT_ISSUED);
        closed.addAll(BillingReceivables.SETTLED);
        java.time.LocalDate from = java.time.LocalDate.of(2026, 1, 1), to = java.time.LocalDate.of(2026, 12, 31);
        assertThat(documentRepository.findCompanyStatementDocuments("CUS-NONE", from, to, types,
                BillingReceivables.RECEIVABLE_TYPES, closed)).isEmpty();
        assertThat(documentRepository.findMemberStatementDocuments("MBR-NONE", from, to, types,
                BillingReceivables.RECEIVABLE_TYPES, closed)).isEmpty();
        assertThat(documentRepository.findCreditNotesForInvoices(java.util.List.of("INV-NONE"))).isEmpty();

        var service = new com.sni.bokaticowork.features.company.statement.service.AccountStatementService(
                documentRepository, customerRepository, memberRepository);
        customerRepository.searchCompanies(null, null, PageRequest.of(0, 1)).stream().findFirst()
                .ifPresent(company -> assertThat(service.company(company.getCustomerId(), from, to).totals()).isNotNull());
    }

    @Autowired private com.sni.bokaticowork.features.company.sharing.repository.CompanySeatRepository seatRepository;
    @Autowired private com.sni.bokaticowork.features.company.sharing.repository.SeatUsageRepository seatUsageRepository;
    @Autowired private com.sni.bokaticowork.features.company.sharing.repository.EntitlementSharingRepository sharingRepository;
    @Autowired private com.sni.bokaticowork.features.company.sharing.repository.SeatAllowanceRepository allowanceRepository;
    @Autowired private com.sni.bokaticowork.features.booking.repository.BookingRepository bookingRepository;

    @Test
    @DisplayName("Abonnement partage : colonnes, numero court des places et requetes")
    void sharedSubscriptionRuns() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(column(jdbc, "subscription_seat", "short_id").get("is_nullable")).isEqualTo("NO");
        assertThat(column(jdbc, "booking", "subscription_seat_id")).isNotEmpty();
        assertThat(column(jdbc, "usage_record", "beneficiary_code")).isNotEmpty();

        java.time.LocalDate period = java.time.LocalDate.of(2026, 10, 1);
        assertThat(seatRepository.findActiveCompanySeatsOfMember("MBR-NONE")).isEmpty();
        assertThat(seatRepository.findActiveSeatsOfMemberInCompany("MBR-NONE", "CUS-NONE")).isEmpty();
        assertThat(seatRepository.findActiveBySubscription(-1L)).isEmpty();
        assertThat(seatRepository.findOpenCompanySubscriptions("CUS-NONE")).isEmpty();
        assertThat(seatRepository.countActive(-1L)).isZero();
        assertThat(seatRepository.lockSubscription(-1L)).isEmpty();
        assertThat(seatUsageRepository.sumSeatQuota(-1L, "X", period)).isEqualByComparingTo("0");
        assertThat(seatUsageRepository.sumSeatTotal(-1L, "X", period)).isEqualByComparingTo("0");
        assertThat(seatUsageRepository.sumPool(-1L, "X", period)).isEqualByComparingTo("0");
        assertThat(seatUsageRepository.findPeriodUsage(-1L, period)).isEmpty();
        assertThat(sharingRepository.findAllBySubscription_Id(-1L)).isEmpty();
        assertThat(allowanceRepository.findAllBySeat_IdIn(java.util.List.of(-1L))).isEmpty();
        assertThat(bookingRepository.findFutureBookingNumbersOnSeats(java.util.List.of(-1L), java.time.LocalDateTime.now())).isEmpty();
    }

    @Autowired private com.sni.bokaticowork.features.subscription.subscription.pass.repository.PassBeneficiaryRepository beneficiaryRepository;
    @Autowired private com.sni.bokaticowork.features.subscription.repository.EntitlementGrantRepository grantRepository;
    @Autowired private com.sni.bokaticowork.features.subscription.repository.EntitlementLedgerRepository ledgerRepository;
    @Autowired private com.sni.bokaticowork.features.subscription.overage.repository.SubscriptionOverageChargeRepository chargeRepository;

    @Test
    @DisplayName("Passes partages, beneficiaire sur le journal, plafond de places et annexe : colonnes et requetes")
    void passSharingAndAnnexRun() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(column(jdbc, "pass_beneficiary", "short_id").get("is_nullable")).isEqualTo("NO");
        assertThat(column(jdbc, "entitlement_ledger", "beneficiary_code")).isNotEmpty();
        assertThat(column(jdbc, "entitlement_reservation", "beneficiary_code")).isNotEmpty();
        assertThat(column(jdbc, "subscription", "seat_ceiling")).isNotEmpty();
        assertThat(column(jdbc, "billing_document", "usage_annex_json")).isNotEmpty();
        String trigger = jdbc.queryForObject(
                "SELECT prosrc FROM pg_proc WHERE proname = 'fn_billing_document_check_immutable'", String.class);
        assertThat(trigger).contains("usage_annex_json").contains("company_identifiers_json");

        var customer = com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType.CUSTOMER;
        assertThat(beneficiaryRepository.findOpenAccesses("MEMBER", "MBR-NONE", customer)).isEmpty();
        assertThat(beneficiaryRepository.findOpenAccessesWithOwner("MEMBER", "MBR-NONE", customer, "CUS-NONE")).isEmpty();
        assertThat(beneficiaryRepository.findByShortId(-1L)).isEmpty();
        assertThat(grantRepository.findUsableGrantOnPassForResource(-1L, null, null, java.time.Instant.now())).isEmpty();
        assertThat(grantRepository.findAllByPassId(-1L)).isEmpty();
        assertThat(ledgerRepository.sumConsumedByBeneficiaryOnPass(-1L)).isEmpty();
        assertThat(bookingRepository.findFutureBookingNumbersOnPass("PAS-NONE", "MBR-NONE", java.time.LocalDateTime.now())).isEmpty();
        java.time.LocalDate november = java.time.LocalDate.of(2026, 11, 1);
        assertThat(seatUsageRepository.findConsumedInPeriod(-1L, november)).isEmpty();
        assertThat(seatUsageRepository.findLastPeriodBefore(-1L, november, november.minusMonths(13))).isNull();
        assertThat(chargeRepository.findBeneficiaryCharges(-1L, java.time.Instant.EPOCH, java.time.Instant.now())).isEmpty();
    }

    @Autowired private com.sni.bokaticowork.features.company.wallet.repository.SpendingAuthorizationRepository authorizationRepository;
    @Autowired private com.sni.bokaticowork.features.company.wallet.repository.CompanyWalletSpendRepository spendRepository;

    @Test
    @DisplayName("Portefeuille de l'organisation : autorisations, depenses et leurs requetes")
    void companyWalletRuns() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(column(jdbc, "wallet_spending_authorization", "short_id").get("is_nullable")).isEqualTo("NO");
        assertThat(column(jdbc, "wallet_company_spend", "spent_by_member").get("is_nullable")).isEqualTo("NO");
        java.util.List<String> indexes = jdbc.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename IN ('wallet_spending_authorization', 'wallet_company_spend')", String.class);
        assertThat(indexes).contains("uk_wallet_spending_authorization_open", "uk_wallet_company_spend_pending_document");

        java.time.Instant now = java.time.Instant.now();
        var page = org.springframework.data.domain.PageRequest.of(0, 5);
        assertThat(authorizationRepository.findAllOfCompany("CUS-NONE")).isEmpty();
        assertThat(authorizationRepository.findOpen(-1L, "MBR-NONE")).isEmpty();
        assertThat(authorizationRepository.findOpenOfMember("MBR-NONE")).isEmpty();
        assertThat(authorizationRepository.findActiveOfMemberInCompany("MBR-NONE", "CUS-NONE")).isEmpty();
        assertThat(authorizationRepository.findByShortId(-1L)).isEmpty();
        assertThat(spendRepository.sumEngagedSince(-1L, "MBR-NONE", now)).isEqualByComparingTo("0");
        assertThat(spendRepository.findOfWallet(-1L, null, page)).isEmpty();
        assertThat(spendRepository.findOfWallet(-1L, com.sni.bokaticowork.features.company.wallet.model.CompanyWalletSpend.Status.PENDING_APPROVAL, page)).isEmpty();
        assertThat(spendRepository.findOfMember(-1L, "MBR-NONE", page)).isEmpty();
        assertThat(spendRepository.findExpiredPending(now, page)).isEmpty();
        assertThat(spendRepository.findPendingOfMemberInCompany("CUS-NONE", "MBR-NONE")).isEmpty();
        assertThat(spendRepository.existsPendingForDocument("INV-NONE")).isFalse();
        assertThat(spendRepository.findByIntentNumbers(java.util.List.of("INT-NONE"))).isEmpty();
        assertThat(spendRepository.lockByShortId(-1L)).isEmpty();
        assertThat(billableItemRepository.findPreviousPassPeriodStart("PAS-NONE", java.time.LocalDate.of(2026, 11, 1))).isNull();
        assertThat(ledgerRepository.sumConsumedByBeneficiaryOnPassBetween(-1L, java.time.Instant.EPOCH, now)).isEmpty();
    }

    @Autowired private com.sni.bokaticowork.features.subscription.repository.BillableItemRepository billableItemRepository;

    private static Map<String, Object> column(JdbcTemplate jdbc, String table, String name) {
        return jdbc.queryForList("""
                SELECT is_nullable, column_default FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
                """, table, name).stream().findFirst().orElse(Map.of());
    }
}
