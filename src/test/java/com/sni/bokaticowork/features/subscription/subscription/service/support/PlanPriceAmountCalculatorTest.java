package com.sni.bokaticowork.features.subscription.subscription.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.service.support.BillingTaxRuleResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Le calcul de taxe, désormais écrit une seule fois.
 *
 * <p>Il l'était deux fois, dans l'opérateur de création d'abonnement et dans celui de création de
 * pass, avec les mêmes fonctions utilitaires recopiées. Deux copies d'un calcul de taxe restent
 * identiques tant que personne ne touche à l'une des deux, et la divergence ne se serait vue que
 * sur une facture.</p>
 */
@ExtendWith(MockitoExtension.class)
class PlanPriceAmountCalculatorTest {

    @Mock
    private BillingTaxRuleResolver taxRuleResolver;

    private PlanPriceAmountCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PlanPriceAmountCalculator(taxRuleResolver);
        // TVA 18 %, centime additionnel 5 % · le second se calcule sur la premiere, pas sur la base.
        // Laxiste : le refus d'un montant negatif sort avant de consulter le profil de taxe.
        lenient().when(taxRuleResolver.defaultTaxProfile()).thenReturn(
                new BillingTaxRuleResolver.TaxProfile(new BigDecimal("18"), new BigDecimal("5")));
    }

    @Test
    void addsTaxOnTopWhenThePriceIsAnnouncedWithoutIt() {
        PlanPriceAmountCalculator.Amounts amounts =
                calculator.compute(new BigDecimal("10000"), null, null, false);

        assertEquals(new BigDecimal("10000.0000"), amounts.subtotal());
        // 1800 de TVA, puis 90 de centime additionnel sur ces 1800.
        assertEquals(new BigDecimal("1890.0000"), amounts.tax());
        assertEquals(new BigDecimal("11890.0000"), amounts.total());
    }

    @Test
    void extractsTaxFromWithinWhenThePriceAlreadyContainsIt() {
        PlanPriceAmountCalculator.Amounts amounts =
                calculator.compute(new BigDecimal("11890"), null, null, true);

        assertEquals(new BigDecimal("10000.0000"), amounts.subtotal());
        assertEquals(new BigDecimal("1890.0000"), amounts.tax());
        assertEquals(new BigDecimal("11890.0000"), amounts.total());
    }

    @Test
    void taxesTheSetupFeeLikeThePriceItself() {
        PlanPriceAmountCalculator.Amounts amounts =
                calculator.compute(new BigDecimal("10000"), new BigDecimal("5000"), null, false);

        assertEquals(new BigDecimal("15000.0000"), amounts.subtotal());
        assertEquals(new BigDecimal("2835.0000"), amounts.tax());
    }

    /**
     * La règle qui mérite un test à elle seule : une caution n'est pas un produit, c'est une somme
     * détenue pour le compte du client et restituable. La taxer reviendrait à taxer de l'argent qui
     * ne nous appartient pas.
     */
    @Test
    void addsTheDepositToTheTotalWithoutEverTaxingIt() {
        PlanPriceAmountCalculator.Amounts withoutDeposit =
                calculator.compute(new BigDecimal("10000"), null, null, false);
        PlanPriceAmountCalculator.Amounts withDeposit =
                calculator.compute(new BigDecimal("10000"), null, new BigDecimal("20000"), false);

        assertEquals(withoutDeposit.subtotal(), withDeposit.subtotal());
        assertEquals(withoutDeposit.tax(), withDeposit.tax());
        assertEquals(withoutDeposit.total().add(new BigDecimal("20000.0000")), withDeposit.total());
    }

    @Test
    void treatsAbsentAmountsAsZeroRatherThanFailing() {
        PlanPriceAmountCalculator.Amounts amounts = calculator.compute(null, null, null, false);

        assertEquals(0, amounts.total().signum());
    }

    @Test
    void refusesANegativeAmount() {
        assertThrows(BadRequestException.class,
                () -> calculator.compute(new BigDecimal("-1"), null, null, false));
    }
}
