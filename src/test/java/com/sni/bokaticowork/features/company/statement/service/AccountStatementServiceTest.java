package com.sni.bokaticowork.features.company.statement.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.statement.dto.StatementDtos;
import com.sni.bokaticowork.features.company.statement.dto.StatementDtos.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * L'etat de compte · facture sur la periode, du a la date de l'etat, anciennete, et par compte.
 */
class AccountStatementServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 30);

    private final BillingDocumentRepository documents = mock(BillingDocumentRepository.class);
    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final MemberRepository members = mock(MemberRepository.class);
    private final AccountStatementService service = new AccountStatementService(documents, customers, members,
            Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

    private Customer acme;
    private long ids;

    @BeforeEach
    void setUp() {
        acme = Customer.builder().id(1L).customerId("CUS-COM-1").companyName("SARL Acme").type(CustomerType.COMPANY)
                .billingEmail("compta@acme.cg").creditLimit(new BigDecimal("500000")).paymentTermsDays(30).build();
        when(customers.findByCustomerId("CUS-COM-1")).thenReturn(Optional.of(acme));
    }

    private BillingDocument doc(String number, BillingDocumentType type, BillingDocumentStatus status, String customerType,
                                String customerCode, String customerName, LocalDate issue, LocalDate due,
                                String total, String paid) {
        BigDecimal t = new BigDecimal(total), p = new BigDecimal(paid);
        return BillingDocument.builder().id(++ids).documentNumber(number).documentType(type).status(status)
                .customerType(customerType).customerCode(customerCode).customerName(customerName)
                .issueDate(issue).dueDate(due).currency("XAF")
                .totalAmount(t).paidAmount(p).balanceDue(t.subtract(p)).build();
    }

    private BillingDocument memberInvoice(String number, String member, String name, LocalDate issue, LocalDate due,
                                          String total, String paid, String source) {
        BillingDocument d = doc(number, BillingDocumentType.INVOICE, BillingDocumentStatus.SENT, "MEMBER", member, name,
                issue, due, total, paid);
        d.setCompanyCustomerCode("CUS-COM-1");
        d.setCompanyName("AT_ISSUE".equals(source) ? "SARL Acme" : null);
        d.setCompanyLinkSource(source);
        return d;
    }

    private void companyDocuments(BillingDocument... list) {
        when(documents.findCompanyStatementDocuments(eq("CUS-COM-1"), any(), any(), anyCollection(), anyCollection(), anyCollection()))
                .thenReturn(new ArrayList<>(List.of(list)));
    }

    @Test
    @DisplayName("Organisation : ses factures et celles de ses membres, totaux, retard et anciennete")
    void companyStatementTotals() {
        companyDocuments(
                doc("INV-1", BillingDocumentType.INVOICE, BillingDocumentStatus.PAID, "CUSTOMER", "CUS-COM-1", "SARL Acme",
                        LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), "200000", "200000"),
                doc("INV-2", BillingDocumentType.INVOICE, BillingDocumentStatus.OVERDUE, "CUSTOMER", "CUS-COM-1", "SARL Acme",
                        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 15), "100000", "40000"),
                memberInvoice("INV-3", "MBR-1", "Jean Mbemba", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1),
                        "50000", "0", "AT_ISSUE"));

        StatementDtos.Statement statement = service.company("CUS-COM-1", null, null);

        assertThat(statement.from()).isEqualTo(LocalDate.of(2025, 10, 31));
        assertThat(statement.to()).isEqualTo(TODAY);
        StatementDtos.Totals xaf = statement.totals().get(0);
        assertThat(xaf.invoiced()).isEqualByComparingTo("350000");
        assertThat(xaf.paid()).isEqualByComparingTo("240000");
        assertThat(xaf.balanceDue()).isEqualByComparingTo("110000");
        assertThat(xaf.overdue()).isEqualByComparingTo("60000");
        assertThat(xaf.openInvoices()).isEqualTo(2);
        assertThat(xaf.overdueInvoices()).isEqualTo(1);
        // Echeance au 15 septembre, etat au 30 octobre : 45 jours.
        assertThat(xaf.aging().days31To60()).isEqualByComparingTo("60000");
        assertThat(xaf.aging().notDue()).isEqualByComparingTo("50000");

        assertThat(statement.lines()).filteredOn(l -> l.documentNumber().equals("INV-2"))
                .singleElement().extracting(StatementDtos.Line::daysOverdue).isEqualTo(45L);
        assertThat(statement.accounts()).extracting(StatementDtos.AccountBreakdown::accountType)
                .containsExactly(AccountType.COMPANY, AccountType.MEMBER);
        assertThat(statement.accounts().get(1).accountName()).isEqualTo("Jean Mbemba");
        assertThat(statement.billingEmail()).isEqualTo("compta@acme.cg");
    }

    @Test
    @DisplayName("Une facture d'avant la periode reste dans le du, sans compter dans le facture")
    void openInvoiceBeforePeriodIsCarried() {
        companyDocuments(doc("INV-OLD", BillingDocumentType.INVOICE, BillingDocumentStatus.SENT, "CUSTOMER", "CUS-COM-1",
                "SARL Acme", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), "80000", "0"));

        StatementDtos.Statement statement = service.company("CUS-COM-1", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertThat(statement.lines().get(0).inPeriod()).isFalse();
        StatementDtos.Totals xaf = statement.totals().get(0);
        assertThat(xaf.invoiced()).isEqualByComparingTo("0");
        assertThat(xaf.balanceDue()).isEqualByComparingTo("80000");
        assertThat(xaf.aging().over90()).isEqualByComparingTo("80000");
    }

    @Test
    @DisplayName("L'avoir sur une facture de membre reliee rejoint l'etat de l'organisation et reduit le net")
    void memberCreditNoteJoinsCompanyStatement() {
        companyDocuments(memberInvoice("INV-3", "MBR-1", "Jean Mbemba", LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 11, 1), "50000", "0", "ATTACHED"));
        BillingDocument credit = doc("AV-1", BillingDocumentType.CREDIT_NOTE, BillingDocumentStatus.VALIDATED, "MEMBER",
                "MBR-1", "Jean Mbemba", LocalDate.of(2026, 10, 5), null, "10000", "0");
        credit.setSourceType("INVOICE");
        credit.setSourceCode("INV-3");
        when(documents.findCreditNotesForInvoices(anyCollection())).thenReturn(List.of(credit));

        StatementDtos.Statement statement = service.company("CUS-COM-1", null, null);

        assertThat(statement.lines()).extracting(StatementDtos.Line::documentNumber).containsExactly("INV-3", "AV-1");
        StatementDtos.Totals xaf = statement.totals().get(0);
        assertThat(xaf.creditAvailable()).isEqualByComparingTo("10000");
        assertThat(xaf.netBalanceDue()).isEqualByComparingTo("40000");
    }

    @Test
    @DisplayName("Membre : son compte et chaque organisation reliee, separes")
    void memberStatementSplitsAccounts() {
        Member jean = new Member();
        jean.setMemberId("MBR-1");
        jean.setFirstname("Jean");
        jean.setLastname("Mbemba");
        when(documents.findMemberStatementDocuments(eq("MBR-1"), any(), any(), anyCollection(), anyCollection(), anyCollection()))
                .thenReturn(List.of(
                        doc("INV-P", BillingDocumentType.INVOICE, BillingDocumentStatus.SENT, "MEMBER", "MBR-1", "Jean Mbemba",
                                LocalDate.of(2026, 10, 2), LocalDate.of(2026, 11, 2), "15000", "0"),
                        memberInvoice("INV-C", "MBR-1", "Jean Mbemba", LocalDate.of(2026, 10, 3), LocalDate.of(2026, 11, 3),
                                "30000", "0", "AT_ISSUE")));

        StatementDtos.Statement statement = service.member(jean, null, null);

        assertThat(statement.name()).isEqualTo("Jean Mbemba");
        assertThat(statement.accounts()).extracting(StatementDtos.AccountBreakdown::accountType, StatementDtos.AccountBreakdown::balanceDue)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(AccountType.PERSONAL, new BigDecimal("15000")),
                        org.assertj.core.groups.Tuple.tuple(AccountType.COMPANY, new BigDecimal("30000")));
        assertThat(statement.lines()).filteredOn(l -> l.documentNumber().equals("INV-C"))
                .singleElement().extracting(StatementDtos.Line::accountName).isEqualTo("SARL Acme");
    }

    @Test
    @DisplayName("Periode a l'envers, ou de plus de trois ans : refus")
    void periodIsValidated() {
        assertThatThrownBy(() -> service.company("CUS-COM-1", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 9, 1)))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.company("CUS-COM-1", LocalDate.of(2020, 1, 1), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("trois ans");
    }

    @Test
    @DisplayName("Un client personne n'a pas d'etat d'organisation")
    void personIsRefused() {
        acme.setType(CustomerType.PERSON);
        assertThatThrownBy(() -> service.company("CUS-COM-1", null, null)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("CSV : une valeur commencant par = ne devient pas une formule")
    void csvNeutralizesFormulas() {
        assertThat(AccountStatementExporter.cell("=SUM(A1)")).isEqualTo("\"'=SUM(A1)\"");
        assertThat(AccountStatementExporter.cell("Ets \"Mboté\"")).isEqualTo("\"Ets \"\"Mboté\"\"\"");
    }
}
