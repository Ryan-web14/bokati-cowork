package com.sni.bokaticowork.features.accounting.service.support;

import com.sni.bokaticowork.features.accounting.service.implementation.PurchaseService;
import com.sni.bokaticowork.features.inventory.procurement.event.InventorySupplierSaved;
import com.sni.bokaticowork.features.inventory.procurement.model.Supplier;
import com.sni.bokaticowork.features.inventory.procurement.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Les fournisseurs de l'inventaire remplissent la table des fournisseurs comptables (17/11/2026).
 *
 * <ul>
 *   <li>A chaque creation ou modification d'une fiche d'inventaire, dans la meme transaction : la
 *       fiche comptable liee est creee, ou mise a jour (nom, NIU, adresse, telephone, courriel,
 *       statut). Les champs propres a la comptabilite (compte, delai, retenue) ne sont pas touches.</li>
 *   <li>Au demarrage : les fiches d'inventaire qui n'ont pas encore de fiche comptable en recoivent
 *       une. Idempotent ; chaque fiche dans sa propre transaction.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventorySupplierSync {

    private final SupplierRepository inventorySuppliers;
    private final PurchaseService purchases;
    private final PlatformTransactionManager transactionManager;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onSaved(InventorySupplierSaved event) {
        inventorySuppliers.findById(event.supplierId()).ifPresent(purchases::syncFromInventory);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void backfill() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        int created = 0;
        try {
            for (Supplier supplier : inventorySuppliers.findAll()) {
                try {
                    Boolean done = tx.execute(status -> purchases.syncFromInventory(supplier));
                    if (Boolean.TRUE.equals(done)) created++;
                } catch (RuntimeException ex) {
                    log.warn("Fournisseur d'inventaire {} non repris en comptabilite : {}", supplier.getSupplierCode(), ex.getMessage());
                }
            }
        } catch (RuntimeException ex) {
            log.warn("Reprise des fournisseurs d'inventaire en comptabilite impossible : {}", ex.getMessage());
            return;
        }
        if (created > 0) {
            log.info("{} fournisseur(s) d'inventaire repris en comptabilite", created);
        }
    }
}
