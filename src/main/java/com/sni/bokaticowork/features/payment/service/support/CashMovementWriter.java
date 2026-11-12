package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.enums.CashDocumentType;
import com.sni.bokaticowork.features.payment.enums.CashFlowDirection;
import com.sni.bokaticowork.features.payment.enums.CashMovementStatus;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;
import com.sni.bokaticowork.features.payment.enums.CashRegisterType;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.model.CashMovement;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.repository.CashMovementRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

/**
 * Ecriture des mouvements produits par les operations internes (lot C3) : transferts,
 * contre-passations, echanges. Le solde courant suit la regle unique de sens et se recalcule pour
 * toute la session quand un mouvement change de statut (visa, annulation, contre-passation).
 */
@Component
@RequiredArgsConstructor
public class CashMovementWriter {

    public static final String SAFE_ACTOR = "SAFE";

    private final CashMovementRepository movementRepository;
    private final CashSessionRepository sessionRepository;
    private final SequenceGeneratorFacade sequenceGenerator;

    @Builder
    public record Spec(CashSession session, CashMovementType type, BigDecimal amount, String currency,
                       CashDocumentType documentType, String documentNumber, String flowCategory, String referenceType,
                       String referenceCode, String counterpartyType, String counterpartyCode, String counterpartyName,
                       String reason, String createdBy, String metadataJson, CashFlowDirection direction,
                       CashMovementStatus status, String transferOrderNumber, String natureCode, Long relatedMovementId) {
    }

    public CashMovement write(Spec spec) {
        CashMovement movement = CashMovement.builder()
                .movementNumber(sequenceGenerator.next("cash_movement"))
                .cashSession(spec.session())
                .movementType(spec.type())
                .amount(spec.amount().setScale(4, RoundingMode.HALF_UP))
                .currency(StringUtils.hasText(spec.currency()) ? spec.currency().trim().toUpperCase(Locale.ROOT) : "XAF")
                .documentType(spec.documentType())
                .documentNumber(spec.documentNumber())
                .flowCategory(spec.flowCategory())
                .referenceType(spec.referenceType())
                .referenceCode(spec.referenceCode())
                .counterpartyType(spec.counterpartyType())
                .counterpartyCode(spec.counterpartyCode())
                .counterpartyName(spec.counterpartyName())
                .reason(spec.reason())
                .createdBy(spec.createdBy())
                .metadataJson(spec.metadataJson())
                .direction(spec.direction())
                .status(spec.status() == null ? CashMovementStatus.CONFIRMED : spec.status())
                .transferOrderNumber(spec.transferOrderNumber())
                .natureCode(spec.natureCode())
                .relatedMovementId(spec.relatedMovementId())
                .build();
        movement.setRunningBalance(balanceAfter(movement));
        return movementRepository.save(movement);
    }

    /** Recalcule le solde courant de tous les mouvements d'une session, dans l'ordre. */
    public void recompute(CashSession session) {
        if (session == null || session.getId() == null) {
            return;
        }
        BigDecimal balance = BigDecimal.ZERO;
        List<CashMovement> movements = movementRepository.findByCashSession_IdOrderByCreatedAtAsc(session.getId());
        for (CashMovement movement : movements) {
            balance = balance.add(CashFlowRules.signedAmount(movement));
            BigDecimal scaled = balance.setScale(4, RoundingMode.HALF_UP);
            if (movement.getRunningBalance() == null || movement.getRunningBalance().compareTo(scaled) != 0) {
                movement.setRunningBalance(scaled);
                movementRepository.save(movement);
            }
        }
    }

    private BigDecimal balanceAfter(CashMovement movement) {
        CashSession session = movement.getCashSession();
        if (session == null || session.getId() == null) {
            return null;
        }
        BigDecimal balance = BigDecimal.ZERO;
        for (CashMovement entry : movementRepository.findByCashSession_IdOrderByCreatedAtAsc(session.getId())) {
            balance = balance.add(CashFlowRules.signedAmount(entry));
        }
        return balance.add(CashFlowRules.signedAmount(movement)).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Session technique d'un coffre : un coffre n'a pas de session de caissier, ses mouvements
     * dates sont ranges dans une session permanente ouverte au nom du coffre.
     */
    public CashSession safeSession(CashRegister safe) {
        if (safe.getRegisterType() != CashRegisterType.SAFE) {
            throw new IllegalArgumentException("Pas un coffre : " + safe.getRegisterCode());
        }
        return sessionRepository.findBlockingSessionByCashRegisterId(safe.getId())
                .orElseGet(() -> sessionRepository.save(CashSession.builder()
                        .sessionNumber(sequenceGenerator.next("cash_session"))
                        .cashRegister(safe)
                        .status(CashSessionStatus.OPEN)
                        .openedBy(SAFE_ACTOR)
                        .openingAmount(safe.getLastCountAmount() == null ? BigDecimal.ZERO.setScale(4)
                                : safe.getLastCountAmount().setScale(4, RoundingMode.HALF_UP))
                        .build()));
    }
}
