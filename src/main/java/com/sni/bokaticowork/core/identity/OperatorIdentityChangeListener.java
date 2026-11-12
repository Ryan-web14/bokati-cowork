package com.sni.bokaticowork.core.identity;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

/**
 * Invalide l'identite de l'exploitant en cache des qu'une fiche {@code business} change.
 *
 * <p>Pose sur l'entite plutot que dans le service metier · la fiche se modifie par plusieurs
 * chemins (fiche d'entreprise, carte legale, designation de l'exploitant) et un oubli dans l'un
 * d'eux laisserait une identite perimee sur les pieces pendant une minute.</p>
 */
public class OperatorIdentityChangeListener {

    @PostPersist
    @PostUpdate
    @PostRemove
    void changed(Object entity) {
        OperatorIdentityService.invalidate();
    }
}
