package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentLineRequest;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.billing.enums.TaxTreatment;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.model.BillingDocumentTax;
import com.sni.bokaticowork.features.billing.model.ServiceCatalogItem;
import com.sni.bokaticowork.features.billing.model.TaxRule;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Lot C1 : la regle de taxe de l'article pilote la ligne. Un article exonere produit une ligne
 * sans TVA ni centimes, meme si l'ecran envoie taxable=true ; un taux reduit s'applique ; un taux
 * fixe par l'appelant reste prioritaire ; plusieurs taux se ventilent exactement.
 */
class BillingItemTaxC1Test {

    private final BillingTaxRuleResolver resolver = mock(BillingTaxRuleResolver.class);
    private final ServiceCatalogItemRepository items = mock(ServiceCatalogItemRepository.class);
    private final BillingCalculationService service = new BillingCalculationService(resolver, items);

    private final TaxRule exempt = TaxRule.builder().code("TVA_CG_EXONERE").baseCode("TVA_CG_EXONERE").name("Exonéré").taxType("VAT")
            .rate(BigDecimal.ZERO).treatment(TaxTreatment.EXEMPT).labelOnDocument("Exonéré de TVA").exemptionReason("Article 7").active(true).build();
    private final TaxRule reduced = TaxRule.builder().code("TVA_CG_5").baseCode("TVA_CG_5").name("TVA 5 %").taxType("VAT")
            .rate(new BigDecimal("5")).treatment(TaxTreatment.REDUCED).active(true).build();

    @BeforeEach
    void setUp() {
        when(resolver.defaultTaxProfile()).thenReturn(new BillingTaxRuleResolver.TaxProfile(new BigDecimal("18"), new BigDecimal("5")));
        when(items.findByItemCode(anyString())).thenReturn(Optional.empty());
        when(resolver.ruleFor(eq("TVA_CG_EXONERE"), any(LocalDate.class))).thenReturn(Optional.of(exempt));
        when(resolver.ruleFor(eq("TVA_CG_5"), any(LocalDate.class))).thenReturn(Optional.of(reduced));
        item("SRV-DOMI", "TVA_CG_EXONERE");
        item("SRV-LIVRE", "TVA_CG_5");
    }

    @Test
    void shouldNotTaxALineOfAnExemptItemEvenWhenTheScreenSendsTaxable() {
        var result = service.calculate(List.of(line("SRV-DOMI", "100000", true, null)), List.of());

        BillingDocumentLine line = result.lines().get(0);
        assertThat(line.getTaxable()).isFalse();
        assertThat(line.getVatAmount()).isEqualByComparingTo("0");
        assertThat(line.getAdditionalCentAmount()).isEqualByComparingTo("0");
        assertThat(line.getTaxRuleCode()).isEqualTo("TVA_CG_EXONERE");
        assertThat(result.totalAmount()).isEqualByComparingTo("100000");
    }

    @Test
    void shouldApplyTheReducedRateOfTheItem() {
        var result = service.calculate(List.of(line("SRV-LIVRE", "10000", null, null)), List.of());

        BillingDocumentLine line = result.lines().get(0);
        assertThat(line.getVatRate()).isEqualByComparingTo("5");
        assertThat(line.getVatAmount()).isEqualByComparingTo("500");
        assertThat(line.getTaxRuleCode()).isEqualTo("TVA_CG_5");
    }

    @Test
    void shouldKeepARateFixedByTheCallerAndDropTheItemMention() {
        var result = service.calculate(List.of(line("SRV-DOMI", "100000", true, new BigDecimal("18"))), List.of());

        BillingDocumentLine line = result.lines().get(0);
        assertThat(line.getTaxable()).isTrue();
        assertThat(line.getVatAmount()).isEqualByComparingTo("18000");
        assertThat(line.getTaxRuleCode()).isNull();
    }

    @Test
    void shouldSplitTheVatByRateExactly() {
        var result = service.calculate(List.of(
                line(null, "100000", true, null),
                line("SRV-LIVRE", "33333", true, null),
                line("SRV-DOMI", "50000", true, null)), List.of());

        List<BillingDocumentTax> rows = BillingDocumentWriter.vatByRate(new BillingDocument(), result);

        assertThat(rows).extracting(BillingDocumentTax::getTaxName).containsExactly("TVA 5 %", "TVA 18 %");
        assertThat(rows.stream().map(BillingDocumentTax::getTaxAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(result.vatAmount());
        assertThat(rows.stream().map(BillingDocumentTax::getTaxableAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(result.taxableAmount());
        // La ligne exoneree n'entre dans aucune base de TVA.
        assertThat(result.taxableAmount()).isEqualByComparingTo("133333");
    }

    @Test
    void shouldKeepASingleVatRowWhenTheDocumentHasOneRate() {
        var result = service.calculate(List.of(line(null, "1000", true, null), line(null, "2000", true, null)), List.of());

        assertThat(BillingDocumentWriter.vatByRate(new BillingDocument(), result)).isEmpty();
    }

    private void item(String code, String taxRuleCode) {
        when(items.findByItemCode(code)).thenReturn(Optional.of(ServiceCatalogItem.builder().itemCode(code).name(code)
                .unitPrice(BigDecimal.ONE).taxRuleCode(taxRuleCode).build()));
    }

    private static CreateBillingDocumentLineRequest line(String itemCode, String unitPrice, Boolean taxable, BigDecimal vatRate) {
        return new CreateBillingDocumentLineRequest(
                null, BillingLineType.SERVICE, itemCode, "Ligne " + unitPrice, null,
                BigDecimal.ONE, new BigDecimal(unitPrice),
                BigDecimal.ZERO, BigDecimal.ZERO, taxable, false,
                vatRate, null, null, null, null, null, null, null, null);
    }
}
