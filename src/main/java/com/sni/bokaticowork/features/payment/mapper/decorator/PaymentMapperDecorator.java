package com.sni.bokaticowork.features.payment.mapper.decorator;

import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentTransactionResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashRegisterResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSessionResponse;
import com.sni.bokaticowork.features.payment.dto.response.WalletHoldResponse;
import com.sni.bokaticowork.features.payment.dto.response.WalletLedgerEntryResponse;
import com.sni.bokaticowork.features.payment.dto.response.WalletResponse;
import com.sni.bokaticowork.features.payment.mapper.interfaces.PaymentMapper;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.model.WalletHold;
import com.sni.bokaticowork.features.payment.model.WalletLedgerEntry;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.features.payment.service.support.TransactionContextResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public abstract class PaymentMapperDecorator implements PaymentMapper {

    @Autowired
    @Qualifier("delegate")
    private PaymentMapper delegate;

    @Autowired
    private TransactionContextResolver contextResolver;

    @Autowired
    private PaymentTransactionRepository transactionRepository;

    @Autowired(required = false)
    private com.sni.bokaticowork.features.payment.repository.WalletHoldRepository holdRepository;

    private String sourceLabel(String sourceType, String sourceCode) {
        if (sourceType == null || sourceCode == null) {
            return null;
        }
        try {
            TransactionContextResolver.SourceView view = contextResolver.resolveSource(sourceType, sourceCode);
            return view == null ? null : view.label();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    @Override
    public PaymentIntentResponse toIntentResponse(PaymentIntent intent) {
        TransactionContextResolver.PartyView party = contextResolver.resolveParty(intent.getCustomerType(), intent.getCustomerCode());
        TransactionContextResolver.SourceView source = contextResolver.resolveSource(intent.getSourceType(), intent.getSourceCode());
        java.math.BigDecimal paidAmount = money(transactionRepository.sumSucceededAmountByPaymentIntentId(intent.getId()));
        java.math.BigDecimal processingAmount = money(transactionRepository.sumProcessingAmountByPaymentIntentId(intent.getId()));
        java.math.BigDecimal remainingAmount = money(intent.getAmount()).subtract(paidAmount).max(java.math.BigDecimal.ZERO.setScale(4));
        java.math.BigDecimal payableAmount = remainingAmount.subtract(processingAmount).max(java.math.BigDecimal.ZERO.setScale(4));
        return new PaymentIntentResponse(
                intent.getIntentNumber(),
                intent.getCustomerType(),
                intent.getCustomerCode(),
                party.name(),
                party.email(),
                party.phone(),
                party.billingAddressJson(),
                party.registered(),
                intent.getAmount(),
                paidAmount,
                processingAmount,
                remainingAmount,
                payableAmount,
                intent.getCurrency(),
                intent.getStatus(),
                intent.getPurpose(),
                intent.getSourceType(),
                intent.getSourceCode(),
                source.type(),
                source.code(),
                source.label(),
                source.registered(),
                intent.getPaymentLinkToken(),
                intent.getPaymentLinkExpiresAt(),
                intent.getIdempotencyKey(),
                intent.getExpiresAt(),
                intent.getMetadataJson(),
                intent.getCancelledAt(),
                intent.getCancelledBy(),
                intent.getCancellationReason(),
                intent.getRelaunchCount() == null ? 0 : intent.getRelaunchCount(),
                intent.getLastRelaunchedAt(),
                intent.getLastRelaunchedBy(),
                com.sni.bokaticowork.features.payment.service.support.PaymentIntentStates.relaunchable(intent, remainingAmount, processingAmount),
                com.sni.bokaticowork.features.payment.service.support.PaymentIntentStates.cancellable(intent, processingAmount)
        );
    }

    private java.math.BigDecimal money(java.math.BigDecimal amount) {
        return (amount == null ? java.math.BigDecimal.ZERO : amount).setScale(4, java.math.RoundingMode.HALF_UP);
    }

    @Override
    public PaymentTransactionResponse toTransactionResponse(PaymentTransaction transaction) {
        return new PaymentTransactionResponse(
                transaction.getTransactionNumber(),
                transaction.getPaymentIntent().getIntentNumber(),
                transaction.getPaymentMethod(),
                transaction.getProvider(),
                transaction.getProviderReference(),
                transaction.getReceiptNumber(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus(),
                transaction.getPaidAt(),
                transaction.getReceivedBy(),
                transaction.getFailureReason(),
                transaction.getMetadataJson(),
                transaction.getChannel(),
                com.sni.bokaticowork.features.payment.service.support.PaymentMethodLabels.label(transaction.getPaymentMethod(), transaction.getProvider()),
                transaction.getTenderedAmount(),
                transaction.getChangeAmount(),
                transaction.getCashSessionNumber(),
                transaction.getBankName(),
                transaction.getPayerName(),
                transaction.getPayerPhone()
        );
    }

    @Override
    public WalletResponse toWalletResponse(WalletAccount wallet) {
        return new WalletResponse(
                wallet.getWalletNumber(),
                wallet.getOwnerType(),
                wallet.getOwnerCode(),
                wallet.getCurrency(),
                wallet.getStatus(),
                wallet.getAvailableBalance(),
                wallet.getLedgerBalance(),
                wallet.getHeldBalance(),
                wallet.getOpenedAt(),
                wallet.getClosedAt(),
                ownerName(wallet),
                activeHolds(wallet)
        );
    }

    private String ownerName(WalletAccount wallet) {
        try {
            TransactionContextResolver.PartyView party = contextResolver.resolveParty(wallet.getOwnerType(), wallet.getOwnerCode());
            return party == null ? null : party.name();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private java.util.List<WalletResponse.HoldLine> activeHolds(WalletAccount wallet) {
        if (holdRepository == null || wallet.getId() == null || wallet.getHeldBalance() == null || wallet.getHeldBalance().signum() <= 0) {
            return java.util.List.of();
        }
        return holdRepository.findAllByWalletAndStatusOrderByCreatedAtDesc(wallet,
                        com.sni.bokaticowork.features.payment.enums.WalletHoldStatus.ACTIVE).stream()
                .map(hold -> new WalletResponse.HoldLine(hold.getHoldNumber(), hold.getAmount(), hold.getSourceType(),
                        hold.getSourceCode(), sourceLabel(hold.getSourceType(), hold.getSourceCode()), hold.getExpiresAt(),
                        hold.getCreatedAt()))
                .toList();
    }

    @Override
    public WalletHoldResponse toWalletHoldResponse(WalletHold hold) {
        return new WalletHoldResponse(
                hold.getHoldNumber(),
                hold.getWallet().getWalletNumber(),
                hold.getAmount(),
                hold.getCurrency(),
                hold.getStatus(),
                hold.getSourceType(),
                hold.getSourceCode(),
                hold.getExpiresAt(),
                sourceLabel(hold.getSourceType(), hold.getSourceCode()),
                hold.getCreatedBy(),
                hold.getCreatedAt()
        );
    }

    @Override
    public WalletLedgerEntryResponse toLedgerEntryResponse(WalletLedgerEntry entry) {
        return new WalletLedgerEntryResponse(
                entry.getEntryNumber(),
                entry.getTransactionNumber(),
                entry.getTransactionUuid(),
                entry.getWallet().getWalletNumber(),
                entry.getDirection(),
                entry.getAmount(),
                entry.getCurrency(),
                entry.getBalanceAfter(),
                entry.getEntryType(),
                entry.getSourceType(),
                entry.getSourceCode(),
                entry.getReference(),
                entry.getCreatedBy(),
                entry.getCreatedAt(),
                sourceLabel(entry.getSourceType(), entry.getSourceCode())
        );
    }

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
}
