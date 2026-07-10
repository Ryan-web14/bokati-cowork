package com.sni.bokaticowork.features.payment.service.pawaypay;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.utils.code.CodeComposer;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.provider.pawaypay.PawapayClient;
import com.sni.bokaticowork.features.payment.provider.pawaypay.PawapayDepositProvider;
import com.sni.bokaticowork.features.payment.provider.pawaypay.PawapayProperties;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayPaymentPageRequest;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayPaymentPageResponse;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Creates a PawaPay hosted Payment Page (checkout) session for a payment intent.
 * A PROCESSING mobile money transaction carrying the generated depositId as its
 * provider reference is persisted up front so the existing deposit callback can
 * settle the intent once the customer completes the payment on PawaPay's page.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bokati.payment.pawaypay.enabled", havingValue = "true")
public class PawapayPaymentPageService {

    private static final int REASON_MAX_LENGTH = 100;

    private final PawapayClient client;
    private final PawapayProperties properties;
    private final PaymentTransactionRepository transactionRepository;
    private final SequenceGeneratorFacade sequenceGenerator;

    @Transactional
    public Checkout createCheckout(PaymentIntent intent, BigDecimal amount) {
        String depositId = UUID.randomUUID().toString();

        String methodCtx = CodeComposer.abbrev("MOBILE_MONEY");
        long txnSeq = CodeComposer.extractSeq(sequenceGenerator.next("payment_transaction"));
        String txnNumber = CodeComposer.withDay("TXN", methodCtx, LocalDate.now(), txnSeq);

        transactionRepository.save(PaymentTransaction.builder()
                .transactionNumber(txnNumber)
                .paymentIntent(intent)
                .paymentMethod(PaymentMethod.MOBILE_MONEY)
                .provider("PAWAYPAY")
                .providerReference(depositId)
                .amount(amount)
                .currency(intent.getCurrency())
                .status(PaymentTransactionStatus.PROCESSING)
                .build());

        String returnUrl = properties.getCallbackBaseUrl()
                + ApiPath.V1
                + PawapayDepositProvider.PAWAYPAY_RETURN_PATH
                + "?depositId=" + depositId;

        PawapayPaymentPageResponse response = client.createPaymentPage(new PawapayPaymentPageRequest(
                depositId,
                returnUrl,
                amount.setScale(0, RoundingMode.HALF_UP).toPlainString(),
                properties.getPaymentPageCountry(),
                reason(intent),
                properties.getPaymentPageLanguage()
        ));

        if (response == null || !StringUtils.hasText(response.redirectUrl())) {
            throw new IllegalStateException("PawaPay did not return a payment page URL for intent "
                    + intent.getIntentNumber());
        }

        log.info("PawaPay payment page created for intent {} — depositId={}, transaction={}",
                intent.getIntentNumber(), depositId, txnNumber);
        return new Checkout(depositId, txnNumber, response.redirectUrl());
    }

    private String reason(PaymentIntent intent) {
        String reason = StringUtils.hasText(intent.getPurpose())
                ? intent.getPurpose().trim()
                : "Paiement " + intent.getIntentNumber();
        return reason.length() <= REASON_MAX_LENGTH ? reason : reason.substring(0, REASON_MAX_LENGTH).trim();
    }

    public record Checkout(String depositId, String transactionNumber, String redirectUrl) {
    }
}
