package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.config.BillingDiscountGuardProperties;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.model.ServiceCatalogItem;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BillingDiscountGuardTest {

    private final ServiceCatalogItemRepository catalogItemRepository = mock(ServiceCatalogItemRepository.class);
    private final BillingDiscountGuardProperties properties = new BillingDiscountGuardProperties();
    private final BillingDiscountGuard guard = new BillingDiscountGuard(catalogItemRepository, properties);

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldIgnoreAnItemWhosePolicyIsNone() {
        // Politique NONE : aucun controle, meme avec un plancher renseigne.
        catalogReturns(item("NONE", "10000", null));

        // Remise volontairement sous le seuil global, pour n'observer que le controle par article.
        assertThat(guard.evaluate(List.of(line("15000", "1000", "1")), new BigDecimal("15000"), new BigDecimal("1000")))
                .isEmpty();
    }

    @Test
    void shouldFlagALineFallingBelowItsFloor() {
        catalogReturns(item("BLOCK", "14000", null));

        // 15 000 brut, 2 000 de remise, une unite · prix net 13 000, sous le plancher de 14 000.
        // La remise reste a 13,3 %, sous le seuil global, pour isoler le controle par article.
        var violations = guard.evaluate(List.of(line("15000", "2000", "1")),
                new BigDecimal("15000"), new BigDecimal("2000"));

        assertThat(violations).hasSize(1);
        assertThat(violations.getFirst().type()).isEqualTo("FLOOR_PRICE");
        assertThat(violations.getFirst().severity()).isEqualTo("BLOCK");
        assertThat(violations.getFirst().observed()).isEqualByComparingTo("13000");
        assertThat(violations.getFirst().limit()).isEqualByComparingTo("14000");
    }

    @Test
    void shouldCompareTheFloorAgainstThePriceActuallyGranted() {
        // Le plancher borne le prix remise, pas le prix catalogue · sinon une remise suffirait
        // a passer dessous sans jamais declencher le controle.
        catalogReturns(item("BLOCK", "10000", null));

        assertThat(guard.evaluate(List.of(line("15000", "0", "1")),
                new BigDecimal("15000"), BigDecimal.ZERO)).isEmpty();
    }

    @Test
    void shouldBoundADiscountEnteredAsAnAmountLikeOneEnteredAsARate() {
        catalogReturns(item("WARN", null, "10"));

        // 1 500 sur 10 000, soit 15 % · au-dela des 10 % admis, bien que saisie en valeur.
        var violations = guard.evaluate(List.of(line("10000", "1500", "1")),
                new BigDecimal("10000"), new BigDecimal("1500"));

        assertThat(violations).hasSize(1);
        assertThat(violations.getFirst().type()).isEqualTo("MAX_DISCOUNT_RATE");
        assertThat(violations.getFirst().severity()).isEqualTo("WARN");
        assertThat(violations.getFirst().observed()).isEqualByComparingTo("15");
    }

    @Test
    void shouldCountLineDiscountsInTheGlobalThreshold() {
        // Aucune limite au catalogue · seul le seuil global joue. 30 % depasse les 20 % admis.
        when(catalogItemRepository.findByItemCode(anyString())).thenReturn(Optional.empty());

        var violations = guard.evaluate(List.of(line("10000", "3000", "1")),
                new BigDecimal("10000"), new BigDecimal("3000"));

        assertThat(violations).hasSize(1);
        assertThat(violations.getFirst().type()).isEqualTo("DOCUMENT_DISCOUNT_RATE");
        assertThat(violations.getFirst().severity()).isEqualTo("BLOCK");
    }

    @Test
    void shouldApplyNoGlobalThresholdWhenTheGuardIsDisabled() {
        properties.setEnabled(false);
        when(catalogItemRepository.findByItemCode(anyString())).thenReturn(Optional.empty());

        assertThat(guard.evaluate(List.of(line("10000", "9000", "1")),
                new BigDecimal("10000"), new BigDecimal("9000"))).isEmpty();
    }

    @Test
    void shouldLetAWarningThroughWithoutAnyOverride() {
        var warning = new BillingDiscountGuard.DiscountViolation(
                "MAX_DISCOUNT_RATE", "WARN", "SRV-1", "Salle", BigDecimal.TEN, BigDecimal.ONE, "trop");

        assertThat(guard.enforce(List.of(warning), null)).isNull();
    }

    @Test
    void shouldRefuseABlockingViolationWithoutAReason() {
        assertThatThrownBy(() -> guard.enforce(List.of(blocking()), null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("motif de derogation est requis");
    }

    @Test
    void shouldRefuseAReasonFromSomeoneWithoutThePermission() {
        authenticateWith("BILLING:UPDATE");

        assertThatThrownBy(() -> guard.enforce(List.of(blocking()), "Geste commercial"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("BILLING:DISCOUNT_OVERRIDE");
    }

    @Test
    void shouldAcceptAMotivatedOverrideFromSomeoneHoldingThePermission() {
        authenticateWith("BILLING:DISCOUNT_OVERRIDE");

        assertThat(guard.enforce(List.of(blocking()), "  Geste commercial  "))
                .isEqualTo("Geste commercial");
    }

    @Test
    void shouldAlsoAcceptTheUnderscoreSpellingOfThePermission() {
        // resolveAuthorities publie les deux ecritures · le garde-fou doit accepter les deux.
        authenticateWith("BILLING_DISCOUNT_OVERRIDE");

        assertThat(guard.enforce(List.of(blocking()), "Geste commercial")).isEqualTo("Geste commercial");
    }

    // =================================================================================

    private void catalogReturns(ServiceCatalogItem item) {
        when(catalogItemRepository.findByItemCode(anyString())).thenReturn(Optional.of(item));
    }

    private ServiceCatalogItem item(String policy, String floorPrice, String maxDiscountRate) {
        return ServiceCatalogItem.builder()
                .itemCode("SRV-001")
                .name("Salle de reunion")
                .unitPrice(new BigDecimal("15000"))
                .discountPolicy(policy)
                .floorPrice(floorPrice == null ? null : new BigDecimal(floorPrice))
                .maxDiscountRate(maxDiscountRate == null ? null : new BigDecimal(maxDiscountRate))
                .build();
    }

    private BillingDocumentLine line(String subtotal, String discount, String quantity) {
        return BillingDocumentLine.builder()
                .itemCode("SRV-001")
                .description("Salle de reunion")
                .quantity(new BigDecimal(quantity))
                .subtotalAmount(new BigDecimal(subtotal))
                .discountAmount(new BigDecimal(discount))
                .build();
    }

    private BillingDiscountGuard.DiscountViolation blocking() {
        return new BillingDiscountGuard.DiscountViolation(
                "FLOOR_PRICE", "BLOCK", "SRV-1", "Salle",
                new BigDecimal("9000"), new BigDecimal("10000"), "sous le plancher");
    }

    private void authenticateWith(String authority) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@bokati.com", "n/a",
                        List.of(new SimpleGrantedAuthority(authority))));
    }
}
