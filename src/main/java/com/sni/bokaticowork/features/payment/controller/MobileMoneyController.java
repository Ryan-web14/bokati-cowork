package com.sni.bokaticowork.features.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentRequest;
import com.sni.bokaticowork.features.payment.dto.request.InitiateMobileMoneyDepositRequest;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyProviderOptionResponse;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyTestDepositResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.provider.pawaypay.CongoCorrespondent;
import com.sni.bokaticowork.features.payment.provider.pawaypay.PawapaySignatureVerifier;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayCallbackPayload;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayRefundCallbackPayload;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import com.sni.bokaticowork.features.payment.service.pawaypay.PawapayCallbackProcessor;
import com.sni.bokaticowork.features.payment.service.pawaypay.PawapayDepositService;
import com.sni.bokaticowork.features.payment.service.pawaypay.PawapayRefundCallbackProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(ApiPath.V1 + "/payments/mobile-money")
@RequiredArgsConstructor
public class MobileMoneyController {

    private final PaymentService paymentService;
    private final PawapayDepositService depositService;
    private final PawapayCallbackProcessor callbackProcessor;
    private final PawapayRefundCallbackProcessor refundCallbackProcessor;
    private final PawapaySignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;

    @GetMapping("/providers")
    public ResponseEntity<List<MobileMoneyProviderOptionResponse>> providers() {
        return ResponseEntity.ok(depositService.providers());
    }

    @GetMapping("/deposits/{depositId}")
    public ResponseEntity<MobileMoneyDepositResponse> getDeposit(@PathVariable String depositId) {
        return ResponseEntity.ok(depositService.get(depositId));
    }

    @PostMapping("/pawaypay/test/{phoneNumber}")
    public ResponseEntity<MobileMoneyTestDepositResponse> testDeposit(@PathVariable String phoneNumber) {
        String normalizedPhone = phoneNumber == null ? "" : phoneNumber.replaceAll("[^\\d]", "");
        String testRun = String.valueOf(Instant.now().toEpochMilli());
        String testCode = "MM-TEST-" + normalizedPhone + "-" + testRun;
        PaymentIntentResponse intent = paymentService.createIntent(new CreatePaymentIntentRequest(
                "CUSTOMER",
                testCode,
                BigDecimal.TEN,
                "XAF",
                "Bokati test " + testRun.substring(Math.max(0, testRun.length() - 6)),
                "MOBILE_MONEY_TEST",
                normalizedPhone + "-" + testRun,
                "MM-TEST-" + testRun,
                Instant.now().plus(15, ChronoUnit.MINUTES),
                writeJson(Map.of(
                        "test", true,
                        "testRun", testRun,
                        "phoneNumber", normalizedPhone,
                        "provider", CongoCorrespondent.MTN_MOMO_COG.providerCode()
                ))
        ));
        MobileMoneyDepositResponse deposit = paymentService.initiateMobileMoneyDeposit(
                intent.intentNumber(),
                new InitiateMobileMoneyDepositRequest(
                        intent.intentNumber(),
                        normalizedPhone,
                        CongoCorrespondent.MTN_MOMO_COG,
                        BigDecimal.TEN,
                        "backend-test",
                        null
                )
        );
        return ResponseEntity.ok(new MobileMoneyTestDepositResponse(
                deposit,
                paymentService.getIntent(intent.intentNumber()),
                depositService.providers()
        ));
    }

    /**
     * Webhook called by PawaPay when a deposit changes status (COMPLETED or FAILED).
     * Always returns 200 — PawaPay retries on non-2xx responses.
     */
    @PostMapping({"/pawapay/callback", "/pawaypay/callback"})
    public ResponseEntity<Void> depositCallback(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-PawaPay-Signature", required = false) String signature) {

        log.info("PawaPay deposit callback received");

        if (!signatureVerifier.verify(rawBody, signature)) {
            log.warn("PawaPay deposit callback rejected — invalid signature");
            return ResponseEntity.ok().build();
        }

        try {
            PawapayCallbackPayload payload = objectMapper.readValue(rawBody, PawapayCallbackPayload.class);
            log.info("PawaPay deposit callback: depositId={}, status={}", payload.depositId(), payload.status());
            callbackProcessor.process(payload);
        } catch (Exception ex) {
            log.error("Error processing PawaPay deposit callback", ex);
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Webhook called by PawaPay when a refund changes status (COMPLETED or FAILED).
     * Always returns 200.
     */
    @PostMapping({"/pawapay/refund-callback", "/pawaypay/refund-callback"})
    public ResponseEntity<Void> refundCallback(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-PawaPay-Signature", required = false) String signature) {

        log.info("PawaPay refund callback received");

        if (!signatureVerifier.verify(rawBody, signature)) {
            log.warn("PawaPay refund callback rejected — invalid signature");
            return ResponseEntity.ok().build();
        }

        try {
            PawapayRefundCallbackPayload payload = objectMapper.readValue(rawBody, PawapayRefundCallbackPayload.class);
            log.info("PawaPay refund callback: refundId={}, status={}", payload.refundId(), payload.status());
            refundCallbackProcessor.process(payload);
        } catch (Exception ex) {
            log.error("Error processing PawaPay refund callback", ex);
        }
        return ResponseEntity.ok().build();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }
}
