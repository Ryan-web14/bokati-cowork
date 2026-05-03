package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentLinkRequest;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;
import com.sni.bokaticowork.features.payment.mapper.interfaces.PaymentMapper;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.repository.PaymentIntentRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

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

    @Override
    public PaymentIntentResponse createLink(String intentNumber, CreatePaymentLinkRequest request) {
        PaymentIntent intent = findIntent(intentNumber);
        if (!PAYABLE_STATUSES.contains(intent.getStatus())) {
            throw new BadRequestException("Only pending payment intents can receive a payment link");
        }
        int ttl = request == null || request.expiresInMinutes() == null ? 1440 : request.expiresInMinutes();
        intent.setPaymentLinkToken(UUID.randomUUID().toString().replace("-", ""));
        intent.setPaymentLinkExpiresAt(Instant.now().plus(Math.max(5, ttl), ChronoUnit.MINUTES));
        return mapper.toIntentResponse(intentRepository.save(intent));
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

    private PaymentIntent findIntent(String intentNumber) {
        if (!StringUtils.hasText(intentNumber)) {
            throw new BadRequestException("Payment intent number is required");
        }
        return intentRepository.findByIntentNumber(intentNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Payment intent not found"));
    }
}
