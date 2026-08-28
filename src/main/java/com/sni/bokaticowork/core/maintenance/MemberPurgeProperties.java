package com.sni.bokaticowork.core.maintenance;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Interrupteur et garde-fous de la purge par membre.
 *
 * <p>La purge supprime physiquement et definitivement des donnees, y compris des documents
 * fiscalement scelles. Elle est donc desactivee par defaut : il faut l'allumer explicitement,
 * le temps du nettoyage, puis l'eteindre. Tant que {@code enabled} est faux, le service refuse
 * la suppression mais laisse passer la previsualisation, qui est en lecture seule.
 */
@Data
@Component
@ConfigurationProperties(prefix = "bokati.maintenance.member-purge")
public class MemberPurgeProperties {

    /** Autorise la suppression effective. La previsualisation reste disponible meme a false. */
    private boolean enabled = true;

    /** Nombre maximum de membres qu'un seul appel peut purger. */
    private int maxBatchSize = 25;

    /**
     * Profondeur maximale de descente dans le graphe de cles etrangeres. Sert de garde-fou :
     * une chaine plus profonde signale un cycle non detecte plutot qu'un schema legitime.
     */
    private int maxDepth = 12;
}
