package com.sni.bokaticowork.features.ressource.service.support;

import com.sni.bokaticowork.features.ressource.model.ResourceAvailability;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAvailabilityRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceClosureRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Remet les creneaux a venir a la capacite reelle de leur ressource.
 *
 * <p>Un open space de vingt postes pouvait avoir des creneaux a 1 · la quantite envoyee a la
 * creation etait gardee telle quelle, et une capacite relevee ensuite ne se reportait pas sur les
 * creneaux deja ouverts. Une salle de douze places affichait 1.</p>
 *
 * <p>Ce qui est deja reserve est conserve. Sur une ressource partagee, la part consommee d'un
 * creneau ({@code total - restant}) reste consommee, seule la part libre suit la nouvelle capacite.
 * Sur une piece louee entiere, un creneau est libre (restant egal au total) ou pris · libre, il
 * passe a la capacite de la piece ; pris, il le reste, a zero. Un creneau ferme (fermeture
 * declaree, creneau desactive) le reste.</p>
 *
 * <p>Un lot par transaction, creneaux verrouilles · une reservation qui arrive pendant l'alignement
 * attend, et ne lit jamais un creneau a moitie modifie.</p>
 */
@Slf4j
@Component
public class SharedSlotCapacityAligner {

    static final int BATCH_SIZE = 200;

    private final ResourceAvailabilityRepository availabilityRepository;
    private final ResourceClosureRepository closureRepository;
    private final TransactionTemplate isolatedTransaction;

    public SharedSlotCapacityAligner(ResourceAvailabilityRepository availabilityRepository,
                                     ResourceClosureRepository closureRepository,
                                     PlatformTransactionManager transactionManager) {
        this.availabilityRepository = availabilityRepository;
        this.closureRepository = closureRepository;
        this.isolatedTransaction = new TransactionTemplate(transactionManager);
        this.isolatedTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Aligne tout ce qui doit l'etre · rend le nombre de creneaux modifies. */
    public int alignAll() {
        int aligned = 0;
        while (true) {
            List<Long> ids = availabilityRepository.findMisalignedSlotIds(
                    LocalDateTime.now(), Limit.of(BATCH_SIZE));
            if (ids.isEmpty()) {
                return aligned;
            }
            Integer done = isolatedTransaction.execute(status -> alignBatch(ids));
            int count = done == null ? 0 : done;
            aligned += count;
            if (count == 0) {
                // Rien n'a bouge sur un lot non vide · la requete et l'alignement divergent.
                // On s'arrete plutot que de tourner sur les memes lignes.
                log.warn("Capacite des creneaux · {} creneaux restent hors capacite sans pouvoir etre alignes",
                        ids.size());
                return aligned;
            }
        }
    }

    private int alignBatch(List<Long> ids) {
        List<ResourceAvailability> slots = availabilityRepository.lockAllByIdIn(ids);
        int changed = 0;
        for (ResourceAvailability slot : slots) {
            if (align(slot)) {
                changed++;
            }
        }
        availabilityRepository.saveAll(slots);
        return changed;
    }

    /** Un creneau · {@code false} s'il etait deja juste ou n'est pas a toucher. */
    boolean align(ResourceAvailability slot) {
        if (slot.getResource() == null) {
            return false;
        }
        boolean whole = slot.getResource().isWholeResourceBooking();
        int target = whole
                ? slot.getResource().resolveSeatingCapacity()
                : slot.getResource().resolveBookableSlots();
        int total = slot.getTotalCapacity() == null ? 0 : slot.getTotalCapacity();
        if (target < 1 || target == total) {
            return false;
        }
        int remaining = slot.getRemainingCapacity() == null ? 0 : slot.getRemainingCapacity();
        int newRemaining;
        if (whole) {
            // Une piece se loue entiere · intacte, elle reste libre ; entamee, elle reste prise.
            newRemaining = remaining == total ? target : 0;
        } else {
            int consumed = Math.max(0, total - remaining);
            newRemaining = Math.max(0, target - consumed);
        }

        slot.setTotalCapacity(target);
        slot.setRemainingCapacity(newRemaining);
        slot.setAvailable(newRemaining > 0 && isOpen(slot, remaining));
        return true;
    }

    /**
     * Le creneau peut-il etre propose · on ne rouvre jamais ce qu'une fermeture ou une
     * desactivation a ferme.
     *
     * <p>Un creneau indisponible alors qu'il lui restait de la place a ete ferme pour une autre
     * raison que le remplissage · il reste ferme. Un creneau indisponible parce que plein peut
     * se rouvrir si la capacite augmente, sauf fermeture declaree sur sa plage.</p>
     */
    private boolean isOpen(ResourceAvailability slot, int previousRemaining) {
        if (!Boolean.TRUE.equals(slot.getActive())) {
            return false;
        }
        if (Boolean.TRUE.equals(slot.getAvailable())) {
            return true;
        }
        if (previousRemaining > 0) {
            return false;
        }
        return !closureRepository.existsActiveOverlap(slot.getResource(), slot.getStartedAt(), slot.getEndedAt());
    }
}
