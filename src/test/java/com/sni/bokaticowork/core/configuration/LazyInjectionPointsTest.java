package com.sni.bokaticowork.core.configuration;

import com.sni.bokaticowork.features.billing.dunning.service.DunningRunner;
import com.sni.bokaticowork.features.subscription.lifecycle.service.SubscriptionDirectDebitService;
import com.sni.bokaticowork.features.subscription.lifecycle.service.SubscriptionGraceService;
import com.sni.bokaticowork.features.subscription.subscription.service.support.subscription.SubscriptionLifecycleOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.annotation.Lazy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Un {@code @Lazy} sur un champ ne vaut que si Lombok le recopie sur le constructeur.
 *
 * <p>Lombok ne recopie aucune annotation par defaut : sans {@code lombok.config}, tous les
 * {@code @Lazy private final X x;} du projet sont decoratifs. Spring injecte alors la dependance
 * sans proxy, et deux services qui se citent l'un l'autre empechent le demarrage · ce qui est
 * arrive entre la tolerance, le cycle de vie et le prelevement. Ce test lit le bytecode produit :
 * si la configuration Lombok disparait, il tombe ici et pas au demarrage en production.</p>
 */
class LazyInjectionPointsTest {

    @ParameterizedTest(name = "{0} reçoit {1} en @Lazy")
    @CsvSource({
            "com.sni.bokaticowork.features.subscription.lifecycle.service.SubscriptionGraceService, subscriptionLifecycleOperator",
            "com.sni.bokaticowork.features.subscription.subscription.service.support.subscription.SubscriptionLifecycleOperator, subscriptionDirectDebitService",
            "com.sni.bokaticowork.features.billing.dunning.service.DunningRunner, subscriptionLifecycleOperator"
    })
    void declaredLazyDependenciesReachTheConstructor(String owner, String dependency) throws Exception {
        Class<?> type = Class.forName(owner);
        assertTrue(hasLazyParameterOfType(type, dependency),
                () -> owner + " doit recevoir " + dependency + " en @Lazy · sinon le contexte forme un cycle");
    }

    @Test
    void theThreeServicesOfTheCycleAreStillWiredTogether() {
        // Si l'une de ces aretes disparait, le test ci-dessus n'a plus de raison d'etre · on veut
        // le savoir plutot que de garder une garde qui ne garde rien.
        assertTrue(hasParameterOfType(SubscriptionGraceService.class, SubscriptionLifecycleOperator.class));
        assertTrue(hasParameterOfType(SubscriptionLifecycleOperator.class, SubscriptionDirectDebitService.class));
        assertTrue(hasParameterOfType(SubscriptionDirectDebitService.class, SubscriptionGraceService.class));
        assertTrue(hasParameterOfType(DunningRunner.class, SubscriptionLifecycleOperator.class));
    }

    private static boolean hasLazyParameterOfType(Class<?> owner, String simpleTypeName) {
        return Arrays.stream(owner.getDeclaredConstructors())
                .flatMap(constructor -> Arrays.stream(constructor.getParameters()))
                .filter(parameter -> parameter.getType().getSimpleName().equalsIgnoreCase(simpleTypeName))
                .anyMatch(parameter -> parameter.isAnnotationPresent(Lazy.class));
    }

    private static boolean hasParameterOfType(Class<?> owner, Class<?> dependency) {
        for (Constructor<?> constructor : owner.getDeclaredConstructors()) {
            for (Parameter parameter : constructor.getParameters()) {
                if (parameter.getType().equals(dependency)) {
                    return true;
                }
            }
        }
        return false;
    }
}
