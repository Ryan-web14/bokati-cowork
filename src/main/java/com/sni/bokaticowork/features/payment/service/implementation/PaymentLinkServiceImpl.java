package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentLinkRequest;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentLinkResponse;
import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;
import com.sni.bokaticowork.features.payment.mapper.interfaces.PaymentMapper;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.repository.PaymentIntentRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentLinkService;
import com.sni.bokaticowork.features.payment.service.pawaypay.PawapayPaymentPageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PaymentLinkServiceImpl implements PaymentLinkService {

    private static final Set<PaymentIntentStatus> PAYABLE_STATUSES = Set.of(
            PaymentIntentStatus.PENDING,
            PaymentIntentStatus.PROCESSING,
            PaymentIntentStatus.AUTHORIZED
    );

    private final PaymentIntentRepository intentRepository;
    private final PaymentMapper mapper;
    private final DefaultEmailSender emailSender;
    private final ObjectProvider<PawapayPaymentPageService> paymentPageServiceProvider;

    @Override
    public PaymentLinkResponse createLink(String intentNumber, CreatePaymentLinkRequest request) {
        PaymentIntent intent = findIntent(intentNumber);
        if (!PAYABLE_STATUSES.contains(intent.getStatus())) {
            throw new BadRequestException("Only pending payment intents can receive a payment link");
        }

        PawapayPaymentPageService pageService = paymentPageServiceProvider.getIfAvailable();
        if (pageService == null) {
            throw new BadRequestException("Mobile money checkout is not enabled");
        }

        PaymentIntentResponse view = mapper.toIntentResponse(intent);
        BigDecimal amount = view.remainingAmount();
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Payment intent has no outstanding amount to collect");
        }

        PawapayPaymentPageService.Checkout checkout = pageService.createCheckout(intent, amount);

        int ttl = request == null || request.expiresInMinutes() == null ? 1440 : request.expiresInMinutes();
        intent.setPaymentLinkToken(UUID.randomUUID().toString().replace("-", ""));
        intent.setPaymentLinkExpiresAt(Instant.now().plus(Math.max(5, ttl), ChronoUnit.MINUTES));
        intentRepository.save(intent);

        String recipientEmail = view.customerEmail();
        if (StringUtils.hasText(recipientEmail)) {
            // Send only after the transaction commits · otherwise a rollback would still
            // email a checkout URL whose backing transaction/deposit no longer exists.
            registerAfterCommitEmail(recipientEmail, view.customerName(), intent, amount, checkout.redirectUrl());
        } else {
            log.warn("No email address for intent {} · payment link created but not emailed", intentNumber);
        }

        return new PaymentLinkResponse(
                intent.getIntentNumber(),
                checkout.redirectUrl(),
                checkout.depositId(),
                checkout.transactionNumber(),
                intent.getPaymentLinkToken(),
                intent.getPaymentLinkExpiresAt(),
                StringUtils.hasText(recipientEmail) ? recipientEmail : null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentIntentResponse resolve(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BadRequestException("Payment link token is required");
        }
        PaymentIntent intent = intentRepository.findByPaymentLinkToken(token.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Payment link not found"));
        if (intent.getPaymentLinkExpiresAt() != null && intent.getPaymentLinkExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Payment link has expired");
        }
        if (!PAYABLE_STATUSES.contains(intent.getStatus())) {
            throw new BadRequestException("Payment link is no longer payable");
        }
        return mapper.toIntentResponse(intent);
    }

    private void registerAfterCommitEmail(String recipientEmail,
                                          String recipientName,
                                          PaymentIntent intent,
                                          BigDecimal amount,
                                          String checkoutUrl) {
        String subject = "Lien de paiement · " + intent.getIntentNumber();
        String html = buildEmailHtml(recipientName, amount, intent.getCurrency(), checkoutUrl);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendEmail(recipientEmail, subject, html, intent.getIntentNumber());
                }
            });
        } else {
            sendEmail(recipientEmail, subject, html, intent.getIntentNumber());
        }
    }

    private void sendEmail(String recipientEmail, String subject, String html, String intentNumber) {
        try {
            emailSender.sendHtmlEmail(recipientEmail, subject, html);
            log.info("Payment link emailed to {} for intent {}", recipientEmail, intentNumber);
        } catch (Exception ex) {
            log.warn("Failed to email payment link for intent {} to {}: {}", intentNumber, recipientEmail, ex.getMessage());
        }
    }

    private String buildEmailHtml(String recipientName, BigDecimal amount, String currency, String checkoutUrl) {
        String name = StringUtils.hasText(recipientName) ? recipientName : "client";
        String formattedAmount = amount.stripTrailingZeros().toPlainString();
        return "<p>Bonjour " + name + ",</p>"
                + "<p>Vous pouvez régler votre paiement d'un montant de <strong>"
                + formattedAmount + " " + currency + "</strong> en cliquant sur le lien sécurisé ci-dessous :</p>"
                + "<p><a href=\"" + checkoutUrl + "\" style=\"display:inline-block;padding:12px 20px;"
                + "background:#b8860b;color:#ffffff;text-decoration:none;border-radius:6px;\">Régler maintenant</a></p>"
                + "<p>Si le bouton ne fonctionne pas, copiez ce lien dans votre navigateur :<br/>"
                + "<a href=\"" + checkoutUrl + "\">" + checkoutUrl + "</a></p>"
                + "<p>Ce lien de paiement expirera prochainement.</p>"
                + "<p>Merci,<br/>Elle A Osé</p>";
    }

    private PaymentIntent findIntent(String intentNumber) {
        if (!StringUtils.hasText(intentNumber)) {
            throw new BadRequestException("Payment intent number is required");
        }
        return intentRepository.findByIntentNumber(intentNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Payment intent not found"));
    }
}
