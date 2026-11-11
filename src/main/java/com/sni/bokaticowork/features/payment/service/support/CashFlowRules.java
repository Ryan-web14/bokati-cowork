package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.features.payment.enums.CashFlowDirection;
import com.sni.bokaticowork.features.payment.enums.CashMovementStatus;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;
import com.sni.bokaticowork.features.payment.model.CashMovement;

import java.math.BigDecimal;

/**
 * Regle unique de sens des mouvements de caisse.
 *
 * <p>L'attendu de cloture, le solde courant, le rapport, les statistiques et les etats financiers
 * passent tous par ici (les requetes SQL en reprennent la meme table de verite). Avant, chaque
 * calcul avait la sienne : le solde courant comptait deux fois le fonds d'ouverture, et un
 * ajustement etait ajoute a l'attendu mais ignore par le solde courant.</p>
 *
 * <ul>
 *   <li>Entree : {@code OPENING_FLOAT}, {@code PAYMENT}, {@code CASH_IN}, {@code TRANSFER_IN},
 *       {@code ADJUSTMENT} de sens {@code IN}.</li>
 *   <li>Sortie : {@code REFUND}, {@code CASH_OUT}, {@code SAFE_DEPOSIT}, {@code TRANSFER_OUT},
 *       {@code ADJUSTMENT} de sens {@code OUT}.</li>
 *   <li>Neutre : {@code CLOSING_COUNT}.</li>
 * </ul>
 *
 * <p>Seuls les mouvements {@code CONFIRMED} et {@code REVERSED} comptent : un mouvement contre-passe
 * et sa contre-passation s'annulent, un mouvement en attente ou annule ne touche pas au solde.</p>
 */
public final class CashFlowRules {

    private CashFlowRules() {
    }

    /** Le mouvement entre-t-il dans les soldes ? */
    public static boolean counts(CashMovementStatus status) {
        return status == null || status == CashMovementStatus.CONFIRMED || status == CashMovementStatus.REVERSED;
    }

    /**
     * Sens d'un mouvement : {@code IN}, {@code OUT}, ou {@code null} s'il est neutre.
     *
     * <p>Un ajustement sans sens enregistre (donnee anterieure a la regle) est une entree : c'est
     * ainsi que l'attendu de cloture le traitait, et la migration a pose ce sens sur l'existant.</p>
     */
    public static CashFlowDirection direction(CashMovementType type, CashFlowDirection storedDirection) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case OPENING_FLOAT, PAYMENT, CASH_IN, TRANSFER_IN -> CashFlowDirection.IN;
            case REFUND, CASH_OUT, SAFE_DEPOSIT, TRANSFER_OUT -> CashFlowDirection.OUT;
            case ADJUSTMENT -> storedDirection == CashFlowDirection.OUT ? CashFlowDirection.OUT : CashFlowDirection.IN;
            case CLOSING_COUNT -> null;
        };
    }

    /** Montant signe du mouvement dans le solde, zero s'il est neutre ou ne compte pas. */
    public static BigDecimal signedAmount(CashMovement movement) {
        if (movement == null || movement.getAmount() == null || !counts(movement.getStatus())) {
            return BigDecimal.ZERO;
        }
        CashFlowDirection direction = direction(movement.getMovementType(), movement.getDirection());
        if (direction == null) {
            return BigDecimal.ZERO;
        }
        return direction == CashFlowDirection.OUT ? movement.getAmount().negate() : movement.getAmount();
    }
}
