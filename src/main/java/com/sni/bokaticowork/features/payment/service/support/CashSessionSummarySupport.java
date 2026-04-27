package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.features.payment.dto.response.CashSessionSummaryResponse;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.repository.CashMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class CashSessionSummarySupport {

    private final CashMovementRepository movementRepository;

    public CashSessionSummaryResponse summarize(CashSession session) {
        Totals totals = totals(session.getId());
        BigDecimal expectedClosingAmount = expectedClosingAmount(session, totals);
        return new CashSessionSummaryResponse(
                session.getSessionNumber(),
                session.getCashRegister().getRegisterCode(),
                session.getStatus(),
                session.getOpenedBy(),
                session.getClosedBy(),
                safe(session.getOpeningAmount()),
                totals.totalPayments(),
                totals.totalRefunds(),
                totals.totalCashIn(),
                totals.totalCashOut(),
                totals.totalAdjustments(),
                expectedClosingAmount,
                session.getCountedClosingAmount(),
                session.getVarianceAmount(),
                session.getVarianceReason(),
                session.getOpenedAt(),
                session.getClosingRequestedAt(),
                session.getClosedAt()
        );
    }

    public BigDecimal expectedClosingAmount(CashSession session) {
        return expectedClosingAmount(session, totals(session.getId()));
    }

    private BigDecimal expectedClosingAmount(CashSession session, Totals totals) {
        return safe(session.getOpeningAmount())
                .add(totals.totalPayments())
                .add(totals.totalCashIn())
                .add(totals.totalAdjustments())
                .subtract(totals.totalRefunds())
                .subtract(totals.totalCashOut());
    }

    private Totals totals(Long sessionId) {
        Object[] row = unwrap(movementRepository.sessionTotals(sessionId));
        return new Totals(
                decimalAt(row, 0),
                decimalAt(row, 1),
                decimalAt(row, 2),
                decimalAt(row, 3),
                decimalAt(row, 4)
        );
    }

    private Object[] unwrap(Object[] row) {
        if (row != null && row.length == 1 && row[0] instanceof Object[] nested) {
            return nested;
        }
        return row;
    }

    private BigDecimal decimalAt(Object[] row, int index) {
        if (row == null || row[index] == null) {
            return BigDecimal.ZERO;
        }
        if (row[index] instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(row[index].toString());
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record Totals(
            BigDecimal totalPayments,
            BigDecimal totalRefunds,
            BigDecimal totalCashIn,
            BigDecimal totalCashOut,
            BigDecimal totalAdjustments
    ) {
    }
}
