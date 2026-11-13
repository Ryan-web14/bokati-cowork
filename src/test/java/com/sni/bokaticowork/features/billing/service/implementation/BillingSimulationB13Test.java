package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentLineRequest;
import com.sni.bokaticowork.features.billing.dto.request.SimulateBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.request.SimulateBillingLineRequest;
import com.sni.bokaticowork.features.billing.dto.response.SimulateBillingDocumentResponse;
import com.sni.bokaticowork.features.billing.dto.response.SimulateBillingLineResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import com.sni.bokaticowork.features.billing.service.support.BillingCalculationService;
import com.sni.bokaticowork.features.billing.service.support.BillingCustomerSnapshotResolver;
import com.sni.bokaticowork.features.billing.service.support.BillingDiscountGuard;
import com.sni.bokaticowork.features.billing.config.BillingDiscountGuardProperties;
import com.sni.bokaticowork.features.billing.service.support.BillingStockGuard;
import com.sni.bokaticowork.features.company.service.support.OrganizationBillingProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot B13 : la simulation fait le meme calcul que la creation, ligne par ligne, avec ses alertes. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillingSimulationB13Test {

    @Mock private BillingCalculationService calculationService;
    @Mock private BillingDocumentRepository documentRepository;
    @Mock private BillingCustomerSnapshotResolver customerSnapshotResolver;
    @Mock private BillingStockGuard stockGuard;
    @Spy private BillingDiscountGuard discountGuard = new BillingDiscountGuard(mock(ServiceCatalogItemRepository.class),
            new BillingDiscountGuardProperties());

    @InjectMocks
    private BillingDocumentServiceImpl service;

    @Test
    void shouldApplyTheVatExemptionOfTheCustomerLikeTheCreation() {
        // Ecart E17 : la simulation ignorait l'exoneration ; le total affiche n'etait pas celui facture.
        OrganizationBillingProfile.Terms terms = new OrganizationBillingProfile.Terms("CUS-1", "Ambassade", null, null, true, "Exonération",
                null, null, false, null, null, null);
        when(customerSnapshotResolver.organizationOf("CUSTOMER", "CUS-1")).thenReturn(Optional.of(terms));
        when(calculationService.exempted(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        calculated(line("Bureau", "100000"));

        SimulateBillingDocumentResponse simulation = service.simulate(new SimulateBillingDocumentRequest(BillingDocumentType.INVOICE, "XAF", null,
                List.of(request("Bureau", "100000")), null, "CUSTOMER", "CUS-1", null));

        verify(calculationService).exempted(anyList());
        assertThat(simulation.alerts()).extracting(SimulateBillingDocumentResponse.Alert::code).contains("VAT_EXEMPT");
        assertThat(simulation.blocked()).isFalse();
    }

    @Test
    void shouldAnnounceTheCreditLimitWithoutRefusing() {
        OrganizationBillingProfile.Terms terms = new OrganizationBillingProfile.Terms("CUS-2", "Société", null, null, false, null,
                null, new BigDecimal("150000"), false, null, null, null);
        when(customerSnapshotResolver.organizationOf("CUSTOMER", "CUS-2")).thenReturn(Optional.of(terms));
        when(documentRepository.sumCompanyOutstanding(eq("CUS-2"), any(), any())).thenReturn(new BigDecimal("100000"));
        calculated(line("Bureau", "100000"));

        SimulateBillingDocumentResponse simulation = service.simulate(new SimulateBillingDocumentRequest(BillingDocumentType.INVOICE, "XAF", null,
                List.of(request("Bureau", "100000")), null, "CUSTOMER", "CUS-2", null));

        assertThat(simulation.alerts()).extracting(SimulateBillingDocumentResponse.Alert::code).contains("CREDIT_LIMIT");
        assertThat(simulation.blocked()).isTrue();
    }

    @Test
    void shouldReplaceTheTypedLineAndReturnItWithTheTotals() {
        calculated(line("Bureau", "100000"), line("Salle", "30000"));
        when(stockGuard.evaluate(any(), anyList())).thenReturn(List.of(new BillingStockGuard.StockAlert(1, "ART-1", "Salle",
                new BigDecimal("3"), BigDecimal.ONE, "WARN", "Stock insuffisant")));

        SimulateBillingLineResponse response = service.simulateLine(new SimulateBillingLineRequest(1, request("Salle", "30000"),
                BillingDocumentType.QUOTE, "XAF", List.of(request("Bureau", "100000"), request("Ancienne saisie", "1")), null, null, null, null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CreateBillingDocumentLineRequest>> lines = ArgumentCaptor.forClass(List.class);
        verify(calculationService).calculate(lines.capture(), any());
        assertThat(lines.getValue()).extracting(CreateBillingDocumentLineRequest::description).containsExactly("Bureau", "Salle");
        assertThat(response.lineIndex()).isEqualTo(1);
        assertThat(response.line().description()).isEqualTo("Salle");
        assertThat(response.alerts()).extracting(SimulateBillingDocumentResponse.Alert::code).containsExactly("STOCK");
        assertThat(response.document().totalAmount()).isEqualByComparingTo("130000");
    }

    @Test
    void shouldAppendANewLine() {
        calculated(line("Bureau", "100000"), line("Café", "30000"));

        SimulateBillingLineResponse response = service.simulateLine(new SimulateBillingLineRequest(null, request("Café", "30000"),
                null, null, List.of(request("Bureau", "100000")), null, null, null, null));

        assertThat(response.lineIndex()).isEqualTo(1);
        assertThat(response.line().description()).isEqualTo("Café");
    }

    private void calculated(BillingDocumentLine... lines) {
        BigDecimal total = new ArrayList<>(List.of(lines)).stream().map(BillingDocumentLine::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        when(calculationService.calculate(anyList(), any())).thenReturn(new BillingCalculationService.CalculatedDocument(List.of(lines), total,
                BigDecimal.ZERO, List.of(), total, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, total));
    }

    private static BillingDocumentLine line(String description, String amount) {
        return BillingDocumentLine.builder().description(description).quantity(BigDecimal.ONE).unitPrice(new BigDecimal(amount))
                .subtotalAmount(new BigDecimal(amount)).discountAmount(BigDecimal.ZERO).taxAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal(amount)).build();
    }

    private static CreateBillingDocumentLineRequest request(String description, String amount) {
        return new CreateBillingDocumentLineRequest(null, BillingLineType.SERVICE, null, description, null, BigDecimal.ONE, new BigDecimal(amount),
                null, null, null, null, null, null, null, null, null, null, null, false, null);
    }
}
