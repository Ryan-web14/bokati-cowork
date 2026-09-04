package com.sni.bokaticowork.features.ressource.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.ressource.model.Resource;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Source unique de la duree de creneau d'une ressource.
 *
 * <p>Cette duree etait une constante {@code SLOT_MINUTES = 30}, redefinie a l'identique dans
 * {@code ResourceAvailabilityServiceImpl} et {@code BookingResourceGuard}. Deux sources de verite
 * pour une meme regle : les faire diverger d'une valeur suffisait a rendre reservable une plage
 * pour laquelle aucun creneau n'existe, ou l'inverse. Elles sont remplacees par ce composant.
 *
 * <p>La duree appartient a la <b>ressource</b>, jamais a une disponibilite prise isolement. Le
 * moteur compose une fenetre reservable a partir de creneaux contigus
 * ({@code requiredSlots = duree / dureeDeCreneau}) : des creneaux de durees differentes sur une
 * meme ressource fausseraient ce decompte, et silencieusement · on renverrait des fenetres de la
 * mauvaise longueur, pas une erreur.
 */
@Component
public class ResourceSlotPolicy {

    /** Valeur retenue quand la ressource n'en porte aucune · celle qui prevalait partout. */
    public static final int DEFAULT_SLOT_MINUTES = 30;

    /**
     * Duree de creneau de la ressource. Jamais nulle : une ressource anterieure a la migration,
     * ou construite en memoire, retombe sur la valeur historique.
     */
    public int slotMinutes(Resource resource) {
        if (resource == null || resource.getSlotDurationMinutes() == null) {
            return DEFAULT_SLOT_MINUTES;
        }
        return resource.getSlotDurationMinutes();
    }

    /**
     * Valide une duree proposee.
     *
     * <p>Seuls les diviseurs de 60 sont admis. Une duree de 7 ou de 45 minutes casse l'alignement
     * des creneaux sur l'heure : la journee ne se decoupe plus en tranches regulieres, et une
     * partie des heures devient inaccessible a la reservation sans que rien ne l'explique.
     */
    public void assertAcceptable(Integer slotMinutes) {
        if (slotMinutes == null) {
            return;
        }
        if (slotMinutes <= 0 || slotMinutes > 60 || 60 % slotMinutes != 0) {
            throw new BadRequestException(
                    "La duree de creneau doit diviser 60 · valeurs admises : 5, 10, 15, 20, 30, 60. Recue : "
                            + slotMinutes);
        }
    }

    /**
     * Verifie qu'une plage tombe exactement sur les creneaux de la ressource.
     *
     * <p>Le message nomme la duree effective plutot que « 30 minutes » en dur : sur une ressource
     * en creneaux de 15, un refus mentionnant 30 enverrait chercher au mauvais endroit.
     */
    public void assertAligned(Resource resource, LocalDateTime startedAt, LocalDateTime endedAt) {
        int minutes = slotMinutes(resource);
        long span = Duration.between(startedAt, endedAt).toMinutes();
        if (span % minutes != 0
                || startedAt.getMinute() % minutes != 0
                || endedAt.getMinute() % minutes != 0) {
            throw new BadRequestException(
                    "La plage doit etre alignee sur des creneaux de " + minutes + " minutes");
        }
    }

    /** Nombre de creneaux couvrant une duree · le decompte au coeur de la composition de fenetre. */
    public int slotCount(Resource resource, long durationMinutes) {
        return (int) (durationMinutes / slotMinutes(resource));
    }
}
