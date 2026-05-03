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
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.features.payment.service.support.TransactionContextResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public abstract class CashRegisterMapperDecorator implements CashRegisterMapper {

    @Autowired
    @Qualifier("delegate")
    private CashRegisterMapper delegate;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private TransactionContextResolver contextResolver;

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
        PaymentTransaction paymentTransaction = relatedTransaction(cashMovement);
        TransactionContextResolver.PartyView party = paymentTransaction == null
                ? TransactionContextResolver.PartyView.unregistered(cashMovement.getCounterpartyType(), cashMovement.getCounterpartyCode())
                : contextResolver.resolveParty(paymentTransaction.getPaymentIntent().getCustomerType(), paymentTransaction.getPaymentIntent().getCustomerCode());
        TransactionContextResolver.SourceView source = paymentTransaction == null
                ? contextResolver.resolveSource(cashMovement.getReferenceType(), cashMovement.getReferenceCode())
                : contextResolver.resolveSource(paymentTransaction.getPaymentIntent().getSourceType(), paymentTransaction.getPaymentIntent().getSourceCode());
        return new CashMovementResponse(
                cashMovement.getMovementNumber(),
                session == null ? null : session.getSessionNumber(),
                register == null ? null : register.getRegisterCode(),
                register == null ? null : register.getName(),
                register == null ? null : register.getBusinessEntityCode(),
                register == null ? null : register.getDeviceCode(),
                session == null ? null : session.getStatus(),
                session == null ? null : session.getOpenedBy(),
                session == null ? null : session.getOpenedAt(),
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
                paymentTransaction == null ? null : paymentTransaction.getTransactionNumber(),
                paymentTransaction == null ? null : paymentTransaction.getPaymentIntent().getIntentNumber(),
                paymentTransaction == null ? null : paymentTransaction.getReceiptNumber(),
                paymentTransaction == null ? party.type() : paymentTransaction.getPaymentIntent().getCustomerType(),
                paymentTransaction == null ? party.code() : paymentTransaction.getPaymentIntent().getCustomerCode(),
                paymentTransaction == null ? party.name() : party.name(),
                source.type(),
                source.code(),
                source.label(),
                cashMovement.getMetadataJson(),
                cashMovement.getCreatedAt()
        );
    }

    private PaymentTransaction relatedTransaction(CashMovement cashMovement) {
        if (cashMovement == null || !StringUtils.hasText(cashMovement.getReferenceType()) || !StringUtils.hasText(cashMovement.getReferenceCode())) {
            return null;
        }
        if (!"PAYMENT_TRANSACTION".equalsIgnoreCase(cashMovement.getReferenceType().trim())) {
            return null;
        }
        return paymentTransactionRepository.findByTransactionNumber(cashMovement.getReferenceCode().trim()).orElse(null);
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
