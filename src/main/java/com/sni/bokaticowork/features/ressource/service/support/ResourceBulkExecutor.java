package com.sni.bokaticowork.features.ressource.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.ressource.dto.response.BulkResourceOperationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;

/**
 * Applique une operation a plusieurs ressources, chacune dans sa propre transaction.
 *
 * <p><b>Un {@code TransactionTemplate} et non {@code @Transactional(REQUIRES_NEW)}.</b> Une
 * annotation posee sur une methode appelee depuis la meme classe ne passe pas par le proxy Spring :
 * l'auto-appel rejoint la transaction englobante, le premier echec la marque « rollback-only », et
 * la validation finale echoue en {@code UnexpectedRollbackException} apres avoir laisse croire que
 * les ressources precedentes etaient traitees. Le template ouvre la transaction explicitement,
 * sans dependre d'un proxy.
 *
 * <p>Une ressource en echec ne doit pas defaire les dix-sept deja traitees. Chaque unite est donc
 * isolee, et son echec devient une ligne du compte rendu plutot qu'une exception.
 */
@Slf4j
@Component
public class ResourceBulkExecutor {

    private final TransactionTemplate isolatedTransaction;

    public ResourceBulkExecutor(PlatformTransactionManager transactionManager) {
        this.isolatedTransaction = new TransactionTemplate(transactionManager);
        this.isolatedTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Traite chaque code dans sa propre transaction et rend un compte rendu.
     *
     * @param operation renvoie le detail a porter au compte rendu · un nombre de creneaux, par
     *                  exemple · ou {@code null} s'il n'y a rien a preciser
     */
    public BulkResourceOperationResponse run(List<String> resourceCodes,
                                             int maxBatchSize,
                                             Function<String, Integer> operation) {
        List<String> codes = normalizeCodes(resourceCodes, maxBatchSize);
        List<BulkResourceOperationResponse.Entry> results = new ArrayList<>();

        for (String code : codes) {
            try {
                results.add(BulkResourceOperationResponse.Entry.processed(code, runIsolated(code, operation)));
            } catch (RuntimeException ex) {
                // Le message metier est destine a l'utilisateur · une exception inattendue ne doit
                // pas pour autant faire echouer le lot entier, seulement cette ressource.
                log.warn("Traitement groupe · ressource {} ecartee : {}", code, ex.getMessage());
                results.add(BulkResourceOperationResponse.Entry.skipped(code, reason(ex)));
            }
        }
        return BulkResourceOperationResponse.of(results);
    }

    /** Une transaction par ressource · l'echec de l'une n'annule pas les autres. */
    private Integer runIsolated(String resourceCode, Function<String, Integer> operation) {
        return isolatedTransaction.execute(status -> operation.apply(resourceCode));
    }

    /**
     * Codes nettoyes, dedoublonnes en conservant l'ordre de saisie, et bornes.
     *
     * <p>Publique : la prevision partage la meme normalisation que la creation, faute de quoi un
     * lot accepte en prevision pourrait etre refuse a la creation, ou l'inverse.
     *
     * <p>Le dedoublonnage compte : un meme code presente deux fois creerait la seconde fois un
     * chevauchement avec ce que la premiere vient d'ecrire, et ressortirait en echec incomprehensible.
     */
    public List<String> normalizeCodes(List<String> resourceCodes, int maxBatchSize) {
        if (resourceCodes == null || resourceCodes.isEmpty()) {
            throw new BadRequestException("Au moins un code ressource est requis");
        }
        List<String> codes = new ArrayList<>(new LinkedHashSet<>(resourceCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .toList()));
        if (codes.isEmpty()) {
            throw new BadRequestException("Au moins un code ressource est requis");
        }
        if (codes.size() > maxBatchSize) {
            throw new BadRequestException("Lot de " + codes.size() + " ressources · maximum autorise : "
                    + maxBatchSize + ". Decoupez l'appel.");
        }
        return codes;
    }

    private String reason(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }
}
