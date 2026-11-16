package com.sni.bokaticowork.core.audit.change;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Entites suivies par le journal des actions (lot A1) et leur code lisible.
 *
 * <p>Entites principales (decision du 14/11/2026) : l'avant et l'apres de chaque champ modifie est
 * garde. Les autres entites metier sont notees sans leurs champs (type, code, operation), pour que la
 * chronologie d'un element les retrouve. Les entites techniques du socle ({@code core}, sauf les
 * parametres) sont ignorees : journaux, boite de sortie, compteurs de numerotation.</p>
 */
public final class AuditedEntities {

    /** Entite principale : chemin des champs qui forment son code lisible. */
    private static final Map<String, List<String>> MAIN = Map.ofEntries(
            Map.entry("BillingDocument", List.of("documentNumber")),
            Map.entry("PaymentTransaction", List.of("transactionNumber")),
            Map.entry("Subscription", List.of("subscriptionNumber")),
            Map.entry("Booking", List.of("bookingNumber")),
            Map.entry("Member", List.of("memberId")),
            Map.entry("Customer", List.of("customerId")),
            Map.entry("Users", List.of("userId")),
            Map.entry("Role", List.of("name")),
            Map.entry("Permission", List.of("name")),
            Map.entry("RolePermission", List.of("role.name", "permission.name")),
            Map.entry("RoleUser", List.of("users.userId", "role.name")),
            Map.entry("PlatformSetting", List.of("key")),
            Map.entry("DocumentTemplate", List.of("code")),
            Map.entry("NotificationTemplate", List.of("templateCode")));

    private static final String CORE_PACKAGE = "com.sni.bokaticowork.core.";
    private static final List<String> FALLBACK_CODE_FIELDS = List.of("code", "number", "reference");

    private static final Map<String, Optional<Field>> FIELDS = new ConcurrentHashMap<>();

    private AuditedEntities() {
    }

    public static boolean isMain(Class<?> type) {
        return MAIN.containsKey(type.getSimpleName());
    }

    /** Faux pour les entites techniques du socle, qui ne sont pas des elements metier. */
    public static boolean isTracked(Class<?> type) {
        if (isMain(type)) {
            return true;
        }
        return !type.getName().startsWith(CORE_PACKAGE);
    }

    /** Code lisible d'une entite chargee : champs configures, sinon code ou numero, sinon identifiant. */
    public static String codeOf(Object entity) {
        Object target = unproxy(entity);
        if (target == null) {
            return proxyLabel(entity);
        }
        Class<?> type = target.getClass();
        List<String> paths = MAIN.get(type.getSimpleName());
        if (paths != null) {
            StringBuilder code = new StringBuilder();
            for (String path : paths) {
                Object value = navigate(target, path);
                if (code.length() > 0) {
                    code.append(':');
                }
                code.append(value == null ? "?" : value);
            }
            return code.toString();
        }
        for (String name : FALLBACK_CODE_FIELDS) {
            Object value = read(target, name);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        Object id = idOf(target);
        return id == null ? null : "#" + id;
    }

    /** Libelle d'une entite referencee : {@code Type:code}, sans charger un proxy non initialise. */
    public static String label(Object entity) {
        Object target = unproxy(entity);
        if (target == null) {
            return proxyLabel(entity);
        }
        return target.getClass().getSimpleName() + ":" + codeOf(target);
    }

    public static boolean isEntity(Object value) {
        return value instanceof HibernateProxy || (value != null && value.getClass().isAnnotationPresent(Entity.class));
    }

    private static Object navigate(Object target, String path) {
        Object current = target;
        for (String part : path.split("\\.")) {
            current = unproxy(read(current, part));
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    /** L'objet reel, ou {@code null} pour un proxy non initialise (jamais charge ici). */
    private static Object unproxy(Object value) {
        if (value instanceof HibernateProxy proxy) {
            LazyInitializer initializer = proxy.getHibernateLazyInitializer();
            return initializer.isUninitialized() ? null : initializer.getImplementation();
        }
        return value;
    }

    private static String proxyLabel(Object value) {
        if (value instanceof HibernateProxy proxy) {
            LazyInitializer initializer = proxy.getHibernateLazyInitializer();
            return initializer.getPersistentClass().getSimpleName() + "#" + initializer.getInternalIdentifier();
        }
        return null;
    }

    private static Object idOf(Object target) {
        Class<?> type = target.getClass();
        Optional<Field> field = FIELDS.computeIfAbsent(type.getName() + "#@Id", key -> {
            for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
                for (Field candidate : current.getDeclaredFields()) {
                    if (candidate.isAnnotationPresent(Id.class)) {
                        candidate.setAccessible(true);
                        return Optional.of(candidate);
                    }
                }
            }
            return Optional.empty();
        });
        return field.map(f -> get(f, target)).orElse(null);
    }

    private static Object read(Object target, String name) {
        if (target == null) {
            return null;
        }
        Class<?> type = target.getClass();
        Optional<Field> field = FIELDS.computeIfAbsent(type.getName() + "#" + name, key -> {
            for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
                try {
                    Field found = current.getDeclaredField(name);
                    found.setAccessible(true);
                    return Optional.of(found);
                } catch (NoSuchFieldException ignored) {
                    // classe parente
                }
            }
            return Optional.empty();
        });
        return field.map(f -> get(f, target)).orElse(null);
    }

    private static Object get(Field field, Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException | RuntimeException exception) {
            return null;
        }
    }
}
