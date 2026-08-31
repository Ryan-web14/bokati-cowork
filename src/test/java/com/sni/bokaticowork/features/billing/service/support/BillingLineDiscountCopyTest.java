package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentLineRequest;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Une ligne recopiee doit produire exactement la meme remise.
 *
 * <p>C'est le defaut rapporte en production : apres conversion d'un devis remise en facture, la
 * remise avait double et le total ne correspondait plus. Le moteur additionne la remise en taux
 * et celle en montant, puis conserve les deux sur la ligne · recopier le couple faisait
 * recalculer le taux par-dessus un montant qui le contenait deja.
 *
 * <p>Le test rejoue la recopie telle que la font la conversion, la duplication et la
 * modification, en reproduisant le calcul du residu applique par BillingDocumentServiceImpl.
 */
class BillingLineDiscountCopyTest {

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
    void shouldKeepTheSameDiscountWhenALineExpressedAsARateIsCopied() {
        // 100 000 remises de 10 % · le devis stocke taux = 10 ET montant = 10 000.
        var quote = service.calculate(List.of(line("10", null)), List.of());
        BillingDocumentLine quoteLine = quote.lines().getFirst();
        assertThat(quoteLine.getDiscountAmount()).isEqualByComparingTo("10000");

        var invoice = service.calculate(List.of(copyOf(quoteLine)), List.of());

        // Sans la correction, la remise ressortait a 20 000 et le total baissait d'autant.
        assertThat(invoice.lines().getFirst().getDiscountAmount()).isEqualByComparingTo("10000");
        assertThat(invoice.totalAmount()).isEqualByComparingTo(quote.totalAmount());
    }

    @Test
    void shouldStayStableAcrossSeveralSuccessiveCopies() {
        // La modification recopie aussi les lignes · la remise se cumulait a chaque
        // enregistrement, pas seulement a la conversion.
        var current = service.calculate(List.of(line("10", null)), List.of());
        BigDecimal expected = current.totalAmount();

        for (int i = 0; i < 4; i++) {
            current = service.calculate(List.of(copyOf(current.lines().getFirst())), List.of());
        }

        assertThat(current.lines().getFirst().getDiscountAmount()).isEqualByComparingTo("10000");
        assertThat(current.totalAmount()).isEqualByComparingTo(expected);
    }

    @Test
    void shouldKeepADiscountExpressedAsAnAmount() {
        // Sans taux, le montant est deja le residu · rien ne doit etre retranche.
        var quote = service.calculate(List.of(line(null, "7500")), List.of());

        var invoice = service.calculate(List.of(copyOf(quote.lines().getFirst())), List.of());

        assertThat(invoice.lines().getFirst().getDiscountAmount()).isEqualByComparingTo("7500");
        assertThat(invoice.totalAmount()).isEqualByComparingTo(quote.totalAmount());
    }

    @Test
    void shouldPreserveAMixedDiscountRateAndFixedAmount() {
        // Le moteur admet les deux a la fois · 10 % plus 5 000, soit 15 000 au total. La recopie
        // doit conserver la part fixe, que le taux seul ne reproduirait pas.
        var quote = service.calculate(List.of(line("10", "5000")), List.of());
        assertThat(quote.lines().getFirst().getDiscountAmount()).isEqualByComparingTo("15000");

        var invoice = service.calculate(List.of(copyOf(quote.lines().getFirst())), List.of());

        assertThat(invoice.lines().getFirst().getDiscountAmount()).isEqualByComparingTo("15000");
    }

    @Test
    void shouldKeepTheRateVisibleOnTheCopiedLine() {
        // Le taux reste recopie tel quel · il s'affiche sur le document, seul le montant est
        // ramene a son residu.
        var quote = service.calculate(List.of(line("10", null)), List.of());

        var invoice = service.calculate(List.of(copyOf(quote.lines().getFirst())), List.of());

        assertThat(invoice.lines().getFirst().getDiscountRate()).isEqualByComparingTo("10");
    }

    // =================================================================================

    private CreateBillingDocumentLineRequest line(String discountRate, String discountAmount) {
        return new CreateBillingDocumentLineRequest(
                null, BillingLineType.SERVICE, null, "Salle de reunion", null,
                BigDecimal.ONE, new BigDecimal("100000"),
                discountRate == null ? null : new BigDecimal(discountRate),
                discountAmount == null ? null : new BigDecimal(discountAmount),
                true, false, null, null, null, null, null, null, null, null, null);
    }

    /** Recopie d'une ligne persistee, telle que BillingDocumentServiceImpl la produit. */
    private CreateBillingDocumentLineRequest copyOf(BillingDocumentLine line) {
        return new CreateBillingDocumentLineRequest(
                line.getLineOrder(), line.getLineType(), line.getItemCode(), line.getDescription(),
                line.getDetailedDescription(), line.getQuantity(), line.getUnitPrice(),
                line.getDiscountRate(),
                residualFixedDiscount(line.getSubtotalAmount(), line.getDiscountRate(), line.getDiscountAmount()),
                line.getTaxable(), line.getTaxIncluded(), line.getVatRate(), line.getAdditionalCentRate(),
                null, null, line.getUnit(), null, null, null, line.getCategory());
    }

    private BigDecimal residualFixedDiscount(BigDecimal subtotal, BigDecimal discountRate,
                                             BigDecimal discountAmount) {
        BigDecimal amount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        if (discountRate == null || discountRate.signum() <= 0
                || subtotal == null || subtotal.signum() <= 0) {
            return amount;
        }
        BigDecimal fromRate = subtotal.multiply(discountRate)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal residual = amount.subtract(fromRate);
        return residual.signum() > 0 ? residual : BigDecimal.ZERO;
    }
}
