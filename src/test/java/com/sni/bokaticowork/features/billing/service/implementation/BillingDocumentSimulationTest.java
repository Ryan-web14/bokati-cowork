package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.response.SimulateBillingDocumentResponse;
import com.sni.bokaticowork.features.billing.config.BillingDiscountGuardProperties;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import com.sni.bokaticowork.features.billing.service.support.BillingCalculationService;
import com.sni.bokaticowork.features.billing.service.support.BillingDiscountGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingDocumentSimulationTest {

    @Mock
    private BillingCalculationService calculationService;

    @Mock
    private com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository documentRepository;

    /**
     * Garde-fou reel plutot que simule : la simulation doit remonter les depassements, et un
     * mock renverrait toujours une liste vide sans qu'on s'en apercoive.
     */
    @Spy
    private BillingDiscountGuard discountGuard = new BillingDiscountGuard(
            mock(ServiceCatalogItemRepository.class), new BillingDiscountGuardProperties());

    @InjectMocks
    private BillingDocumentServiceImpl service;

    @Test
    void shouldReturnTotalsWithoutCreatingAnything() {
        // 1000 avec 100 de remise de ligne, 500 sans remise · plus 150 de remise document.
        BillingDocumentLine discounted = line("Salle de reunion", "1000", "100");
        BillingDocumentLine plain = line("Cafe", "500", "0");

        when(calculationService.calculate(any(), any())).thenReturn(
                new BillingCalculationService.CalculatedDocument(
                        List.of(discounted, plain),
                        new BigDecimal("1500"),   // sous-total
                        new BigDecimal("250"),    // remise totale · 100 de ligne + 150 de document
                        new BigDecimal("1250"),
                        new BigDecimal("225"),
                        new BigDecimal("11.25"),
                        new BigDecimal("236.25"),
                        new BigDecimal("1486.25")));

        SimulateBillingDocumentResponse simulation = service.simulate(request());

        assertThat(simulation.totalAmount()).isEqualByComparingTo("1486.25");
        // La remise document n'est pas exposee par le calcul · elle se deduit de l'ecart entre
        // la remise totale et la somme des remises de ligne.
        assertThat(simulation.lineDiscountAmount()).isEqualByComparingTo("100");
        assertThat(simulation.documentDiscountAmount()).isEqualByComparingTo("150");
        assertThat(simulation.discountAmount()).isEqualByComparingTo("250");

        // Simuler ne doit rien ecrire ni consommer de numero de sequence.
        verifyNoInteractions(documentRepository);
    }

    @Test
    void shouldGiveThePerLineBreakdownThatReplacesTheCalculator() {
        when(calculationService.calculate(any(), any())).thenReturn(
                new BillingCalculationService.CalculatedDocument(
                        List.of(line("Salle de reunion", "1000", "150")),
                        new BigDecimal("1000"), new BigDecimal("150"), new BigDecimal("850"),
                        new BigDecimal("153"), new BigDecimal("7.65"), new BigDecimal("160.65"),
                        new BigDecimal("1010.65")));

        SimulateBillingDocumentResponse.SimulatedLine simulated = service.simulate(request()).lines().getFirst();

        assertThat(simulated.subtotalAmount()).isEqualByComparingTo("1000");
        assertThat(simulated.discountAmount()).isEqualByComparingTo("150");
        assertThat(simulated.netAmount()).isEqualByComparingTo("850");
        // Le taux effectif est recalcule meme quand la remise a ete saisie en montant.
        assertThat(simulated.effectiveRate()).isEqualByComparingTo("15.0000");
    }

    @Test
    void shouldNotDivideByZeroOnAFreeLine() {
        when(calculationService.calculate(any(), any())).thenReturn(
                new BillingCalculationService.CalculatedDocument(
                        List.of(line("Geste commercial", "0", "0")),
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        SimulateBillingDocumentResponse.SimulatedLine simulated = service.simulate(request()).lines().getFirst();

        assertThat(simulated.effectiveRate()).isEqualByComparingTo("0");
        assertThat(simulated.netAmount()).isEqualByComparingTo("0");
    }

    private BillingDocumentLine line(String description, String subtotal, String discount) {
        return BillingDocumentLine.builder()
                .description(description)
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal(subtotal))
                .subtotalAmount(new BigDecimal(subtotal))
                .discountAmount(new BigDecimal(discount))
                .taxable(true)
                .build();
    }

    private CreateBillingDocumentRequest request() {
        return new CreateBillingDocumentRequest(
                BillingDocumentType.QUOTE, "MEMBER", "MBR-001", "Jean", null, null, null,
                null, null, "Devis", null, null, "XAF", null, null, null,
                List.of(), List.of(), List.of(),
                null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
