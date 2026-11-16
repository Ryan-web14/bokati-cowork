package com.sni.bokaticowork.core.audit.change;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Changements faits pendant une action auditee, sur le fil de la requete.
 *
 * <p>Chaque appel {@code @Audited} ouvre un cadre ; les ecouteurs Hibernate y versent les insertions,
 * modifications et suppressions ecrites en base pendant l'appel. Les appels imbriques ont chacun leur
 * cadre, et un changement est verse dans tous les cadres ouverts. Rien n'est garde hors d'un appel
 * audite.</p>
 */
public final class AuditChanges {

    /** Au-dela, les changements suivants sont seulement comptes. */
    static final int MAX_CHANGES = 30;
    static final int MAX_FIELDS = 40;

    private static final ThreadLocal<Deque<Frame>> FRAMES = new ThreadLocal<>();

    private AuditChanges() {
    }

    /** Un element touche : type, code, operation et champs modifies (avant / apres). */
    public static final class Change {
        private final String entityType;
        private final String entityCode;
        private String operation;
        private final boolean main;
        private final Map<String, Map<String, Object>> fields = new LinkedHashMap<>();

        Change(String entityType, String entityCode, String operation, boolean main) {
            this.entityType = entityType;
            this.entityCode = entityCode;
            this.operation = operation;
            this.main = main;
        }

        public String entityType() {
            return entityType;
        }

        public String entityCode() {
            return entityCode;
        }

        public String operation() {
            return operation;
        }

        public boolean main() {
            return main;
        }

        public Map<String, Map<String, Object>> fields() {
            return fields;
        }

        void field(String name, Object before, Object after) {
            Map<String, Object> existing = fields.get(name);
            if (existing != null) {
                // Deux ecritures dans la meme action : premier avant, dernier apres.
                existing.put("after", after);
                return;
            }
            if (fields.size() >= MAX_FIELDS) {
                return;
            }
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("before", before);
            value.put("after", after);
            fields.put(name, value);
        }

        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("entityType", entityType);
            map.put("entityCode", entityCode);
            map.put("operation", operation);
            if (!fields.isEmpty()) {
                map.put("fields", fields);
            }
            return map;
        }
    }

    static final class Frame {
        private final Map<String, Change> changes = new LinkedHashMap<>();
        private int omitted;

        Change change(String entityType, String entityCode, String operation, boolean main) {
            String key = entityType + "|" + entityCode;
            Change existing = changes.get(key);
            if (existing != null) {
                if ("DELETE".equals(operation)) {
                    existing.operation = "DELETE";
                }
                return existing;
            }
            if (changes.size() >= MAX_CHANGES) {
                omitted++;
                return null;
            }
            Change created = new Change(entityType, entityCode, operation, main);
            changes.put(key, created);
            return created;
        }
    }

    public static void begin() {
        Deque<Frame> frames = FRAMES.get();
        if (frames == null) {
            frames = new ArrayDeque<>();
            FRAMES.set(frames);
        }
        frames.push(new Frame());
    }

    /** Ferme le cadre courant et rend ses changements, sous la forme gardee dans {@code diff_json}. */
    public static Result end() {
        Deque<Frame> frames = FRAMES.get();
        if (frames == null || frames.isEmpty()) {
            return Result.EMPTY;
        }
        Frame frame = frames.pop();
        if (frames.isEmpty()) {
            FRAMES.remove();
        }
        return new Result(new ArrayList<>(frame.changes.values()), frame.omitted);
    }

    public static boolean active() {
        Deque<Frame> frames = FRAMES.get();
        return frames != null && !frames.isEmpty();
    }

    /** Verse un changement dans tous les cadres ouverts ; rend ceux qui l'acceptent (sans depassement). */
    static List<Change> record(String entityType, String entityCode, String operation, boolean main) {
        Deque<Frame> frames = FRAMES.get();
        List<Change> targets = new ArrayList<>();
        if (frames == null) {
            return targets;
        }
        for (Frame frame : frames) {
            Change change = frame.change(entityType, entityCode, operation, main);
            if (change != null) {
                targets.add(change);
            }
        }
        return targets;
    }

    static void field(List<Change> targets, String name, Object before, Object after) {
        for (Change change : targets) {
            if (change.main) {
                change.field(name, before, after);
            }
        }
    }

    /** Changements d'une action et nombre de changements non gardes au-dela de la limite. */
    public record Result(List<Change> changes, int omitted) {
        static final Result EMPTY = new Result(List.of(), 0);

        public boolean isEmpty() {
            return changes.isEmpty() && omitted == 0;
        }

        /** Element principal de l'action : la premiere entite principale touchee, sinon la premiere entite. */
        public Change primary() {
            return changes.stream().filter(Change::main).findFirst()
                    .orElse(changes.isEmpty() ? null : changes.getFirst());
        }

        public Map<String, Object> toDiff() {
            Map<String, Object> diff = new LinkedHashMap<>();
            diff.put("changes", changes.stream().map(Change::toMap).toList());
            if (omitted > 0) {
                diff.put("omitted", omitted);
            }
            return diff;
        }
    }
}
