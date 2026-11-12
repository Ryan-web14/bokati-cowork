package com.sni.bokaticowork.features.payment;

import com.sni.bokaticowork.features.payment.enums.CashDenominationKind;
import com.sni.bokaticowork.features.payment.enums.CashMonthEndPolicy;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.repository.CashDenominationRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterRepository;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** Lot C2 sur le schema de production : coupures, parametres des nouvelles caisses. */
@org.springframework.test.context.TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class CashSessionsC2IT extends PostgresIntegrationTestBase {

    @Autowired private CashDenominationRepository denominationRepository;
    @Autowired private CashRegisterRepository registerRepository;

    @Test
    @DisplayName("Les coupures du franc CFA sont chargees, avec le billet et la piece de 500")
    void xafDenominations() {
        assertThat(denominationRepository.findByCurrencyOrderBySortOrderAsc("XAF"))
                .hasSize(13)
                .anySatisfy(denomination -> {
                    assertThat(denomination.getValue()).isEqualByComparingTo("500");
                    assertThat(denomination.getKind()).isEqualTo(CashDenominationKind.COIN);
                })
                .anySatisfy(denomination -> {
                    assertThat(denomination.getValue()).isEqualByComparingTo("500");
                    assertThat(denomination.getKind()).isEqualTo(CashDenominationKind.BILL);
                });
    }

    @Test
    @DisplayName("Une nouvelle caisse suit le plan : cloture a l'aveugle, billetage, comptage exige en fin de mois")
    void newRegisterFollowsThePlan() {
        CashRegister register = registerRepository.saveAndFlush(CashRegister.builder().registerCode("CSR-C2-A").name("Accueil")
                .businessEntityCode("BIZ").deviceCode("DEV").cashControlEnabled(true).build());

        assertThat(register.getBlindClose()).isTrue();
        assertThat(register.getCountLinesRequired()).isTrue();
        assertThat(register.getMonthEndPolicy()).isEqualTo(CashMonthEndPolicy.REQUIRE_COUNT);
        assertThat(register.getMaxSessionHours()).isEqualTo(24);
    }
}
