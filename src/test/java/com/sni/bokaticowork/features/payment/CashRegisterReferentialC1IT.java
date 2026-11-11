package com.sni.bokaticowork.features.payment;

import com.sni.bokaticowork.features.payment.enums.CashAssignmentQuality;
import com.sni.bokaticowork.features.payment.enums.CashAssignmentRole;
import com.sni.bokaticowork.features.payment.enums.CashRegisterStatus;
import com.sni.bokaticowork.features.payment.enums.CashRegisterType;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashRegisterAssignment;
import com.sni.bokaticowork.features.payment.repository.CashReferenceValueRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterAssignmentRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterRepository;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Lot C1 sur le schema de production : colonnes, contraintes, listes initiales. */
@org.springframework.test.context.TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class CashRegisterReferentialC1IT extends PostgresIntegrationTestBase {

    @Autowired private CashRegisterRepository registerRepository;
    @Autowired private CashRegisterAssignmentRepository assignmentRepository;
    @Autowired private CashReferenceValueRepository referenceRepository;

    @Test
    @DisplayName("Une caisse se cree avec son type et son statut ; une caisse automatique est typee comme telle")
    void registerTypeAndStatus() {
        CashRegister counter = registerRepository.saveAndFlush(CashRegister.builder().registerCode("CSR-C1-A").name("Accueil")
                .businessEntityCode("BIZ").deviceCode("DEV").cashControlEnabled(true).build());
        CashRegister automatic = registerRepository.saveAndFlush(CashRegister.builder().registerCode("CSR-C1-AUTO").name("Auto")
                .businessEntityCode("BIZ").deviceCode("DEV").cashControlEnabled(true).systemManaged(true).build());

        assertThat(counter.getRegisterType()).isEqualTo(CashRegisterType.COUNTER);
        assertThat(counter.getStatus()).isEqualTo(CashRegisterStatus.ACTIVE);
        assertThat(counter.getActive()).isTrue();
        assertThat(counter.getAcceptedCurrencies()).isEqualTo("XAF");
        assertThat(automatic.getRegisterType()).isEqualTo(CashRegisterType.AUTOMATIC);
    }

    @Test
    @DisplayName("Un interim sans date de fin est refuse par la base")
    void interimWithoutEndIsRejected() {
        assertThatThrownBy(() -> assignmentRepository.saveAndFlush(CashRegisterAssignment.builder()
                .registerCode("CSR-C1-A").userEmail("x@elleaose.com").role(CashAssignmentRole.CASHIER)
                .quality(CashAssignmentQuality.INTERIM).validFrom(Instant.now()).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Les affectations se retrouvent par courriel, sans tenir compte de la casse")
    void assignmentsByEmail() {
        assignmentRepository.saveAndFlush(CashRegisterAssignment.builder()
                .registerCode("CSR-C1-B").userEmail("Caissier@Elleaose.com").role(CashAssignmentRole.CASHIER)
                .quality(CashAssignmentQuality.HOLDER).validFrom(Instant.now()).build());

        assertThat(assignmentRepository.findLiveByUser("caissier@elleaose.com"))
                .extracting(CashRegisterAssignment::getRegisterCode).contains("CSR-C1-B");
    }

    @Test
    @DisplayName("Les listes initiales sont chargees, avec « Autre, preciser » qui exige un texte")
    void initialReferenceLists() {
        assertThat(referenceRepository.findByListCodeOrderBySortOrderAscLabelAsc("VARIANCE_REASON"))
                .extracting("code").contains("CHANGE_ERROR", "UNEXPLAINED", "OTHER");
        assertThat(referenceRepository.findByListCodeAndCode("SUSPENSION_REASON", "OTHER"))
                .hasValueSatisfying(other -> assertThat(other.getRequiresText()).isTrue());
        assertThat(referenceRepository.findByListCodeOrderBySortOrderAscLabelAsc("BANK")).isNotEmpty();
    }
}
