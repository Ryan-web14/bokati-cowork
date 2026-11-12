package com.sni.bokaticowork.features.payment;

import com.sni.bokaticowork.features.payment.enums.CashFlowDirection;
import com.sni.bokaticowork.features.payment.enums.CashMovementStatus;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.model.CashMovement;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.repository.CashMovementRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Lot C0 sur le schema de production : sens des ajustements, filtre de statut, permissions. */
@org.springframework.test.context.TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class CashCorrectionsC0IT extends PostgresIntegrationTestBase {

    @Autowired private DataSource dataSource;
    @Autowired private CashRegisterRepository registerRepository;
    @Autowired private CashSessionRepository sessionRepository;
    @Autowired private CashMovementRepository movementRepository;

    @Test
    @DisplayName("Les totaux de session signent les ajustements et ignorent l'attente et l'annule")
    void sessionTotalsFollowTheSingleDirectionRule() {
        CashSession session = session("C0-A");
        movement(session, "C0-A-1", CashMovementType.OPENING_FLOAT, "10000", null, CashMovementStatus.CONFIRMED);
        movement(session, "C0-A-2", CashMovementType.PAYMENT, "5000", null, CashMovementStatus.CONFIRMED);
        movement(session, "C0-A-3", CashMovementType.ADJUSTMENT, "2000", CashFlowDirection.OUT, CashMovementStatus.CONFIRMED);
        movement(session, "C0-A-4", CashMovementType.ADJUSTMENT, "500", CashFlowDirection.IN, CashMovementStatus.CONFIRMED);
        movement(session, "C0-A-5", CashMovementType.CASH_OUT, "999", null, CashMovementStatus.PENDING);
        movement(session, "C0-A-6", CashMovementType.CASH_IN, "777", null, CashMovementStatus.CANCELLED);
        movementRepository.flush();

        Object[] row = movementRepository.sessionTotals(session.getId());
        Object[] totals = row.length == 1 && row[0] instanceof Object[] nested ? nested : row;
        assertThat(new BigDecimal(totals[0].toString())).isEqualByComparingTo("5000");
        assertThat(new BigDecimal(totals[2].toString())).as("l'entree annulee ne compte pas").isEqualByComparingTo("0");
        assertThat(new BigDecimal(totals[3].toString())).as("la sortie en attente ne compte pas").isEqualByComparingTo("0");
        assertThat(new BigDecimal(totals[4].toString())).as("ajustements nets : -2000 + 500").isEqualByComparingTo("-1500");
    }

    @Test
    @DisplayName("Un ajustement sans sens est refuse par la base")
    void adjustmentWithoutDirectionIsRejected() {
        CashSession session = session("C0-B");
        assertThatThrownBy(() -> {
            movement(session, "C0-B-1", CashMovementType.ADJUSTMENT, "100", null, CashMovementStatus.CONFIRMED);
            movementRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Les permissions fines des caisses existent")
    void finePermissionsExist() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        List<String> names = jdbc.queryForList(
                "SELECT name FROM permission WHERE name LIKE 'CASH\\_%\\_%' ESCAPE '\\' ORDER BY name", String.class);
        assertThat(names).contains("CASH_REGISTER_MANAGE", "CASH_SESSION_OPEN", "CASH_SESSION_CLOSE",
                "CASH_VARIANCE_DECIDE", "CASH_MOVEMENT_CREATE", "CASH_MOVEMENT_APPROVE", "CASH_MOVEMENT_REVERSE");
    }

    private CashSession session(String code) {
        CashRegister register = registerRepository.save(CashRegister.builder()
                .registerCode("CSR-" + code)
                .name("Caisse " + code)
                .businessEntityCode("BIZ-" + code)
                .deviceCode("DEV-" + code)
                .active(true)
                .cashControlEnabled(true)
                .build());
        return sessionRepository.save(CashSession.builder()
                .sessionNumber("CSS-" + code)
                .cashRegister(register)
                .status(CashSessionStatus.OPEN)
                .openedBy("caissier@elleaose.com")
                .openingAmount(new BigDecimal("10000"))
                .build());
    }

    private void movement(CashSession session, String number, CashMovementType type, String amount,
                          CashFlowDirection direction, CashMovementStatus status) {
        movementRepository.save(CashMovement.builder()
                .movementNumber(number)
                .cashSession(session)
                .movementType(type)
                .amount(new BigDecimal(amount))
                .currency("XAF")
                .direction(direction)
                .status(status)
                .createdBy("caissier@elleaose.com")
                .build());
    }
}
