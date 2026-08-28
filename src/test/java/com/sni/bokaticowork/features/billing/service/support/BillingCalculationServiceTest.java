package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentDiscountRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentLineRequest;
import com.sni.bokaticowork.features.billing.enums.BillingDiscountType;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BillingCalculationServiceTest {

    private static final BigDecimal VAT = new BigDecimal("18");
    private static final BigDecimal ADDITIONAL_CENT = new BigDecimal("5");

    private final BillingTaxRuleResolver taxRuleResolver = mock(BillingTaxRuleResolver.class);
    private final ServiceCatalogItemRepository catalogItemRepository = mock(ServiceCatalogItemRepository.class);

    private final BillingCalculationService service =
            new BillingCalculationService(taxRuleResolver, catalogItemRepository);

    @BeforeEach
    void setUp() {
        when(taxRuleResolver.defaultTaxProfile())
                .thenReturn(new BillingTaxRuleResolver.TaxProfile(VAT, ADDITIONAL_CENT));
        when(catalogItemRepository.findByItemCode(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void shouldCountANonTaxableLineInTheDocumentTotal() {
        // Regression : le total se calculait depuis la base taxable, or calculateTaxExcludedAmounts
        // force taxableAmount a zero sur une ligne exoneree. Un avoir - dont la ligne d'ajustement
        // est toujours taxable=false - ressortait donc a 0,00 et son application levait
        // "Payment amount must be positive", bloquant l'annulation d'une facture scellee.
        var result = service.calculate(List.of(line("Avoir", "1000", false)), List.of());

        assertThat(result.subtotalAmount()).isEqualByComparingTo("1000");
        assertThat(result.taxAmount()).isEqualByComparingTo("0");
        assertThat(result.totalAmount()).isEqualByComparingTo("1000");
    }

    @Test
    void shouldKeepTaxingATaxableLineTheSameWayAsBefore() {
        var result = service.calculate(List.of(line("Prestation", "1000", true)), List.of());

        // 1000 HT · TVA 18 % = 180 · centimes additionnels 5 % de la TVA = 9
        assertThat(result.taxableAmount()).isEqualByComparingTo("1000");
        assertThat(result.vatAmount()).isEqualByComparingTo("180");
        assertThat(result.additionalCentAmount()).isEqualByComparingTo("9");
        assertThat(result.totalAmount()).isEqualByComparingTo("1189");
    }

    @Test
    void shouldAddTaxableAndExemptLinesTogether() {
        var result = service.calculate(
                List.of(line("Prestation", "1000", true), line("Debours exonere", "500", false)),
                List.of());

        assertThat(result.subtotalAmount()).isEqualByComparingTo("1500");
        // Seule la part taxable porte la taxe · la part exoneree entre au total en HT.
        assertThat(result.taxableAmount()).isEqualByComparingTo("1000");
        assertThat(result.taxAmount()).isEqualByComparingTo("189");
        assertThat(result.totalAmount()).isEqualByComparingTo("1689");
    }

    @Test
    void shouldSplitADocumentDiscountBetweenTheTaxableAndExemptShares() {
        // 10 % sur un net HT de 1500 = 150, reparti 100 sur le taxable / 50 sur l'exonere.
        var result = service.calculate(
                List.of(line("Prestation", "1000", true), line("Debours exonere", "500", false)),
                List.of(new CreateBillingDocumentDiscountRequest(
                        "REMISE_COM", "Remise commerciale", BillingDiscountType.PERCENTAGE, new BigDecimal("10"))));

        assertThat(result.discountAmount()).isEqualByComparingTo("150");
        assertThat(result.taxableAmount()).isEqualByComparingTo("900");
        // Les taxes suivent la seule assiette taxable : 900 * 18 % = 162, + 5 % = 8.10
        assertThat(result.vatAmount()).isEqualByComparingTo("162");
        assertThat(result.additionalCentAmount()).isEqualByComparingTo("8.1000");
        // 900 taxable + 450 exonere + 170.10 de taxes
        assertThat(result.totalAmount()).isEqualByComparingTo("1520.1000");
    }

    @Test
    void shouldExcludeOptionalLinesFromTheDocumentTotal() {
        CreateBillingDocumentLineRequest optional = new CreateBillingDocumentLineRequest(
                null, BillingLineType.SERVICE, null, "Option", null, BigDecimal.ONE, new BigDecimal("400"),
                null, null, false, false, null, null, null, null, null, null, null, Boolean.TRUE, null);

        var result = service.calculate(List.of(line("Avoir", "1000", false), optional), List.of());

        assertThat(result.lines()).hasSize(2);
        assertThat(result.totalAmount()).isEqualByComparingTo("1000");
    }

    private CreateBillingDocumentLineRequest line(String description, String unitPrice, boolean taxable) {
        return new CreateBillingDocumentLineRequest(
                null, BillingLineType.SERVICE, null, description, null,
                BigDecimal.ONE, new BigDecimal(unitPrice),
                BigDecimal.ZERO, BigDecimal.ZERO, taxable, false,
                null, null, null, null, null, null, null, null, null);
    }
}
