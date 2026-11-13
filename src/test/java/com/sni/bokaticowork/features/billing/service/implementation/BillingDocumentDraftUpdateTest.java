package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentClauseRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentDiscountRequest;
import com.sni.bokaticowork.features.billing.dto.request.UpdateBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.enums.BillingDiscountType;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentDiscount;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentAdvanceRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentClauseRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentDiscountRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentEarlyPaymentDiscountRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentLineRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentTaxRepository;
import com.sni.bokaticowork.features.billing.service.support.BillingCalculationService;
import com.sni.bokaticowork.features.billing.service.support.BillingDocumentWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La modification d'un brouillon remplace remises, taxes et clauses sans dupliquer lignes ni taxes.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillingDocumentDraftUpdateTest {

    @Mock private BillingDocumentRepository documentRepository;
    @Mock private BillingDocumentLineRepository lineRepository;
    @Mock private BillingDocumentDiscountRepository discountRepository;
    @Mock private BillingDocumentTaxRepository taxRepository;
    @Mock private BillingDocumentClauseRepository clauseRepository;
    @Mock private BillingCalculationService calculationService;
    @Mock private com.sni.bokaticowork.features.billing.service.support.BillingLifecycleSupport lifecycleSupport;
    @Mock private com.sni.bokaticowork.features.billing.service.support.BillingDocumentVersionService versionService;
    @Mock private com.sni.bokaticowork.features.billing.service.support.BillingEventWriter eventWriter;
    @Mock private com.sni.bokaticowork.features.billing.mapper.interfaces.BillingDocumentMapper mapper;
    @Mock private com.sni.bokaticowork.features.billing.service.support.BillingCustomerSnapshotResolver customerSnapshotResolver;

    @InjectMocks
    private BillingDocumentServiceImpl service;

    private BillingDocument document;

    @BeforeEach
    void setUp() {
        BillingDocumentWriter writer = new BillingDocumentWriter(documentRepository, lineRepository, discountRepository,
                taxRepository, clauseRepository, mock(BillingDocumentAdvanceRepository.class),
                mock(BillingDocumentEarlyPaymentDiscountRepository.class));
        ReflectionTestUtils.setField(service, "documentWriter", writer);

        document = BillingDocument.builder()
                .documentNumber("INV-100")
                .documentType(BillingDocumentType.INVOICE)
                .status(BillingDocumentStatus.DRAFT)
                .locked(false)
                .paidAmount(BigDecimal.ZERO)
                .currency("XAF")
                .build();
        BillingDocumentLine line = BillingDocumentLine.builder()
                .document(document)
                .lineOrder(1)
                .lineType(BillingLineType.SERVICE)
                .description("Bureau")
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("100000"))
                .subtotalAmount(new BigDecimal("100000"))
                .discountRate(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .taxable(true)
                .build();
        when(documentRepository.findByDocumentNumber("INV-100")).thenReturn(Optional.of(document));
        when(customerSnapshotResolver.organizationOf(any(), any())).thenReturn(Optional.empty());
        when(documentRepository.save(any(BillingDocument.class))).thenAnswer(i -> i.getArgument(0));
        when(lineRepository.findAllByDocumentOrderByLineOrderAscIdAsc(document)).thenReturn(List.of(line));

        // Le calcul rend une ligne neuve, comme le vrai service · c'est elle qui etait reenregistree
        BillingDocumentLine recalculated = BillingDocumentLine.builder().lineOrder(1).build();
        when(calculationService.calculate(anyList(), anyList())).thenReturn(new BillingCalculationService.CalculatedDocument(
                List.of(recalculated), new BigDecimal("100000"), new BigDecimal("10000"), List.of(new BigDecimal("10000")),
                new BigDecimal("90000"), new BigDecimal("16200"), new BigDecimal("810"), new BigDecimal("17010"),
                new BigDecimal("107010")));
    }

    @Test
    void shouldReplaceDiscountsAndTaxesWithoutDuplicatingLines() {
        service.update("INV-100", request(
                List.of(new CreateBillingDocumentDiscountRequest(null, "Remise fidelite", BillingDiscountType.PERCENTAGE, BigDecimal.TEN)),
                List.of(new CreateBillingDocumentClauseRequest("PAY", "Paiement", "A 30 jours", 1))));

        verify(lineRepository, never()).save(any(BillingDocumentLine.class));
        InOrder taxes = inOrder(taxRepository);
        taxes.verify(taxRepository).deleteAllByDocument(document);
        // TVA et centime additionnel, une seule fois chacun
        taxes.verify(taxRepository, times(2)).save(any());
        verify(discountRepository, times(1)).save(any(BillingDocumentDiscount.class));
        verify(clauseRepository, times(1)).save(any());
        assertThat(document.getTotalAmount()).isEqualByComparingTo("107010");
    }

    @Test
    void shouldKeepStoredDiscountsInTheTotalWhenTheRequestCarriesNone() {
        BillingDocumentDiscount stored = BillingDocumentDiscount.builder()
                .document(document)
                .description("Remise fidelite")
                .discountType(BillingDiscountType.PERCENTAGE)
                .value(BigDecimal.TEN)
                .amount(BigDecimal.ZERO)
                .build();
        when(discountRepository.findAllByDocumentOrderByIdAsc(document)).thenReturn(List.of(stored));

        service.update("INV-100", request(null, null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CreateBillingDocumentDiscountRequest>> discounts = ArgumentCaptor.forClass(List.class);
        verify(calculationService).calculate(anyList(), discounts.capture());
        assertThat(discounts.getValue()).hasSize(1);
        assertThat(discounts.getValue().get(0).value()).isEqualByComparingTo("10");
        // Le montant enregistre de la remise suit le recalcul
        assertThat(stored.getAmount()).isEqualByComparingTo("10000");
        verify(lineRepository, never()).save(any(BillingDocumentLine.class));
    }

    @Test
    void shouldKeepAnExemptOrganizationOutOfVatWhenTheDraftIsRecalculated() {
        // Lot B0 : l'exoneration n'etait appliquee qu'a la creation ; une ligne ajoutee a la mise a
        // jour d'une facture d'organisation exoneree prenait la TVA.
        var terms = mock(com.sni.bokaticowork.features.company.service.support.OrganizationBillingProfile.Terms.class);
        when(terms.vatExempt()).thenReturn(true);
        when(customerSnapshotResolver.organizationOf(any(), any())).thenReturn(Optional.of(terms));
        when(calculationService.exempted(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        service.update("INV-100", request(null, null));

        verify(calculationService).exempted(anyList());
    }

    @Test
    void shouldNotExemptAnOrdinaryCustomer() {
        service.update("INV-100", request(null, null));

        verify(calculationService, never()).exempted(anyList());
    }

    private static UpdateBillingDocumentRequest request(List<CreateBillingDocumentDiscountRequest> discounts,
                                                        List<CreateBillingDocumentClauseRequest> clauses) {
        return new UpdateBillingDocumentRequest(null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, discounts, clauses);
    }
}
