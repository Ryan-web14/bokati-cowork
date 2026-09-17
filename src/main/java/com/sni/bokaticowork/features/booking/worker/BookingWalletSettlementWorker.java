package com.sni.bokaticowork.features.booking.worker;

import com.sni.bokaticowork.features.booking.service.support.BookingWalletSettlementSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Encaisse les blocages de portefeuille arrives a echeance.
 *
 * <p>Chaque blocage est regle dans sa propre transaction : une facture introuvable ou un debit
 * refuse sur une reservation n'empeche pas les suivantes d'etre encaissees.</p>
 *
 * <p>Cadence configurable via {@code bokati.booking.wallet.worker-delay-ms} (defaut : 15 s). Elle
 * n'a pas besoin d'etre plus fine : elle ne fait que borner la derive du delai de reglement, et
 * quelques secondes de retard sur une ecriture que personne n'attend n'ont aucune consequence.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingWalletSettlementWorker {

    private static final int BATCH_SIZE = 100;

    private final BookingWalletSettlementSupport settlementSupport;

    @Scheduled(fixedDelayString = "${bokati.booking.wallet.worker-delay-ms:15000}")
    public void settleDueWalletHolds() {
        List<String> due;
        try {
            due = settlementSupport.dueHoldNumbers(BATCH_SIZE);
        } catch (Exception ex) {
            log.error("BookingWalletSettlementWorker · lecture des blocages echues impossible : {}", ex.getMessage(), ex);
            return;
        }
        if (due.isEmpty()) {
            return;
        }

        int settled = 0;
        for (String holdNumber : due) {
            try {
                settlementSupport.settle(holdNumber);
                settled++;
            } catch (Exception ex) {
                log.warn("Reglement du blocage {} reporte · {}", holdNumber, ex.getMessage());
            }
        }
        log.info("Reglement portefeuille : {} blocage(s) sur {} traite(s)", settled, due.size());
    }
}
