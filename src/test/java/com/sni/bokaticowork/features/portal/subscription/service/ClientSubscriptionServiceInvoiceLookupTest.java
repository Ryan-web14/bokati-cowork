package com.sni.bokaticowork.features.portal.subscription.service;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

/**
 * Recherche de la facture à payer.
 *
 * <p>« No payable invoice found » sur un abonnement qui avait manifestement quelque chose à régler.
 * Deux défauts se combinaient, et le second masquait le premier : la recherche ne demandait qu'une
 * seule facture avant de filtrer celles dont le solde est non nul, et la requête rend les plus
 * récentes d'abord. Il suffisait donc que la dernière émise soit réglée pour que tout le reste
 * devienne inaccessible.</p>
 *
 * <p>Un abonnement émet dès sa création une facture de loyer, une de frais d'entrée et une de
 * caution. Le cas n'était pas rare, il était garanti.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClientSubscriptionServiceInvoiceLookupTest {

    @Mock
    private BillingDocumentService billingDocumentService;

    private ClientSubscriptionService service;

    private BillingDocumentResponse find(String currency) {
        service = new ClientSubscriptionService(null, null, null, billingDocumentService, null, null, null);
        return ReflectionTestUtils.invokeMethod(service, "findPayableInvoice",
                "SUBSCRIPTION", "SUB-0001", currency);
    }

    private void givenInvoices(BillingDocumentResponse... invoices) {
        when(billingDocumentService.list(any(), isNull(), isNull(), isNull(),
                anyString(), anyString(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PaginatedResponse<>(new PageImpl<>(List.of(invoices))));
    }

    /** Le cas du bug : la plus récente est réglée, une autre attend derrière. */
    @Test
    void findsAnUnpaidInvoiceEvenWhenTheMostRecentOneIsSettled() {
        givenInvoices(
                invoice("INV-CAUTION", "2026-09-10", "0", BillingDocumentStatus.PAID),
                invoice("INV-LOYER", "2026-09-01", "25000", BillingDocumentStatus.ISSUED));

        assertEquals("INV-LOYER", find(null).documentNumber());
    }

    /** On solde une dette en commençant par la plus ancienne. */
    @Test
    void settlesTheOldestOutstandingInvoiceFirst() {
        givenInvoices(
                invoice("INV-SEPTEMBRE", "2026-09-01", "25000", BillingDocumentStatus.ISSUED),
                invoice("INV-AOUT", "2026-08-01", "25000", BillingDocumentStatus.ISSUED));

        assertEquals("INV-AOUT", find(null).documentNumber());
    }

    @Test
    void ignoresCancelledAndDraftInvoices() {
        givenInvoices(
                invoice("INV-ANNULEE", "2026-08-01", "25000", BillingDocumentStatus.CANCELLED),
                invoice("INV-BROUILLON", "2026-08-15", "25000", BillingDocumentStatus.DRAFT),
                invoice("INV-REELLE", "2026-09-01", "25000", BillingDocumentStatus.ISSUED));

        assertEquals("INV-REELLE", find(null).documentNumber());
    }

    /** Le portefeuille est mono-devise · une conversion silencieuse est une perte pour quelqu'un. */
    @Test
    void ignoresAnInvoiceInAnotherCurrency() {
        givenInvoices(
                invoice("INV-EUR", "2026-08-01", "40", BillingDocumentStatus.ISSUED, "EUR"),
                invoice("INV-XAF", "2026-09-01", "25000", BillingDocumentStatus.ISSUED, "XAF"));

        assertEquals("INV-XAF", find("XAF").documentNumber());
    }

    @Test
    void complainsClearlyWhenEverythingIsAlreadyPaid() {
        givenInvoices(invoice("INV-LOYER", "2026-09-01", "0", BillingDocumentStatus.PAID));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> find(null));

        assertEquals("Aucune facture à payer pour SUB-0001", ex.getMessage());
    }

    // -------------------------------------------------------------------------------------

    private BillingDocumentResponse invoice(String number, String issueDate, String balanceDue,
                                            BillingDocumentStatus status) {
        return invoice(number, issueDate, balanceDue, status, "XAF");
    }

    private BillingDocumentResponse invoice(String number, String issueDate, String balanceDue,
                                            BillingDocumentStatus status, String currency) {
        BillingDocumentResponse document = org.mockito.Mockito.mock(BillingDocumentResponse.class);
        when(document.documentNumber()).thenReturn(number);
        when(document.issueDate()).thenReturn(LocalDate.parse(issueDate));
        when(document.balanceDue()).thenReturn(new BigDecimal(balanceDue));
        when(document.status()).thenReturn(status);
        when(document.currency()).thenReturn(currency);
        return document;
    }
}
