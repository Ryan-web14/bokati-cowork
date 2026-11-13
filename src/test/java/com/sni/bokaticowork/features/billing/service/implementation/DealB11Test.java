package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.request.DealStageRequest;
import com.sni.bokaticowork.features.billing.dto.response.DealDashboardResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDealStage;
import com.sni.bokaticowork.features.billing.enums.BillingDepositStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDeal;
import com.sni.bokaticowork.features.billing.model.BillingDeposit;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.model.ServiceCatalogItem;
import com.sni.bokaticowork.features.billing.repository.BillingDealRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDepositRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentLineRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lot B11 : une affaire avec un devis refuse, un devis accepte, un acompte et une facture payee en
 * partie affiche les bons totaux ; les pieces derivees rejoignent l'affaire de leur piece d'origine.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DealB11Test {

    @Mock private BillingDealRepository deals;
    @Mock private BillingDocumentRepository documents;
    @Mock private BillingDocumentLineRepository lines;
    @Mock private BillingDepositRepository deposits;
    @Mock private ServiceCatalogItemRepository catalog;
    @Mock private SequenceGeneratorFacade sequences;
    @Mock private StaffProfileService staffProfiles;

    @InjectMocks
    private DealService service;

    private final BillingDeal deal = BillingDeal.builder().code("AFF-1").customerType("CUSTOMER").customerCode("CUS-1").customerName("Société")
            .title("Aménagement du plateau").stage(BillingDealStage.NEGOTIATION).source("MANUAL").createdAt(Instant.now()).build();

    @BeforeEach
    void setUp() {
        when(deals.findByCode("AFF-1")).thenReturn(Optional.of(deal));
        when(deals.save(any(BillingDeal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(documents.save(any(BillingDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(staffProfiles.currentAuthor()).thenReturn(new StaffProfileService.Author("awa@elleaose.com", "Awa Mbemba", "Commerciale"));
        when(sequences.next("billing_deal")).thenReturn("AFF-2");
        when(deposits.findByFinalDocumentNumberOrderByRankAsc(anyString())).thenReturn(List.of());
    }

    @Test
    void shouldShowTheRightTotals() {
        BillingDocument refused = doc("DEV-1", BillingDocumentType.QUOTE, BillingDocumentStatus.REJECTED, "500000", "0", "0", null);
        BillingDocument acceptedQuote = doc("DEV-2", BillingDocumentType.QUOTE, BillingDocumentStatus.CONVERTED, "1000000", "0", "0", null);
        BillingDocument deposit = doc("FAC-A1", BillingDocumentType.INVOICE, BillingDocumentStatus.PAID, "300000", "300000", "0", "DEV-2");
        BillingDocument invoice = doc("FAC-9", BillingDocumentType.INVOICE, BillingDocumentStatus.PARTIALLY_PAID, "1000000", "500000", "500000", null);
        when(documents.findByDealCodeOrderByCreatedAtAsc("AFF-1")).thenReturn(List.of(refused, acceptedQuote, deposit, invoice));
        when(deposits.findByFinalDocumentNumberOrderByRankAsc("FAC-9")).thenReturn(List.of(BillingDeposit.builder()
                .status(BillingDepositStatus.DEDUCTED).deductedAmount(new BigDecimal("300000")).build()));
        // Marge : un bureau vendu 1 000 000 HT, coute 600 000.
        when(lines.findAllByDocumentOrderByLineOrderAscIdAsc(any())).thenReturn(List.of(BillingDocumentLine.builder().itemCode("BUR")
                .quantity(BigDecimal.ONE).subtotalAmount(new BigDecimal("1000000")).discountAmount(BigDecimal.ZERO).build()));
        when(catalog.findByItemCode("BUR")).thenReturn(Optional.of(ServiceCatalogItem.builder().costPrice(new BigDecimal("600000")).build()));

        DealDashboardResponse dashboard = service.dashboard("AFF-1");

        assertThat(dashboard.quoted()).isEqualByComparingTo("1500000");
        assertThat(dashboard.accepted()).isEqualByComparingTo("1000000");
        assertThat(dashboard.invoiced()).isEqualByComparingTo("1000000");
        assertThat(dashboard.depositsInvoiced()).isEqualByComparingTo("300000");
        assertThat(dashboard.depositsPaid()).isEqualByComparingTo("300000");
        // 300 000 d'acompte + 500 000 sur la facture, dont 300 000 sont l'acompte deduit.
        assertThat(dashboard.collected()).isEqualByComparingTo("500000");
        assertThat(dashboard.remainingToInvoice()).isEqualByComparingTo("0");
        assertThat(dashboard.remainingToCollect()).isEqualByComparingTo("500000");
        assertThat(dashboard.plannedMargin()).isEqualByComparingTo("400000");
        assertThat(dashboard.realizedMargin()).isEqualByComparingTo("400000");
        assertThat(dashboard.costedShare()).isEqualByComparingTo("100");
        assertThat(dashboard.documents()).hasSize(4);
    }

    @Test
    void shouldAttachADerivedDocumentToTheDealOfItsOrigin() {
        when(documents.findByDocumentNumber("DEV-2")).thenReturn(Optional.of(BillingDocument.builder().documentNumber("DEV-2").dealCode("AFF-1").build()));
        BillingDocument invoice = BillingDocument.builder().documentNumber("FAC-9").documentType(BillingDocumentType.INVOICE).sourceType("QUOTE")
                .sourceCode("DEV-2").customerType("CUSTOMER").customerCode("CUS-1").build();

        service.onDocumentCreated(invoice);

        assertThat(invoice.getDealCode()).isEqualTo("AFF-1");
    }

    @Test
    void shouldOpenADealOnTheFirstQuoteOfACustomer() {
        when(deals.findByCustomerTypeAndCustomerCodeAndStageInOrderByCreatedAtDesc(any(), any(), any())).thenReturn(List.of());
        BillingDocument quote = BillingDocument.builder().documentNumber("DEV-5").documentType(BillingDocumentType.QUOTE).customerType("CUSTOMER")
                .customerCode("CUS-1").customerName("Société").title("Bureaux").totalAmount(new BigDecimal("800000")).build();

        service.onDocumentCreated(quote);

        assertThat(quote.getDealCode()).isEqualTo("AFF-2");
    }

    @Test
    void shouldJoinTheOnlyOpenDealOfTheCustomer() {
        when(deals.findByCustomerTypeAndCustomerCodeAndStageInOrderByCreatedAtDesc(any(), any(), any())).thenReturn(List.of(deal));
        BillingDocument quote = BillingDocument.builder().documentNumber("DEV-6").documentType(BillingDocumentType.QUOTE).customerType("CUSTOMER")
                .customerCode("CUS-1").build();

        service.onDocumentCreated(quote);

        assertThat(quote.getDealCode()).isEqualTo("AFF-1");
        verify(sequences, never()).next("billing_deal");
    }

    @Test
    void shouldRequireAReasonToLoseADeal() {
        assertThatThrownBy(() -> service.stage("AFF-1", new DealStageRequest(BillingDealStage.LOST, null))).isInstanceOf(BadRequestException.class);

        service.stage("AFF-1", new DealStageRequest(BillingDealStage.LOST, "Budget reporté"));

        assertThat(deal.getStage()).isEqualTo(BillingDealStage.LOST);
        assertThat(deal.getClosedAt()).isNotNull();
    }

    private static BillingDocument doc(String number, BillingDocumentType type, BillingDocumentStatus status, String total, String paid,
                                       String balance, String depositFor) {
        return BillingDocument.builder().documentNumber(number).documentType(type).status(status).totalAmount(new BigDecimal(total))
                .paidAmount(new BigDecimal(paid)).balanceDue(new BigDecimal(balance)).depositForDocumentNumber(depositFor).currency("XAF")
                .createdAt(Instant.now()).build();
    }
}
