package com.sni.bokaticowork.features.payment.integrity.service;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.model.WalletLedgerEntry;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.features.payment.repository.WalletLedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Verifie que le journal dit encore ce qu'il disait.
 *
 * <p>Deux controles independants, et c'est leur independance qui fait leur valeur :</p>
 *
 * <ol>
 *   <li><b>La chaine</b> · chaque ecriture porte l'empreinte de la precedente. Une ligne modifiee,
 *       inseree ou supprimee rompt le maillon suivant.</li>
 *   <li><b>Le solde</b> · la somme signee des ecritures doit redonner le solde comptable affiche.
 *       Une ligne supprimee proprement, empreintes recalculees, laisserait la chaine intacte mais
 *       creuserait l'ecart ici.</li>
 * </ol>
 *
 * <p>Falsifier le journal suppose donc de tromper les deux a la fois, ce qui demande de reecrire
 * toute la suite de la chaine <em>et</em> d'ajuster le solde du compte · aucune modification ponctuelle
 * ne passe.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletIntegrityService {

    private final WalletAccountRepository walletRepository;
    private final WalletLedgerEntryRepository ledgerRepository;

    /**
     * @param entriesChecked  nombre d'ecritures parcourues
     * @param brokenLinks     references des ecritures dont le maillon ne tient plus
     * @param unchained       ecritures anterieures au chainage · ni valides ni suspectes
     * @param balanceDrift    ecart entre le solde recalcule et le solde affiche
     */
    public record IntegrityReport(
            String walletNumber,
            int entriesChecked,
            List<String> brokenLinks,
            int unchained,
            BigDecimal expectedBalance,
            BigDecimal actualBalance,
            BigDecimal balanceDrift
    ) {

        public boolean intact() {
            return brokenLinks.isEmpty() && balanceDrift.signum() == 0;
        }
    }

    /** Verifie un portefeuille de bout en bout. */
    @Transactional(readOnly = true)
    public IntegrityReport verify(String walletNumber) {
        WalletAccount wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Portefeuille introuvable"));

        List<WalletLedgerEntry> entries = ledgerRepository.findChain(wallet.getId());
        List<String> broken = new ArrayList<>();
        String expectedPrevious = WalletLedgerChain.GENESIS;
        int unchained = 0;

        for (WalletLedgerEntry entry : entries) {
            if (entry.getCurrentHash() == null) {
                // Ecriture anterieure a la mise en place du chainage. La signaler comme rompue
                // ferait crier au loup sur tout l'historique existant · on la compte a part, et le
                // maillon attendu repart de celle-ci pour ne pas propager l'interruption.
                unchained++;
                expectedPrevious = WalletLedgerChain.GENESIS;
                continue;
            }
            String recomputed = WalletLedgerChain.hash(entry, entry.getPreviousHash());
            boolean linkHolds = expectedPrevious.equals(entry.getPreviousHash())
                    || WalletLedgerChain.GENESIS.equals(entry.getPreviousHash());
            if (!recomputed.equals(entry.getCurrentHash()) || !linkHolds) {
                broken.add(entry.getEntryNumber());
            }
            expectedPrevious = entry.getCurrentHash();
        }

        BigDecimal expected = nonNull(ledgerRepository.sumLedgerImpact(wallet.getId()));
        BigDecimal actual = nonNull(wallet.getLedgerBalance());
        BigDecimal drift = expected.subtract(actual);

        IntegrityReport report = new IntegrityReport(wallet.getWalletNumber(), entries.size(),
                broken, unchained, expected, actual, drift);
        if (!report.intact()) {
            log.error("Portefeuille {} · integrite compromise, {} maillon(s) rompu(s), ecart de solde {}",
                    wallet.getWalletNumber(), broken.size(), drift);
        }
        return report;
    }

    private BigDecimal nonNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
