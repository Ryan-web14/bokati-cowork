package com.sni.bokaticowork.features.payment.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentFromBillingDocumentRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentForDocumentsRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentRecoveryIntentRequest;
import com.sni.bokaticowork.features.payment.dto.request.PayInvoiceRequest;
import com.sni.bokaticowork.features.payment.dto.request.RefundPaymentRequest;
import com.sni.bokaticowork.features.payment.dto.request.InitiateMobileMoneyDepositRequest;
import com.sni.bokaticowork.features.payment.dto.request.RegisterCashPaymentRequest;
import com.sni.bokaticowork.features.payment.dto.request.WalletPaymentRequest;
import com.sni.bokaticowork.features.payment.dto.response.PayInvoiceResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentRecoveryResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentTransactionResponse;
import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PaymentService {
    PaymentIntentResponse createIntent(CreatePaymentIntentRequest request);
    PaymentIntentResponse createIntentFromBillingDocument(CreatePaymentIntentFromBillingDocumentRequest request);
    PaymentIntentResponse createIntentForBillingDocuments(CreatePaymentIntentForDocumentsRequest request);
    PaymentRecoveryResponse previewRecovery(String customerType, String customerCode, String currency);
    PaymentRecoveryResponse createRecoveryIntent(CreatePaymentRecoveryIntentRequest request);
    PaginatedResponse<BillingDocumentResponse> listPayableDocuments(BillingDocumentType documentType, String customerType, String customerCode, String lineSourceType, String lineSourceCode, String searchText, Pageable pageable);
    PaymentTransactionResponse registerCashPayment(String intentNumber, RegisterCashPaymentRequest request);
    PaymentTransactionResponse payWithWallet(String intentNumber, WalletPaymentRequest request);
    PaymentTransactionResponse initiateMobileMoneyDeposit(String intentNumber, InitiateMobileMoneyDepositRequest request);
    PayInvoiceResponse payInvoice(String documentNumber, PayInvoiceRequest request);
    PaymentTransactionResponse refund(String transactionNumber, RefundPaymentRequest request);
    PaymentTransactionResponse reverse(String transactionNumber, RefundPaymentRequest request);
    PaymentIntentResponse getIntent(String intentNumber);
    List<PaymentTransactionResponse> listIntentTransactions(String intentNumber);
    PaginatedResponse<PaymentIntentResponse> listIntents(PaymentIntentStatus status, String customerType, String customerCode, String sourceType, String sourceCode, String searchText, Pageable pageable);
}
