package com.sni.bokaticowork.features.payment.mapper.decorator;

import com.sni.bokaticowork.features.payment.dto.response.CashMovementAttachmentResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashMovementResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashRegisterResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSessionResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSessionSummaryResponse;
import com.sni.bokaticowork.features.payment.enums.CashFlowDirection;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;
import com.sni.bokaticowork.features.payment.mapper.interfaces.CashRegisterMapper;
import com.sni.bokaticowork.features.payment.model.CashMovement;
import com.sni.bokaticowork.features.payment.model.CashMovementAttachment;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.repository.CashMovementAttachmentRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.features.payment.service.support.CashSessionSummarySupport;
import com.sni.bokaticowork.features.payment.service.support.TransactionContextResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Component
public abstract class CashRegisterMapperDecorator implements CashRegisterMapper {

    @Autowired
    @Qualifier("delegate")
    private CashRegisterMapper delegate;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private TransactionContextResolver contextResolver;

    @Autowired
    private CashSessionSummarySupport summarySupport;

    @Autowired
    private com.sni.bokaticowork.features.payment.service.support.CashActorResolver actors;

    @Autowired
    private CashMovementAttachmentRepository attachmentRepository;

    @Autowired
    private com.sni.bokaticowork.features.payment.repository.CashMovementRepository cashMovementRepository;

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
                cashRegister.getSystemManaged(),
                cashRegister.getRestrictedToMethod(),
                cashRegister.getMaxCashAmount(),
                cashRegister.getManagerEmail(),
                cashRegister.getCreatedAt(),
                cashRegister.getUpdatedAt(),
                cashRegister.getRegisterType(),
                cashRegister.getStatus(),
                cashRegister.getParentRegisterCode(),
                cashRegister.getLastCountAt(),
                cashRegister.getLastCountAmount()
        );
    }

    @Override
    public CashSessionResponse toCashSessionResponse(CashSession cashSession) {
        CashSessionSummaryResponse summary = summarySupport.summarize(cashSession);
        BigDecimal expectedClosingAmount = firstNonNull(cashSession.getExpectedClosingAmount(), summary.expectedClosingAmount(), BigDecimal.ZERO);
        BigDecimal countedClosingAmount = firstNonNull(cashSession.getCountedClosingAmount(), cashSession.getClosingAmount(), expectedClosingAmount);
        BigDecimal closingAmount = firstNonNull(cashSession.getClosingAmount(), countedClosingAmount, expectedClosingAmount);
        BigDecimal varianceAmount = firstNonNull(cashSession.getVarianceAmount(), countedClosingAmount.subtract(expectedClosingAmount));
        boolean hidden = expectedHidden(cashSession);
        return new CashSessionResponse(
                cashSession.getSessionNumber(),
                cashSession.getCashRegister().getRegisterCode(),
                cashSession.getStatus(),
                textOrEmpty(cashSession.getOpenedBy()),
                textOrEmpty(cashSession.getClosedBy()),
                textOrEmpty(cashSession.getReviewedBy()),
                firstNonNull(cashSession.getOpeningAmount(), BigDecimal.ZERO),
                hidden ? null : closingAmount,
                hidden ? null : expectedClosingAmount,
                hidden ? null : countedClosingAmount,
                hidden ? null : varianceAmount,
                textOrEmpty(cashSession.getVarianceReason()),
                cashSession.getOpenedAt(),
                cashSession.getClosingRequestedAt(),
                cashSession.getClosedAt(),
                cashSession.getExpectedOpeningAmount(),
                cashSession.getOpeningVariance(),
                cashSession.getOpeningVarianceReason(),
                cashSession.getPausedAt(),
                cashSession.getPauseSeconds(),
                cashSession.getSuspendedBy(),
                cashSession.getSuspensionReason(),
                cashSession.getKeptFloat(),
                cashSession.getAmountToRemit(),
                cashSession.getClosingCountNumber(),
                cashSession.getVarianceDecision(),
                cashSession.getVarianceDecidedBy(),
                cashSession.getForcedClose(),
                cashSession.getHandoverNumber(),
                cashSession.getOutOfHoursReason(),
                hidden
        );
    }

    /**
     * Cloture a l'aveugle (lot C2) : tant que la session n'est pas comptee, l'attendu, l'ecart et le
     * solde courant ne sont pas montres a qui n'a pas le droit de decider d'un ecart.
     */
    private boolean expectedHidden(CashSession session) {
        if (session == null || session.getCashRegister() == null || !Boolean.TRUE.equals(session.getCashRegister().getBlindClose())) {
            return false;
        }
        var status = session.getStatus();
        boolean uncounted = status == com.sni.bokaticowork.features.payment.enums.CashSessionStatus.OPEN || status == com.sni.bokaticowork.features.payment.enums.CashSessionStatus.PAUSED
                || status == com.sni.bokaticowork.features.payment.enums.CashSessionStatus.SUSPENDED || status == com.sni.bokaticowork.features.payment.enums.CashSessionStatus.COUNTING
                || status == com.sni.bokaticowork.features.payment.enums.CashSessionStatus.RECOUNT || status == com.sni.bokaticowork.features.payment.enums.CashSessionStatus.OPENING;
        return uncounted && (actors == null || !actors.hasAuthority("CASH_VARIANCE:DECIDE"));
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
        String relatedMovementNumber = relatedMovementNumber(cashMovement);
        List<CashMovementAttachmentResponse> attachments = attachmentRepository
                .findByCashMovement_IdOrderByUploadedAtDesc(cashMovement.getId())
                .stream()
                .map(this::toAttachmentResponse)
                .toList();
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
                flowDirection(cashMovement),
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
                cashMovement.getStatus(),
                relatedMovementNumber,
                cashMovement.getBatchId(),
                expectedHidden(session) ? null : cashMovement.getRunningBalance(),
                cashMovement.getExchangeRate(),
                cashMovement.getChannel(),
                cashMovement.getDeviceCode(),
                cashMovement.getDeviceIp(),
                cashMovement.getSubCategory(),
                cashMovement.getTags(),
                cashMovement.getRiskScore(),
                cashMovement.getRequiresSignature(),
                cashMovement.getSignedBy(),
                cashMovement.getSignedAt(),
                cashMovement.getPrintedAt(),
                attachments,
                cashMovement.getCreatedAt(),
                cashMovement.getNatureCode(),
                cashMovement.getTransferOrderNumber(),
                cashMovement.getApprovedBy(),
                cashMovement.getApprovedAt(),
                cashMovement.getExpiresAt(),
                cashMovement.getReversalReason(),
                cashMovement.getCancelReason()
        );
    }

    private CashMovementAttachmentResponse toAttachmentResponse(CashMovementAttachment attachment) {
        return new CashMovementAttachmentResponse(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getStoragePath(),
                attachment.getLabel(),
                attachment.getUploadedBy(),
                attachment.getUploadedAt()
        );
    }

    private String relatedMovementNumber(CashMovement cashMovement) {
        Long relatedId = cashMovement.getRelatedMovementId();
        if (relatedId == null) {
            return null;
        }
        return cashMovementRepository.findById(relatedId)
                .map(CashMovement::getMovementNumber)
                .orElse(null);
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

    /**
     * Sens affiche : celui de la regle unique ; un ajustement montre desormais s'il entre ou sort,
     * seul un mouvement neutre (comptage) reste marque {@code ADJUSTMENT}.
     */
    private CashFlowDirection flowDirection(CashMovement movement) {
        CashFlowDirection direction = com.sni.bokaticowork.features.payment.service.support.CashFlowRules
                .direction(movement.getMovementType(), movement.getDirection());
        return direction == null ? CashFlowDirection.ADJUSTMENT : direction;
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String textOrEmpty(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
