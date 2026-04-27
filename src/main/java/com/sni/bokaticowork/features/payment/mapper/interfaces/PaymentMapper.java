package com.sni.bokaticowork.features.payment.mapper.interfaces;

import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentTransactionResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashRegisterResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSessionResponse;
import com.sni.bokaticowork.features.payment.dto.response.ReconciliationBatchResponse;
import com.sni.bokaticowork.features.payment.dto.response.WalletHoldResponse;
import com.sni.bokaticowork.features.payment.dto.response.WalletLedgerEntryResponse;
import com.sni.bokaticowork.features.payment.dto.response.WalletResponse;
import com.sni.bokaticowork.features.payment.mapper.decorator.PaymentMapperDecorator;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.model.PaymentReconciliationBatch;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.model.WalletHold;
import com.sni.bokaticowork.features.payment.model.WalletLedgerEntry;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(PaymentMapperDecorator.class)
public interface PaymentMapper {

    default PaymentIntentResponse toIntentResponse(PaymentIntent intent) {
        return null;
    }

    default PaymentTransactionResponse toTransactionResponse(PaymentTransaction transaction) {
        return null;
    }

    default WalletResponse toWalletResponse(WalletAccount wallet) {
        return null;
    }

    default WalletHoldResponse toWalletHoldResponse(WalletHold hold) {
        return null;
    }

    default WalletLedgerEntryResponse toLedgerEntryResponse(WalletLedgerEntry entry) {
        return null;
    }

    default CashRegisterResponse toCashRegisterResponse(CashRegister cashRegister) {
        return null;
    }

    default CashSessionResponse toCashSessionResponse(CashSession cashSession) {
        return null;
    }

    default ReconciliationBatchResponse toReconciliationBatchResponse(PaymentReconciliationBatch batch) {
        return null;
    }
}
