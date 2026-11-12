package com.sni.bokaticowork.features.payment.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentFromBillingDocumentRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentForDocumentsRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentLinkRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentRecoveryIntentRequest;
import com.sni.bokaticowork.features.payment.dto.request.InitiateMobileMoneyDepositRequest;
import com.sni.bokaticowork.features.payment.dto.request.RefundPaymentRequest;
import com.sni.bokaticowork.features.payment.dto.request.RegisterCashPaymentRequest;
import com.sni.bokaticowork.features.payment.dto.request.RegisterOfflinePaymentRequest;
import com.sni.bokaticowork.features.payment.dto.request.CancelPaymentIntentRequest;
import com.sni.bokaticowork.features.payment.dto.request.RelaunchPaymentIntentRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashChangeResponse;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentIntentLifecycleService;
import com.sni.bokaticowork.features.payment.dto.request.WalletPaymentRequest;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentLinkResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentRecoveryResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentTransactionResponse;
import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentLinkService;
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
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping(ApiPath.V1 + "/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentLinkService paymentLinkService;
    private final PaymentIntentLifecycleService intentLifecycleService;

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

    @GetMapping("/payable-documents")
    public ResponseEntity<PaginatedResponse<BillingDocumentResponse>> listPayableDocuments(
            @RequestParam(required = false) BillingDocumentType documentType,
            @RequestParam(required = false) String customerType,
            @RequestParam(required = false) String customerCode,
            @RequestParam(required = false) String lineSourceType,
            @RequestParam(required = false) String lineSourceCode,
            @RequestParam(required = false) String searchText,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(paymentService.listPayableDocuments(
                documentType,
                customerType,
                customerCode,
                lineSourceType,
                lineSourceCode,
                searchText,
                pageable
        ));
    }

    @PatchMapping("/intents/{intentNumber}/cash")
    public ResponseEntity<PaymentTransactionResponse> registerCashPayment(@PathVariable String intentNumber,
                                                                          @Valid @RequestBody RegisterCashPaymentRequest request) {
        return ResponseEntity.ok(paymentService.registerCashPayment(intentNumber, request));
    }

    /** Cheque, virement ou mobile money hors plateforme (MTN MoMo Pay, Airtel Money) saisi au guichet. */
    @PatchMapping("/intents/{intentNumber}/offline")
    public ResponseEntity<PaymentTransactionResponse> registerOfflinePayment(@PathVariable String intentNumber,
                                                                             @Valid @RequestBody RegisterOfflinePaymentRequest request) {
        return ResponseEntity.ok(paymentService.registerOfflinePayment(intentNumber, request));
    }

    /** Monnaie a rendre pour un montant remis en especes · a appeler pendant la saisie, rien n'est enregistre. */
    @GetMapping("/intents/{intentNumber}/cash-change")
    public ResponseEntity<CashChangeResponse> previewIntentCashChange(@PathVariable String intentNumber,
                                                                      @RequestParam BigDecimal tenderedAmount,
                                                                      @RequestParam(required = false) BigDecimal amount) {
        return ResponseEntity.ok(paymentService.previewIntentCashChange(intentNumber, tenderedAmount, amount));
    }

    /** Meme apercu sur une facture, pour le reglement direct au guichet. */
    @GetMapping("/billing-documents/{documentNumber}/cash-change")
    public ResponseEntity<CashChangeResponse> previewDocumentCashChange(@PathVariable String documentNumber,
                                                                        @RequestParam BigDecimal tenderedAmount,
                                                                        @RequestParam(required = false) BigDecimal amount) {
        return ResponseEntity.ok(paymentService.previewDocumentCashChange(documentNumber, tenderedAmount, amount));
    }

    /** Annule une intention en cours · refuse si un paiement mobile money y est encore en attente. */
    @PostMapping("/intents/{intentNumber}/cancel")
    public ResponseEntity<PaymentIntentResponse> cancelIntent(@PathVariable String intentNumber,
                                                              @Valid @RequestBody(required = false) CancelPaymentIntentRequest request) {
        return ResponseEntity.ok(intentLifecycleService.cancel(intentNumber, request));
    }

    /** Rouvre une intention expiree, annulee ou en echec, avec une nouvelle echeance. */
    @PostMapping("/intents/{intentNumber}/relaunch")
    public ResponseEntity<PaymentIntentResponse> relaunchIntent(@PathVariable String intentNumber,
                                                                @RequestBody(required = false) RelaunchPaymentIntentRequest request) {
        return ResponseEntity.ok(intentLifecycleService.relaunch(intentNumber, request));
    }

    @PatchMapping("/intents/{intentNumber}/wallet")
    public ResponseEntity<PaymentTransactionResponse> payWithWallet(@PathVariable String intentNumber,
                                                                    @Valid @RequestBody WalletPaymentRequest request) {
        return ResponseEntity.ok(paymentService.payWithWallet(intentNumber, request));
    }

    @PatchMapping("/intents/{intentNumber}/mobile-money")
    public ResponseEntity<MobileMoneyDepositResponse> initiateMobileMoneyDeposit(
            @PathVariable String intentNumber,
            @Valid @RequestBody InitiateMobileMoneyDepositRequest request) {
        if (StringUtils.hasText(request.intentNumber()) && !intentNumber.equalsIgnoreCase(request.intentNumber().trim())) {
            throw new BadRequestException("Body intentNumber must match path intentNumber");
        }
        return ResponseEntity.ok(paymentService.initiateMobileMoneyDeposit(intentNumber, request));
    }

    @PostMapping("/mobile-money/deposits")
    public ResponseEntity<MobileMoneyDepositResponse> initiateMobileMoneyDeposit(
            @Valid @RequestBody InitiateMobileMoneyDepositRequest request) {
        if (!StringUtils.hasText(request.intentNumber())) {
            throw new BadRequestException("intentNumber is required");
        }
        return ResponseEntity.ok(paymentService.initiateMobileMoneyDeposit(request.intentNumber().trim(), request));
    }

    @PostMapping("/transactions/{transactionNumber}/refund")
    public ResponseEntity<PaymentTransactionResponse> refund(@PathVariable String transactionNumber,
                                                             @Valid @RequestBody RefundPaymentRequest request) {
        return ResponseEntity.ok(paymentService.refund(transactionNumber, request));
    }

    @GetMapping("/transactions/{transactionNumber}/refunds")
    public ResponseEntity<List<PaymentTransactionResponse>> listRefunds(@PathVariable String transactionNumber) {
        return ResponseEntity.ok(paymentService.listRefunds(transactionNumber));
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

    @PostMapping("/intents/{intentNumber}/link")
    public ResponseEntity<PaymentLinkResponse> createPaymentLink(@PathVariable String intentNumber,
                                                                 @RequestBody(required = false) CreatePaymentLinkRequest request) {
        return ResponseEntity.ok(paymentLinkService.createLink(intentNumber, request));
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
