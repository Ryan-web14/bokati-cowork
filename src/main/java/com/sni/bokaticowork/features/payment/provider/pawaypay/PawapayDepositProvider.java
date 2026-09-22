package com.sni.bokaticowork.features.payment.provider.pawaypay;

import com.sni.bokaticowork.features.payment.provider.MobileMoneyInitiationRequest;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyInitiationResponse;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyPaymentProvider;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyRefundRequest;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyRefundResponse;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyStatusResponse;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayDepositRequest;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayDepositResponse;
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

/**
 * L'adaptateur PawaPay · il traduit, il ne decide pas.
 *
 * <p>Sa seule regle propre : ne jamais transformer une absence de reponse en echec. Un operateur
 * injoignable rend {@code UNKNOWN}, que l'appelant traite comme « a relire », jamais comme « le
 * client n'a pas paye ». Un refus explicite rend {@code FAILED} avec le code de l'operateur et la
 * phrase qu'on dira au client.</p>
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bokati.payment.pawaypay.enabled", havingValue = "true")
public class PawapayDepositProvider implements MobileMoneyPaymentProvider {

    public static final String PAWAYPAY_CALLBACK_PATH = "/payments/mobile-money/pawapay/callback";
    public static final String PAWAYPAY_RETURN_PATH = "/payments/mobile-money/pawapay/return";

    private final PawapayClient client;

    @Override
    public MobileMoneyInitiationResponse initiate(MobileMoneyInitiationRequest request) {
        if (request.correspondent() == null) {
            return new MobileMoneyInitiationResponse(null, "FAILED", "L'opérateur mobile money est requis");
        }
        PawapayDepositRequest depositRequest = new PawapayDepositRequest(
                request.depositId(),
                new PawapayDepositRequest.Payer("MMO",
                        new PawapayDepositRequest.AccountDetails(
                                normalizePhone(request.phoneNumber()),
                                request.correspondent()
                        )),
                formatAmount(request.amount()),
                request.currency(),
                null,
                request.clientReferenceId(),
                request.customerMessage(),
                request.metadata()
        );
        try {
            PawapayDepositResponse response = client.initiateDeposit(depositRequest);
            String status = response == null ? null : response.status();
            // DUPLICATE_IGNORED · le meme depositId a deja ete envoye. C'est exactement ce qu'on
            // veut d'une retentative reseau : l'operateur n'a pas cree de seconde demande.
            if ("ACCEPTED".equals(status) || "DUPLICATE_IGNORED".equals(status)) {
                String providerReference = response.depositId() != null ? response.depositId() : request.depositId();
                return new MobileMoneyInitiationResponse(providerReference, "PROCESSING", "Demande envoyée à l'opérateur");
            }
            PawapayFailureCodes.Explanation explanation = PawapayFailureCodes.explain(
                    response == null ? null : response.rejectionReason());
            log.warn("Depot PawaPay refuse pour l'intention {} · {}", request.intentNumber(), explanation.code());
            return new MobileMoneyInitiationResponse(request.depositId(), "FAILED", explanation.userMessage(),
                    response == null ? null : response.rejectionReason());
        } catch (PawapayClient.ProviderUnreachableException ex) {
            // La demande est peut-etre partie · la relecture de statut tranchera, et NOT_FOUND dira
            // qu'il ne s'est rien passe. Declarer un echec ici ferait payer deux fois le client.
            log.warn("PawaPay injoignable a l'initiation pour l'intention {} · {}", request.intentNumber(), ex.getMessage());
            return new MobileMoneyInitiationResponse(request.depositId(), "UNKNOWN", "En attente de confirmation de l'opérateur");
        } catch (Exception ex) {
            log.error("Initiation PawaPay en erreur pour l'intention {}", request.intentNumber(), ex);
            return new MobileMoneyInitiationResponse(request.depositId(), "UNKNOWN", "En attente de confirmation de l'opérateur");
        }
    }

    @Override
    public MobileMoneyStatusResponse checkStatus(String providerReference) {
        try {
            PawapayClient.DepositStatus status = client.getDepositStatus(providerReference);
            if (!status.found()) {
                return new MobileMoneyStatusResponse(providerReference, "NOT_FOUND",
                        "La demande n'a pas atteint l'opérateur", null, null);
            }
            return new MobileMoneyStatusResponse(providerReference, mapDepositStatus(status.providerStatus()),
                    status.providerStatus(), status.failureReason(), status.providerTransactionId());
        } catch (PawapayClient.ProviderUnreachableException ex) {
            log.warn("Statut PawaPay indisponible pour {} · {}", providerReference, ex.getMessage());
            return new MobileMoneyStatusResponse(providerReference, "UNKNOWN", "Opérateur injoignable", null, null);
        } catch (Exception ex) {
            log.error("Lecture du statut PawaPay en erreur pour {}", providerReference, ex);
            return new MobileMoneyStatusResponse(providerReference, "UNKNOWN", "Statut indisponible", null, null);
        }
    }

    @Override
    public MobileMoneyRefundResponse refund(MobileMoneyRefundRequest request) {
        String refundId = UUID.randomUUID().toString();
        PawapayRefundRequest pawapayRequest = new PawapayRefundRequest(
                refundId,
                request.providerReference(),
                formatAmount(request.amount()),
                request.currency()
        );
        try {
            PawapayRefundResponse response = client.initiateRefund(pawapayRequest);
            String status = response == null ? null : response.status();
            if ("ACCEPTED".equals(status) || "DUPLICATE_IGNORED".equals(status)) {
                return new MobileMoneyRefundResponse(request.providerReference(), refundId,
                        "PROCESSING", "Remboursement envoyé à l'opérateur");
            }
            PawapayFailureCodes.Explanation explanation = PawapayFailureCodes.explain(
                    response == null ? null : response.rejectionReason());
            log.warn("Remboursement PawaPay refuse pour {} · {}", request.providerReference(), explanation.code());
            return new MobileMoneyRefundResponse(request.providerReference(), null, "FAILED", explanation.userMessage());
        } catch (PawapayClient.ProviderUnreachableException ex) {
            log.warn("PawaPay injoignable pour le remboursement de {} · {}", request.providerReference(), ex.getMessage());
            return new MobileMoneyRefundResponse(request.providerReference(), refundId, "UNKNOWN",
                    "En attente de confirmation de l'opérateur");
        } catch (Exception ex) {
            log.error("Remboursement PawaPay en erreur pour {}", request.providerReference(), ex);
            return new MobileMoneyRefundResponse(request.providerReference(), refundId, "UNKNOWN",
                    "En attente de confirmation de l'opérateur");
        }
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private String normalizePhone(String phone) {
        return phone == null ? null : phone.replaceAll("[^0-9]", "");
    }

    /** Un statut qu'on ne connait pas n'est pas un echec · c'est une inconnue, et on relira. */
    private String mapDepositStatus(String pawapayStatus) {
        if (pawapayStatus == null) {
            return "UNKNOWN";
        }
        return switch (pawapayStatus.trim().toUpperCase()) {
            case "COMPLETED", "SUCCESSFUL", "SUCCEEDED" -> "SUCCEEDED";
            case "FAILED", "REJECTED", "EXPIRED", "CANCELLED" -> "FAILED";
            case "NOT_FOUND" -> "NOT_FOUND";
            case "CREATED", "ACCEPTED", "APPROVED", "SUBMITTED", "PROCESSING", "PENDING", "FOUND" -> "PROCESSING";
            default -> "UNKNOWN";
        };
    }
}
