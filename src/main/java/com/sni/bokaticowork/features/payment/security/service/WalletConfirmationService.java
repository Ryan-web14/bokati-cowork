package com.sni.bokaticowork.features.payment.security.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.security.enums.WalletChallengeType;
import com.sni.bokaticowork.features.payment.security.enums.WalletConfirmationStatus;
import com.sni.bokaticowork.features.payment.security.enums.WalletOperationType;
import com.sni.bokaticowork.features.payment.security.model.WalletTransactionConfirmation;
import com.sni.bokaticowork.features.payment.security.repository.WalletConfirmationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

/**
 * Demande une confirmation, et la lie a l'operation exacte.
 *
 * <p>Le coeur du mecanisme est l'empreinte. Sans elle, une confirmation serait un simple jeton :
 * on ferait confirmer au titulaire un transfert de mille francs vers un destinataire connu, puis on
 * executerait un transfert de cent mille vers un autre, avec le meme jeton. L'empreinte lie la
 * reponse au montant, au destinataire et a la nature de l'operation · rien ne peut changer entre la
 * demande et la confirmation.</p>
 *
 * <p>L'echeance est courte, quelques minutes. Une confirmation qui traine s'arrache a quelqu'un plus
 * tard, hors du contexte ou il l'avait demandee.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletConfirmationService {

    private final WalletConfirmationRepository confirmationRepository;
    private final WalletSecurityService securityService;
    private final SequenceGeneratorFacade sequenceGenerator;

    @Value("${bokati.wallet.confirmation.validity-minutes:5}")
    private int validityMinutes;

    /** Ce que le titulaire doit confirmer · chaque champ entre dans l'empreinte. */
    public record OperationToConfirm(
            WalletOperationType operationType,
            BigDecimal amount,
            String currency,
            String counterpartyCode,
            String counterpartyLabel
    ) {
    }

    // -----------------------------------------------------------------------------------------

    /**
     * Ouvre une demande de confirmation.
     *
     * <p>Le second canal s'ajoute au code au-dela du seuil configure · un paiement de facture
     * interne peut se contenter du code, un transfert important merite davantage.</p>
     */
    @Transactional
    public WalletTransactionConfirmation request(WalletAccount wallet,
                                                 OperationToConfirm operation,
                                                 String ipAddress,
                                                 String deviceId) {
        WalletSecurityService.SecurityVerdict verdict =
                securityService.evaluate(wallet, operation.operationType(), operation.amount());
        if (verdict.pinMissing()) {
            throw new BadRequestException(verdict.reason());
        }

        WalletChallengeType challenge = verdict.otpRequired()
                ? WalletChallengeType.PIN_AND_OTP
                : WalletChallengeType.PIN;

        return confirmationRepository.save(WalletTransactionConfirmation.builder()
                .confirmationCode(sequenceGenerator.next("wallet_confirmation"))
                .wallet(wallet)
                .operationType(operation.operationType())
                .amount(operation.amount())
                .currency(operation.currency())
                .counterpartyLabel(operation.counterpartyLabel())
                .challengeType(challenge)
                .status(WalletConfirmationStatus.PENDING)
                .payloadHash(fingerprint(wallet, operation))
                .expiresAt(Instant.now().plus(Duration.ofMinutes(validityMinutes)))
                .ipAddress(ipAddress)
                .deviceId(deviceId)
                .build());
    }

    /**
     * Confirme, si le code est bon et si l'operation n'a pas bouge.
     *
     * <p>L'empreinte est recalculee sur l'operation reellement soumise et comparee a celle qui a ete
     * annoncee. Un ecart signifie que ce qu'on execute n'est pas ce qui a ete montre · c'est un
     * refus, et une alerte.</p>
     */
    @Transactional
    public WalletTransactionConfirmation confirm(String confirmationCode,
                                                 OperationToConfirm operation,
                                                 String pin) {
        WalletTransactionConfirmation confirmation = confirmationRepository.findByCode(confirmationCode)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de confirmation introuvable"));

        Instant now = Instant.now();
        if (confirmation.getStatus() != WalletConfirmationStatus.PENDING) {
            throw new BadRequestException("Cette demande de confirmation n'est plus en attente");
        }
        if (!now.isBefore(confirmation.getExpiresAt())) {
            confirmation.setStatus(WalletConfirmationStatus.EXPIRED);
            confirmationRepository.save(confirmation);
            throw new BadRequestException("Cette demande de confirmation a expiré, recommencez l'opération");
        }
        if (confirmation.getAttempts() >= confirmation.getMaxAttempts()) {
            confirmation.setStatus(WalletConfirmationStatus.REJECTED);
            confirmation.setRejectedAt(now);
            confirmationRepository.save(confirmation);
            throw new BadRequestException("Trop de tentatives, recommencez l'opération");
        }

        String submitted = fingerprint(confirmation.getWallet(), operation);
        if (!submitted.equals(confirmation.getPayloadHash())) {
            confirmation.setStatus(WalletConfirmationStatus.REJECTED);
            confirmation.setRejectedAt(now);
            confirmationRepository.save(confirmation);
            log.warn("Confirmation {} refusee · l'operation soumise differe de celle annoncee au titulaire",
                    confirmationCode);
            throw new BadRequestException("L'opération a changé depuis votre demande, recommencez");
        }

        confirmation.setAttempts(confirmation.getAttempts() + 1);
        confirmationRepository.save(confirmation);

        // La verification du code vient apres celle de l'empreinte : inutile de faire saisir un
        // code pour une operation qu'on refusera de toute facon.
        securityService.verifyPin(confirmation.getWallet(), pin);

        confirmation.setStatus(WalletConfirmationStatus.CONFIRMED);
        confirmation.setConfirmedAt(now);
        return confirmationRepository.save(confirmation);
    }

    /** Ferme les demandes echues. Une confirmation qui traine s'arrache hors de son contexte. */
    @Transactional
    public int expireStale() {
        List<WalletTransactionConfirmation> expired = confirmationRepository.findExpired(Instant.now());
        expired.forEach(confirmation -> {
            confirmation.setStatus(WalletConfirmationStatus.EXPIRED);
            confirmationRepository.save(confirmation);
        });
        return expired.size();
    }

    // -----------------------------------------------------------------------------------------

    /**
     * Empreinte de l'operation.
     *
     * <p>Y entrent le portefeuille, la nature de l'operation, le montant, la devise et le
     * destinataire. Tout ce qui, s'il changeait, ferait de l'execution autre chose que ce que le
     * titulaire a vu.</p>
     */
    private String fingerprint(WalletAccount wallet, OperationToConfirm operation) {
        String payload = String.join("|",
                wallet.getWalletNumber(),
                operation.operationType().name(),
                operation.amount() == null ? "" : operation.amount().stripTrailingZeros().toPlainString(),
                StringUtils.hasText(operation.currency()) ? operation.currency().toUpperCase() : "",
                StringUtils.hasText(operation.counterpartyCode()) ? operation.counterpartyCode() : "");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            // SHA-256 est garanti par la plateforme · si elle manque, rien de ce module ne tient.
            throw new IllegalStateException("SHA-256 indisponible", ex);
        }
    }
}
