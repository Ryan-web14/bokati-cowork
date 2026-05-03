package com.sni.bokaticowork.features.payment.mapper.decorator;

import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentTransactionResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashRegisterResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSessionResponse;
import com.sni.bokaticowork.features.payment.dto.response.ReconciliationBatchResponse;
import com.sni.bokaticowork.features.payment.dto.response.WalletHoldResponse;
import com.sni.bokaticowork.features.payment.dto.response.WalletLedgerEntryResponse;
import com.sni.bokaticowork.features.payment.dto.response.WalletResponse;
import com.sni.bokaticowork.features.payment.mapper.interfaces.PaymentMapper;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.model.PaymentReconciliationBatch;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.model.WalletHold;
import com.sni.bokaticowork.features.payment.model.WalletLedgerEntry;
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

    @Override
    public PaymentIntentResponse toIntentResponse(PaymentIntent intent) {
        TransactionContextResolver.PartyView party = contextResolver.resolveParty(intent.getCustomerType(), intent.getCustomerCode());
        TransactionContextResolver.SourceView source = contextResolver.resolveSource(intent.getSourceType(), intent.getSourceCode());
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
                intent.getCurrency(),
                intent.getStatus(),
                intent.getPurpose(),
                intent.getSourceType(),
                intent.getSourceCode(),
                source.type(),
                source.code(),
                source.label(),
                source.registered(),
                intent.getIdempotencyKey(),
                intent.getExpiresAt(),
                intent.getMetadataJson()
        );
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
                transaction.getMetadataJson()
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
                wallet.getClosedAt()
        );
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
                hold.getExpiresAt()
        );
    }

    @Override
    public WalletLedgerEntryResponse toLedgerEntryResponse(WalletLedgerEntry entry) {
        return new WalletLedgerEntryResponse(
                entry.getEntryNumber(),
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
                entry.getCreatedAt()
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
    public ReconciliationBatchResponse toReconciliationBatchResponse(PaymentReconciliationBatch batch) {
        return new ReconciliationBatchResponse(
                batch.getBatchNumber(),
                batch.getProvider(),
                batch.getStatus(),
                batch.getCreatedBy(),
                batch.getCreatedAt(),
                batch.getCompletedAt()
        );
    }
}
