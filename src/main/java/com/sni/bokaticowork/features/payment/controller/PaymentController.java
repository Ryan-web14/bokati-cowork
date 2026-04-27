package com.sni.bokaticowork.features.payment.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentFromBillingDocumentRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentForDocumentsRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentRecoveryIntentRequest;
import com.sni.bokaticowork.features.payment.dto.request.RefundPaymentRequest;
import com.sni.bokaticowork.features.payment.dto.request.RegisterCashPaymentRequest;
import com.sni.bokaticowork.features.payment.dto.request.WalletPaymentRequest;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentRecoveryResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentTransactionResponse;
import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPath.V1 + "/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/intents")
    public ResponseEntity<PaymentIntentResponse> createIntent(@Valid @RequestBody CreatePaymentIntentRequest request) {
        return ResponseEntity.ok(paymentService.createIntent(request));
    }

    @PostMapping("/intents/from-billing-document")
    public ResponseEntity<PaymentIntentResponse> createIntentFromBillingDocument(@Valid @RequestBody CreatePaymentIntentFromBillingDocumentRequest request) {
        return ResponseEntity.ok(paymentService.createIntentFromBillingDocument(request));
    }

    @PostMapping("/intents/from-billing-documents")
    public ResponseEntity<PaymentIntentResponse> createIntentForBillingDocuments(@Valid @RequestBody CreatePaymentIntentForDocumentsRequest request) {
        return ResponseEntity.ok(paymentService.createIntentForBillingDocuments(request));
    }

    @GetMapping("/recovery/{customerType}/{customerCode}")
    public ResponseEntity<PaymentRecoveryResponse> previewRecovery(@PathVariable String customerType,
                                                                   @PathVariable String customerCode,
                                                                   @RequestParam(required = false) String currency) {
        return ResponseEntity.ok(paymentService.previewRecovery(customerType, customerCode, currency));
    }

    @PostMapping("/intents/recovery")
    public ResponseEntity<PaymentRecoveryResponse> createRecoveryIntent(@Valid @RequestBody CreatePaymentRecoveryIntentRequest request) {
        return ResponseEntity.ok(paymentService.createRecoveryIntent(request));
    }

    @PatchMapping("/intents/{intentNumber}/cash")
    public ResponseEntity<PaymentTransactionResponse> registerCashPayment(@PathVariable String intentNumber,
                                                                          @Valid @RequestBody RegisterCashPaymentRequest request) {
        return ResponseEntity.ok(paymentService.registerCashPayment(intentNumber, request));
    }

    @PatchMapping("/intents/{intentNumber}/wallet")
    public ResponseEntity<PaymentTransactionResponse> payWithWallet(@PathVariable String intentNumber,
                                                                    @Valid @RequestBody WalletPaymentRequest request) {
        return ResponseEntity.ok(paymentService.payWithWallet(intentNumber, request));
    }

    @PostMapping("/transactions/{transactionNumber}/refund")
    public ResponseEntity<PaymentTransactionResponse> refund(@PathVariable String transactionNumber,
                                                             @Valid @RequestBody RefundPaymentRequest request) {
        return ResponseEntity.ok(paymentService.refund(transactionNumber, request));
    }

    @PostMapping("/transactions/{transactionNumber}/reverse")
    public ResponseEntity<PaymentTransactionResponse> reverse(@PathVariable String transactionNumber,
                                                              @Valid @RequestBody RefundPaymentRequest request) {
        return ResponseEntity.ok(paymentService.reverse(transactionNumber, request));
    }

    @GetMapping("/intents/{intentNumber}")
    public ResponseEntity<PaymentIntentResponse> getIntent(@PathVariable String intentNumber) {
        return ResponseEntity.ok(paymentService.getIntent(intentNumber));
    }

    @GetMapping("/intents/{intentNumber}/transactions")
    public ResponseEntity<List<PaymentTransactionResponse>> listIntentTransactions(@PathVariable String intentNumber) {
        return ResponseEntity.ok(paymentService.listIntentTransactions(intentNumber));
    }

    @GetMapping("/intents")
    public ResponseEntity<PaginatedResponse<PaymentIntentResponse>> listIntents(
            @RequestParam(required = false) PaymentIntentStatus status,
            @RequestParam(required = false) String customerType,
            @RequestParam(required = false) String customerCode,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String sourceCode,
            @RequestParam(required = false) String searchText,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(paymentService.listIntents(status, customerType, customerCode, sourceType, sourceCode, searchText, pageable));
    }
}
