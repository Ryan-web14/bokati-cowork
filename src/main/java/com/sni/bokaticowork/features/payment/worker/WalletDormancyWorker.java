package com.sni.bokaticowork.features.payment.worker;

import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Marque dormants les portefeuilles sans mouvement.
 *
 * <p>La dormance ne ferme rien et ne bloque rien : elle note. Un portefeuille dormant qui bouge
 * se reveille de lui-meme, au premier mouvement, et ce reveil est signale a la revue · c'est la
 * seule consequence, et elle est voulue. Ce que devient un solde dormant a terme est une politique
 * ecrite, pas un automatisme.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletDormancyWorker {

    private final WalletAccountRepository walletRepository;

    @Value("${bokati.wallet.dormancy.after-days:180}")
    private int dormancyDays;

    @Scheduled(cron = "${bokati.wallet.dormancy.cron:0 15 3 * * *}")
    @Transactional
    public void markDormant() {
        Instant before = Instant.now().minus(Duration.ofDays(Math.max(1, dormancyDays)));
        List<Long> candidates = walletRepository.findDormantCandidates(before);
        int marked = 0;
        for (Long id : candidates) {
            WalletAccount wallet = walletRepository.findById(id).orElse(null);
            if (wallet == null || wallet.getDormantSince() != null) {
                continue;
            }
            wallet.setDormantSince(Instant.now());
            walletRepository.save(wallet);
            marked++;
        }
        if (marked > 0) {
            log.info("Portefeuille · {} compte(s) marque(s) dormant(s) apres {} jours sans mouvement", marked, dormancyDays);
        }
    }
}
