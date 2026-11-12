package com.sni.bokaticowork.features.payment;

import com.sni.bokaticowork.features.payment.enums.CashFlowDirection;
import com.sni.bokaticowork.features.payment.enums.CashTransferKind;
import com.sni.bokaticowork.features.payment.enums.CashTransferStatus;
import com.sni.bokaticowork.features.payment.model.CashTransferOrder;
import com.sni.bokaticowork.features.payment.repository.CashOperationNatureRepository;
import com.sni.bokaticowork.features.payment.repository.CashTransferOrderRepository;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Lot C3 sur le schema de production : natures initiales, ordres de transfert. */
@org.springframework.test.context.TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class CashOperationsC3IT extends PostgresIntegrationTestBase {

    @Autowired private CashOperationNatureRepository natureRepository;
    @Autowired private CashTransferOrderRepository transferRepository;

    @Test
    @DisplayName("Les natures initiales sont chargees, les sorties avec le plafond de 50 000 XAF")
    void initialNatures() {
        assertThat(natureRepository.findByCode("ACHAT_FOURNITURES_BUREAU")).hasValueSatisfying(nature -> {
            assertThat(nature.getDirection()).isEqualTo(CashFlowDirection.OUT);
            assertThat(nature.getMaxPerOperation()).isEqualByComparingTo("50000");
        });
        assertThat(natureRepository.findByCode("AVANCE_A_JUSTIFIER"))
                .hasValueSatisfying(nature -> assertThat(nature.getPriorRequestRequired()).isTrue());
    }

    @Test
    @DisplayName("Un transfert d'une caisse vers elle-meme est refuse par la base")
    void transferToItselfIsRejected() {
        assertThatThrownBy(() -> transferRepository.saveAndFlush(CashTransferOrder.builder().transferNumber("OTI-T-1")
                .kind(CashTransferKind.OTHER).fromRegister("CSR-A").toRegister("CSR-A").currency("XAF")
                .sentAmount(new BigDecimal("100")).status(CashTransferStatus.IN_TRANSIT).sentBy("a@elleaose.com").build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
