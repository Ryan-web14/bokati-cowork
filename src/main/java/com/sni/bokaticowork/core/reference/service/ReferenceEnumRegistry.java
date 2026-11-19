package com.sni.bokaticowork.core.reference.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.reference.dto.ReferenceDtos.EnumGroup;
import com.sni.bokaticowork.core.reference.dto.ReferenceDtos.EnumValue;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Registre unique des valeurs fixes (socle des listes de choix, lot L0) : enumerations du code et
 * ensembles en dur, avec leur libelle francais, leur ordre et s'ils se choisissent dans un ecran.
 *
 * <p>Chaque module declare les siennes par un {@link Contributor}. L'ecran les lit par
 * {@code GET /reference/enums?module=&group=} ; le service qui recoit un code le controle par
 * {@link #require} : exposition et controle lisent le meme registre. Construit une fois au
 * demarrage, il ne change plus (lecture sans verrou).</p>
 */
@Component
public class ReferenceEnumRegistry {

    /** Declaration des valeurs fixes d'un module. */
    public interface Contributor {
        void contribute(Builder registry);
    }

    private final Map<String, EnumGroup> groups;

    public ReferenceEnumRegistry(List<Contributor> contributors) {
        Builder builder = new Builder();
        contributors.forEach(contributor -> contributor.contribute(builder));
        this.groups = Collections.unmodifiableMap(builder.groups);
    }

    /** Groupes d'un module (tous si {@code module} est vide), un seul si {@code group} est donne. */
    public List<EnumGroup> groups(String module, String group) {
        String m = StringUtils.hasText(module) ? module.trim().toUpperCase(Locale.ROOT) : null;
        String g = StringUtils.hasText(group) ? group.trim().toUpperCase(Locale.ROOT) : null;
        return groups.values().stream()
                .filter(item -> m == null || item.module().equals(m))
                .filter(item -> g == null || item.group().equals(g))
                .toList();
    }

    public Optional<EnumGroup> group(String module, String group) {
        return Optional.ofNullable(groups.get(key(module, group)));
    }

    /** Libelle d'un code, ou le code s'il est inconnu. */
    public String label(String module, String group, String code) {
        if (code == null) {
            return null;
        }
        return group(module, group).flatMap(item -> item.values().stream().filter(value -> value.code().equals(code)).findFirst())
                .map(EnumValue::label).orElse(code);
    }

    /**
     * Le code, s'il appartient au groupe et se choisit.
     *
     * @throws BadRequestException {@code VALEUR_INCONNUE} avec les valeurs permises
     */
    public String require(String module, String group, String code) {
        EnumGroup item = group(module, group).orElseThrow(() -> new IllegalStateException("Valeurs fixes non déclarées : " + key(module, group)));
        String normalized = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        return item.values().stream().filter(value -> value.selectable() && value.code().equals(normalized)).findFirst()
                .map(EnumValue::code)
                .orElseThrow(() -> BadRequestException.coded("VALEUR_INCONNUE", "Valeur inconnue « " + code + " » pour « " + item.label()
                        + " » ; valeurs permises : " + item.values().stream().filter(EnumValue::selectable).map(EnumValue::code)
                        .collect(Collectors.joining(", "))));
    }

    private static String key(String module, String group) {
        return (module == null ? "" : module.trim().toUpperCase(Locale.ROOT)) + "/" + (group == null ? "" : group.trim().toUpperCase(Locale.ROOT));
    }

    /** Collecte des declarations, au demarrage. */
    public static final class Builder {
        private final Map<String, EnumGroup> groups = new LinkedHashMap<>();

        /** Une enumeration entiere, toutes valeurs selectionnables. */
        public <E extends Enum<E>> Builder add(String module, String group, String label, Class<E> type, Function<E, String> labeler) {
            return add(module, group, label, type, labeler, value -> true);
        }

        /** Une enumeration, certaines valeurs affichables mais non selectionnables (etat atteint par le systeme seul). */
        public <E extends Enum<E>> Builder add(String module, String group, String label, Class<E> type, Function<E, String> labeler,
                                               Predicate<E> selectable) {
            List<EnumValue> values = new ArrayList<>();
            int order = 10;
            for (E constant : type.getEnumConstants()) {
                values.add(new EnumValue(constant.name(), labeler.apply(constant), null, order, selectable.test(constant)));
                order += 10;
            }
            return add(module, group, label, values);
        }

        /** Un ensemble en dur (codes d'un {@code Set.of} d'un service). */
        public Builder add(String module, String group, String label, List<EnumValue> values) {
            String m = module.trim().toUpperCase(Locale.ROOT);
            String g = group.trim().toUpperCase(Locale.ROOT);
            if (groups.putIfAbsent(key(m, g), new EnumGroup(m, g, label, List.copyOf(values))) != null) {
                throw new IllegalStateException("Valeurs fixes déclarées deux fois : " + key(m, g));
            }
            return this;
        }
    }
}
