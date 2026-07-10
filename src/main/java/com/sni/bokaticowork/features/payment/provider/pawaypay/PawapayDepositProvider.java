package com.sni.bokaticowork.features.payment.provider.pawaypay;

import com.sni.bokaticowork.features.payment.provider.MobileMoneyInitiationRequest;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyInitiationResponse;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyPaymentProvider;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyRefundRequest;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyRefundResponse;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyStatusResponse;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayDepositRequest;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayDepositResponse;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayDepositStatusResponse;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayRefundRequest;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayRefundResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bokati.payment.pawaypay.enabled", havingValue = "true")
public class PawapayDepositProvider implements MobileMoneyPaymentProvider {

    public static final String PAWAYPAY_CALLBACK_PATH = "/payments/mobile-money/pawapay/callback";
    public static final String PAWAYPAY_RETURN_PATH = "/payments/mobile-money/pawapay/return";

    private final PawapayClient client;
    private final PawapayProperties properties;

    @Override
    public MobileMoneyInitiationResponse initiate(MobileMoneyInitiationRequest request) {
        if (request.correspondent() == null) {
            return new MobileMoneyInitiationResponse(null, "FAILED", "correspondent is required");
        }

        PawapayDepositRequest depositRequest = new PawapayDepositRequest(
                request.depositId(),
                new PawapayDepositRequest.Payer("MMO",
                        new PawapayDepositRequest.AccountDetails(
                                normalizePhone(request.phoneNumber()),
                                request.correspondent()
                        )),
                formatAmount(request),
                request.currency(),
                null,
                request.clientReferenceId(),
                request.customerMessage(),
                request.metadata()
        );

        try {
            PawapayDepositResponse response = client.initiateDeposit(depositRequest);
            if ("ACCEPTED".equals(response.status())) {
                String providerReference = response.depositId() != null ? response.depositId() : request.depositId();
                return new MobileMoneyInitiationResponse(providerReference, "PROCESSING",
                        "Deposit initiated successfully");
            }
            String reason = response.rejectionReason() != null
                    ? response.rejectionReason().toString()
                    : "Rejected by PawaPay";
            log.warn("PawaPay deposit rejected for intent {}: {}", request.intentNumber(), reason);
            return new MobileMoneyInitiationResponse(request.depositId(), "FAILED", reason);
        } catch (Exception ex) {
            log.error("PawaPay deposit initiation failed for intent {}", request.intentNumber(), ex);
            return new MobileMoneyInitiationResponse(request.depositId(), "FAILED",
                    "Provider communication error: " + ex.getMessage());
        }
    }

    @Override
    public MobileMoneyStatusResponse checkStatus(String providerReference) {
        try {
            PawapayDepositStatusResponse response = client.getDepositStatus(providerReference);
            String mappedStatus = mapDepositStatus(response.status());
            return new MobileMoneyStatusResponse(providerReference, mappedStatus, response.status());
        } catch (Exception ex) {
            log.error("PawaPay status check failed for deposit {}", providerReference, ex);
            return new MobileMoneyStatusResponse(providerReference, "FAILED",
                    "Provider communication error");
        }
    }

    @Override
    public MobileMoneyRefundResponse refund(MobileMoneyRefundRequest request) {
        String refundId = UUID.randomUUID().toString();
        PawapayRefundRequest pawapayRequest = new PawapayRefundRequest(
                refundId,
                request.providerReference(),
                formatBigDecimal(request.amount()),
                request.currency()
        );
        try {
            PawapayRefundResponse response = client.initiateRefund(pawapayRequest);
            if ("ACCEPTED".equals(response.status())) {
                return new MobileMoneyRefundResponse(request.providerReference(), refundId,
                        "PROCESSING", "Refund initiated successfully");
            }
            String reason = response.rejectionReason() != null
                    ? response.rejectionReason().toString()
                    : "Rejected by PawaPay";
            log.warn("PawaPay refund rejected for depositId {}: {}", request.providerReference(), reason);
            return new MobileMoneyRefundResponse(request.providerReference(), null, "FAILED", reason);
        } catch (Exception ex) {
            log.error("PawaPay refund initiation failed for depositId {}", request.providerReference(), ex);
            return new MobileMoneyRefundResponse(request.providerReference(), null, "FAILED",
                    "Provider communication error: " + ex.getMessage());
        }
    }

    private String formatAmount(MobileMoneyInitiationRequest request) {
        return request.amount()
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private String formatBigDecimal(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        return phone.replaceAll("[^\\d]", "");
    }

    private String mapDepositStatus(String pawapayStatus) {
        if (pawapayStatus == null) return "FAILED";
        return switch (pawapayStatus.trim().toUpperCase()) {
            case "COMPLETED", "SUCCESSFUL", "SUCCEEDED" -> "SUCCEEDED";
            case "FAILED", "REJECTED", "EXPIRED" -> "FAILED";
            case "CREATED", "ACCEPTED", "APPROVED", "SUBMITTED", "PROCESSING", "PENDING" -> "PROCESSING";
            default -> "FAILED";
        };
    }
}
