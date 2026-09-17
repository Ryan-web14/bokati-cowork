package com.sni.bokaticowork.core.idempotency.aop;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Permet d'exiger une cle d'idempotence sur des operations declarees {@code required = false},
 * sans recompiler ni modifier les controleurs.
 *
 * <p>Motivation : les operations sensibles doivent pouvoir passer en cle obligatoire quand les
 * clients sont prets, notamment les terminaux de terrain qui rejouent une requete apres une coupure
 * reseau. Rendre la cle obligatoire est une rupture de contrat d'API : elle se pilote donc par
 * configuration, progressivement, et non par une livraison de code.</p>
 *
 * <p>Configuration, liste de noms d'operations separes par des virgules :</p>
 * <pre>
 * idempotency:
 *   required-operations: INVENTORY_STOCK_IN,INVENTORY_STOCK_OUT,INVENTORY_STOCK_TRANSFER
 * </pre>
 *
 * <p>La valeur {@code *} rend la cle obligatoire sur toutes les operations annotees.</p>
 */
@Component
public class IdempotencyRequirementPolicy {

    private static final String WILDCARD = "*";

    private final Set<String> requiredOperations;
    private final boolean allRequired;

    public IdempotencyRequirementPolicy(@Value("${idempotency.required-operations:}") String configuredOperations) {
        this.requiredOperations = parse(configuredOperations);
        this.allRequired = requiredOperations.contains(WILDCARD);
    }

    /**
     * @param operation nom d'operation declare sur {@link Idempotent#operation()}
     * @return vrai si la configuration impose une cle d'idempotence pour cette operation
     */
    public boolean isRequired(String operation) {
        if (allRequired) {
            return true;
        }
        if (operation == null || operation.isBlank()) {
            return false;
        }
        return requiredOperations.contains(operation.trim().toUpperCase(Locale.ROOT));
    }

    private Set<String> parse(String configuredOperations) {
        if (configuredOperations == null || configuredOperations.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(configuredOperations.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> WILDCARD.equals(value) ? WILDCARD : value.toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }
}
