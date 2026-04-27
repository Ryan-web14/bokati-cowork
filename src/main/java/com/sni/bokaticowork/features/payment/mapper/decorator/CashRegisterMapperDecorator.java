package com.sni.bokaticowork.features.payment.mapper.decorator;

import com.sni.bokaticowork.features.payment.dto.response.CashMovementResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashRegisterResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSessionResponse;
import com.sni.bokaticowork.features.payment.enums.CashFlowDirection;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;
import com.sni.bokaticowork.features.payment.mapper.interfaces.CashRegisterMapper;
import com.sni.bokaticowork.features.payment.model.CashMovement;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public abstract class CashRegisterMapperDecorator implements CashRegisterMapper {

    @Autowired
    @Qualifier("delegate")
    private CashRegisterMapper delegate;

    @Override
    public CashRegisterResponse toCashRegisterResponse(CashRegister cashRegister) {
        return new CashRegisterResponse(
                cashRegister.getRegisterCode(),
                cashRegister.getName(),
                cashRegister.getLocationCode(),
                cashRegister.getBusinessEntityCode(),
                cashRegister.getDeviceCode(),
                cashRegister.getActive(),
                cashRegister.getCashControlEnabled(),
                cashRegister.getMaxCashAmount(),
                cashRegister.getCreatedAt(),
                cashRegister.getUpdatedAt()
        );
    }

    @Override
    public CashSessionResponse toCashSessionResponse(CashSession cashSession) {
        return new CashSessionResponse(
                cashSession.getSessionNumber(),
                cashSession.getCashRegister().getRegisterCode(),
                cashSession.getStatus(),
                cashSession.getOpenedBy(),
                cashSession.getClosedBy(),
                cashSession.getReviewedBy(),
                cashSession.getOpeningAmount(),
                cashSession.getClosingAmount(),
                cashSession.getExpectedClosingAmount(),
                cashSession.getCountedClosingAmount(),
                cashSession.getVarianceAmount(),
                cashSession.getVarianceReason(),
                cashSession.getOpenedAt(),
                cashSession.getClosingRequestedAt(),
                cashSession.getClosedAt()
        );
    }

    @Override
    public CashMovementResponse toCashMovementResponse(CashMovement cashMovement) {
        CashSession session = cashMovement.getCashSession();
        CashRegister register = session == null ? null : session.getCashRegister();
        return new CashMovementResponse(
                cashMovement.getMovementNumber(),
                session == null ? null : session.getSessionNumber(),
                register == null ? null : register.getRegisterCode(),
                cashMovement.getMovementType(),
                flowDirection(cashMovement.getMovementType()),
                cashMovement.getAmount(),
                cashMovement.getCurrency(),
                cashMovement.getDocumentType(),
                cashMovement.getDocumentNumber(),
                cashMovement.getFlowCategory(),
                cashMovement.getReferenceType(),
                cashMovement.getReferenceCode(),
                cashMovement.getCounterpartyType(),
                cashMovement.getCounterpartyCode(),
                cashMovement.getCounterpartyName(),
                cashMovement.getReason(),
                cashMovement.getCreatedBy(),
                cashMovement.getMetadataJson(),
                cashMovement.getCreatedAt()
        );
    }

    private CashFlowDirection flowDirection(CashMovementType movementType) {
        if (movementType == null) {
            return CashFlowDirection.ADJUSTMENT;
        }
        return switch (movementType) {
            case PAYMENT, CASH_IN, TRANSFER_IN, OPENING_FLOAT -> CashFlowDirection.IN;
            case REFUND, CASH_OUT, SAFE_DEPOSIT, TRANSFER_OUT -> CashFlowDirection.OUT;
            case ADJUSTMENT, CLOSING_COUNT -> CashFlowDirection.ADJUSTMENT;
        };
    }
}
