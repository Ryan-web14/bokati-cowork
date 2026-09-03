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

import com.sni.bokaticowork.core.exception.customs.BadRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    // ─────────────────────────────────────────────────────────────────────────
    // La remise globale porte sur le TTC · c'est ce qu'attend celui qui l'accorde.
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void shouldReduceTheTotalByExactlyTheDiscountGranted() {
        // Le defaut corrige : une remise de 100 retirait 118,90 au client, parce qu'elle etait
        // deduite du HT et emportait donc aussi la TVA et les centimes. Personne n'avait demande
        // cette baisse supplementaire.
        var result = service.calculate(
                List.of(line("Prestation", "1000", true)),
                List.of(new CreateBillingDocumentDiscountRequest(
                        "GESTE", "Geste commercial", BillingDiscountType.FIXED_AMOUNT, new BigDecimal("100"))));

        var sansRemise = service.calculate(List.of(line("Prestation", "1000", true)), List.of());

        assertThat(sansRemise.totalAmount()).isEqualByComparingTo("1189");
        assertThat(result.totalAmount()).isEqualByComparingTo("1089");
        assertThat(sansRemise.totalAmount().subtract(result.totalAmount()))
                .as("le client paie exactement 100 de moins")
                .isEqualByComparingTo("100");
    }

    @Test
    void shouldKeepTheComponentsAddingUpToTheDiscountedTotal() {
        // Une facture dont les composantes ne redonnent pas le total ne se ventile pas en
        // comptabilite · l'ecart d'arrondi est reporte sur la base taxable.
        var result = service.calculate(
                List.of(line("Prestation", "1000", true), line("Debours", "500", false)),
                List.of(new CreateBillingDocumentDiscountRequest(
                        "GESTE", "Geste", BillingDiscountType.FIXED_AMOUNT, new BigDecimal("333"))));

        BigDecimal somme = result.taxableAmount()
                .add(result.totalAmount().subtract(result.taxableAmount()).subtract(result.taxAmount()))
                .add(result.taxAmount());
        assertThat(somme).isEqualByComparingTo(result.totalAmount());
        assertThat(result.totalAmount()).isEqualByComparingTo("1356");
    }

    @Test
    void shouldApplyEveryPercentageToTheSameBase() {
        // Additif, non cumulatif : 10 % puis 5 % retirent 15 %, et l'ordre n'a aucune incidence.
        var result = service.calculate(
                List.of(line("Prestation", "1000", true)),
                List.of(new CreateBillingDocumentDiscountRequest("A", "Remise A", BillingDiscountType.PERCENTAGE, new BigDecimal("10")),
                        new CreateBillingDocumentDiscountRequest("B", "Remise B", BillingDiscountType.PERCENTAGE, new BigDecimal("5"))));

        // 15 % de 1189 = 178.35
        assertThat(result.totalAmount()).isEqualByComparingTo("1010.6500");
        assertThat(result.discountAmounts()).hasSize(2);
        assertThat(result.discountAmounts().get(0)).isEqualByComparingTo("118.9000");
        assertThat(result.discountAmounts().get(1)).isEqualByComparingTo("59.4500");
    }

    @Test
    void shouldRefuseADiscountLargerThanTheDocument() {
        // Le montant etait ramene en silence a la base : 140 % devenaient 100 %, la facture
        // ressortait a zero, et le garde-fou d'emission annoncait « 100 % » sans jamais voir la
        // demande reelle.
        assertThatThrownBy(() -> service.calculate(
                List.of(line("Prestation", "1000", true)),
                List.of(new CreateBillingDocumentDiscountRequest("A", "A", BillingDiscountType.PERCENTAGE, new BigDecimal("80")),
                        new CreateBillingDocumentDiscountRequest("B", "B", BillingDiscountType.PERCENTAGE, new BigDecimal("60")))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("dépassent le total")
                .hasMessageContaining("100 %");
    }

    @Test
    void shouldAcceptADiscountOfExactlyTheWholeDocument() {
        // Cent pour cent reste permis · c'est cent un qui ne l'est pas.
        var result = service.calculate(
                List.of(line("Prestation", "1000", true)),
                List.of(new CreateBillingDocumentDiscountRequest(
                        "TOTAL", "Offert", BillingDiscountType.PERCENTAGE, new BigDecimal("100"))));

        assertThat(result.totalAmount()).isEqualByComparingTo("0");
    }

    @Test
    void shouldReportOneAmountPerDiscountSummingToTheTotalGranted() {
        // Le detail etait recalcule a l'ecriture sur une autre base · 10 701 stockes pour 10 000
        // deduits. Il vient desormais du calcul lui-meme.
        var result = service.calculate(
                List.of(line("Prestation", "1000", true)),
                List.of(new CreateBillingDocumentDiscountRequest("A", "A", BillingDiscountType.PERCENTAGE, new BigDecimal("10")),
                        new CreateBillingDocumentDiscountRequest("B", "B", BillingDiscountType.FIXED_AMOUNT, new BigDecimal("50"))));

        BigDecimal somme = result.discountAmounts().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(somme).isEqualByComparingTo("168.9000");
        assertThat(result.totalAmount()).isEqualByComparingTo("1020.1000");
    }
}
