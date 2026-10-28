package com.sni.bokaticowork.features.payment.service.pawaypay;

import com.sni.bokaticowork.features.payment.model.PawapayCallback;
import com.sni.bokaticowork.features.payment.repository.PawapayCallbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Le rappel arrive parfois avant que notre depot soit enregistre · on le reprend des que c'est fait.
 *
 * <p>L'appel a PawaPay part pendant la transaction qui cree le depot, et l'appelant (portail,
 * facturation, rechargement) l'englobe souvent dans la sienne. Un client rapide valide sur son
 * telephone, PawaPay rappelle, et notre base ne connait pas encore le depot : le rappel etait
 * marque traite sans rien faire, et le paiement attendait la relecture suivante.</p>
 *
 * <p>Le rappel est desormais mis en attente, et sitot la transaction validee, on relit le statut du
 * depot s'il en a recu un. Si ce relais echoue, la relecture programmee prend le relais une minute
 * plus tard · rien n'est perdu, c'est seulement plus lent.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EarlyCallbackRelay {

    public static final String EARLY_DETAIL = "Depot pas encore enregistre a la reception · relu des son enregistrement";

    private final PawapayCallbackRepository callbackRepository;
    private final ObjectProvider<MobileMoneySupervisionService> supervision;

    /** A appeler une fois le depot cree · agit apres la validation de la transaction en cours. */
    public void afterCommit(String depositId) {
        if (depositId == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            relay(depositId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                relay(depositId);
            }
        });
    }

    void relay(String depositId) {
        try {
            if (callbackRepository.existsByKindAndReferenceIdAndOutcome(
                    PawapayCallback.Kind.DEPOSIT, depositId, PawapayCallback.Outcome.DEFERRED)) {
                log.info("Rappel recu avant l'enregistrement du depot {} · statut relu maintenant", depositId);
                supervision.getObject().recheck(depositId);
            }
        } catch (Exception ex) {
            log.warn("Relecture du depot {} apres un rappel precoce impossible · la relecture programmee reprendra : {}",
                    depositId, ex.getMessage());
        }
    }
}
