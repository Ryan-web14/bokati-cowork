package com.sni.bokaticowork.core.maintenance;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Suppression definitive d'un membre et de toutes ses donnees.
 *
 * <p>Concu pour retirer d'une base de production des jeux de donnees de test qui y sont restes.
 * Ce n'est pas une desactivation : les lignes sont reellement supprimees, ce qui retire du meme
 * coup les montants des agregats comptables, tous derives de {@code billing_document},
 * {@code payment_transaction}, {@code booking} et {@code subscription} · il n'existe pas de grand
 * livre separe a rectifier.
 *
 * <h2>Comment les donnees liees sont trouvees</h2>
 * Deux mecanismes de rattachement coexistent dans le schema, et la purge couvre les deux :
 * <ul>
 *   <li><b>Par code metier.</b> La majorite des tables portent le code du membre dans une colonne
 *       texte ({@code owner_code}, {@code customer_code}, {@code subscriber_code},
 *       {@code party_code}...). Ces codes sont des identifiants uniques a l'echelle de la base,
 *       le rapprochement se fait donc sur le code seul.</li>
 *   <li><b>Par cle etrangere.</b> Les tables filles sont trouvees en parcourant le graphe de cles
 *       etrangeres lu dans {@code information_schema} au moment de l'execution, et supprimees en
 *       profondeur d'abord. Le graphe n'est jamais fige dans le code : une table ajoutee plus tard
 *       est prise en compte sans modifier cette classe.</li>
 * </ul>
 *
 * <h2>Immutabilite fiscale</h2>
 * Les triggers qui interdisent la suppression d'un document scelle sont desactives pour la duree
 * de la transaction puis systematiquement remis, y compris si la purge echoue.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberPurgeService {

    private final JdbcTemplate jdbc;
    private final MemberPurgeProperties properties;

    /**
     * Colonnes texte qui portent le code d'un membre ou de son client. Une table est rattachee
     * des qu'elle possede l'une d'elles ; la colonne de type associee ({@code owner_type}...)
     * n'est volontairement pas testee, car un discriminant divergent ferait manquer des lignes
     * alors que les codes sont deja uniques.
     */
    private static final List<String> CODE_COLUMNS = List.of(
            "owner_code",
            "customer_code",
            "subscriber_code",
            "party_code",
            "member_code",
            "client_code",
            "member_id"
    );

    /** Referentiel et infrastructure · jamais touches, meme si une colonne code y correspondait. */
    private static final Set<String> PROTECTED_TABLES = Set.of(
            "flyway_schema_history",
            "role", "permission", "role_permission",
            "sequence_definition", "sequence_counter",
            "country", "currency",
            "document_type", "document_requirement", "document_sequence",
            "contract_template", "contract_template_clause", "contract_template_policy",
            "billing_tax_rule", "billing_clause_template", "tax_rule",
            "resource_type", "resource_group", "resource_policy",
            "cancellation_policy",
            "inventory_category", "inventory_location", "inventory_unit", "inventory_unit_conversion",
            "purchase_approval_rule",
            "notification_template"
    );

    /** Gardes d'immutabilite a neutraliser le temps de la purge, au format {@code table:trigger}. */
    private static final List<String> IMMUTABILITY_TRIGGERS = List.of(
            "billing_document:trg_billing_document_immutable",
            "billing_document:trg_billing_document_no_delete",
            "billing_document_line:trg_billing_line_immutable",
            "billing_period_closure:trg_billing_period_closure_immutable",
            "contract_audit_event:trg_contract_audit_event_immutable"
    );

    // =================================================================================
    // API publique
    // =================================================================================

    /**
     * Compte, sans rien supprimer, les lignes qui disparaitraient. Disponible meme lorsque la
     * purge est desactivee : c'est la lecture qui permet de decider si on l'allume.
     */
    @Transactional(readOnly = true)
    public PurgeReport preview(String memberCode) {
        return run(memberCode, null, true);
    }

    /** Supprime definitivement le membre et tout ce qui s'y rattache. */
    @Transactional
    public PurgeReport purge(String memberCode, String reason) {
        if (!properties.isEnabled()) {
            throw new BadRequestException(
                    "La purge par membre est desactivee. Mettre bokati.maintenance.member-purge.enabled a true "
                            + "(variable MEMBER_PURGE_ENABLED) pour l'autoriser, puis la remettre a false apres le nettoyage.");
        }
        return run(memberCode, reason, false);
    }

    /**
     * Purge une liste de membres explicitement nommes. Volontairement pas de purge « tout ce qui
     * est anterieur a telle date » : la selection se fait par {@link #candidates}, elle est relue,
     * puis les codes retenus sont passes ici. Une date mal saisie ne peut donc pas vider la base.
     */
    @Transactional
    public BatchPurgeReport purgeAll(List<String> memberCodes, String reason) {
        if (memberCodes == null || memberCodes.isEmpty()) {
            throw new BadRequestException("Aucun code membre fourni");
        }
        if (memberCodes.size() > properties.getMaxBatchSize()) {
            throw new BadRequestException("Lot de " + memberCodes.size() + " membres · maximum autorise : "
                    + properties.getMaxBatchSize());
        }
        List<PurgeReport> reports = new ArrayList<>();
        for (String code : memberCodes) {
            reports.add(purge(code, reason));
        }
        long rows = reports.stream().mapToLong(PurgeReport::rowsAffected).sum();
        return new BatchPurgeReport(reports.size(), rows, reports);
    }

    /**
     * Membres crees avant une date · sert a identifier les donnees de test restees en production.
     * Les membres rattaches a un compte administrateur sont exclus de la liste.
     */
    @Transactional(readOnly = true)
    public List<MemberCandidate> candidates(Instant createdBefore, int limit) {
        if (createdBefore == null) {
            throw new BadRequestException("La date limite est obligatoire");
        }
        return jdbc.query("""
                        SELECT m.member_id, m.email, m.firstname, m.lastname, m.created_at
                        FROM member m
                        WHERE m.created_at < ?
                          AND NOT EXISTS (
                              SELECT 1 FROM role_user ru
                              JOIN role r ON r.id = ru.role_id
                              WHERE ru.user_id = m.user_id AND r.name IN ('ADMIN', 'SUPER_ADMIN'))
                        ORDER BY m.created_at
                        LIMIT ?
                        """,
                (rs, i) -> new MemberCandidate(
                        rs.getString(1), rs.getString(2),
                        rs.getString(3), rs.getString(4),
                        rs.getTimestamp(5) == null ? null : rs.getTimestamp(5).toInstant()),
                java.sql.Timestamp.from(createdBefore), Math.max(1, limit));
    }

    // =================================================================================
    // Execution
    // =================================================================================

    private PurgeReport run(String memberCode, String reason, boolean dryRun) {
        String code = normalize(memberCode);
        Instant start = Instant.now();

        MemberRef member = resolve(code);
        ensureNotAdministrator(member);

        Map<String, Integer> counts = new LinkedHashMap<>();
        List<ForeignKey> foreignKeys = foreignKeys();
        List<String> skipped = new ArrayList<>();

        if (!dryRun) {
            log.warn("=== PURGE MEMBRE {} === utilisateur={} client={} motif={}",
                    code, member.userId(), member.customerId(), reason);
        }

        boolean triggersDisabled = false;
        try {
            if (!dryRun) {
                triggersDisabled = setImmutabilityTriggers(false);
            }

            // 1. Tout ce qui porte le code du membre, table par table, filles d'abord.
            purgeByCode(code, foreignKeys, counts, skipped, dryRun);

            // 2. Le client rattache, uniquement s'il n'est partage avec aucun autre membre.
            if (member.customerId() != null && member.customerCode() != null && !isCustomerShared(member)) {
                purgeByCode(member.customerCode(), foreignKeys, counts, skipped, dryRun);
                deleteCascade("customer", "id = ?", List.of(member.customerId()),
                        foreignKeys, new ArrayDeque<>(), counts, skipped, dryRun);
            }

            // 3. Le compte utilisateur en dernier · la ligne member le reference.
            deleteCascade("users", "id = ?", List.of(member.userId()),
                    foreignKeys, new ArrayDeque<>(), counts, skipped, dryRun);
        } finally {
            if (triggersDisabled) {
                setImmutabilityTriggers(true);
            }
        }

        long rows = counts.values().stream().mapToLong(Integer::longValue).sum();
        long durationMs = Duration.between(start, Instant.now()).toMillis();

        if (!dryRun) {
            log.warn("=== PURGE MEMBRE {} TERMINEE === {} ligne(s) sur {} table(s) · {} ms",
                    code, rows, counts.size(), durationMs);
        }

        return new PurgeReport(code, member.email(), dryRun, rows, sortedByCountDesc(counts), skipped, durationMs);
    }

    /** Supprime, dans chaque table qui porte une colonne de code, les lignes de ce code. */
    private void purgeByCode(String code, List<ForeignKey> foreignKeys, Map<String, Integer> counts,
                             List<String> skipped, boolean dryRun) {
        for (Map.Entry<String, String> entry : tablesCarryingACode().entrySet()) {
            deleteCascade(entry.getKey(), quote(entry.getValue()) + " = ?", List.of(code),
                    foreignKeys, new ArrayDeque<>(), counts, skipped, dryRun);
        }
    }

    /**
     * Supprime les lignes de {@code table} qui satisfont {@code predicate}, apres avoir supprime
     * recursivement leurs lignes filles. Le predicat des filles reference la table parente par
     * sous-requete, si bien que les parametres restent inchanges quelle que soit la profondeur.
     */
    private void deleteCascade(String table, String predicate, List<Object> args,
                               List<ForeignKey> foreignKeys, Deque<String> path,
                               Map<String, Integer> counts, List<String> skipped, boolean dryRun) {
        if (PROTECTED_TABLES.contains(table)) {
            return;
        }
        if (path.contains(table)) {
            // Cycle dans le graphe · on ne redescend pas, la suppression du niveau courant suffit.
            return;
        }
        if (path.size() >= properties.getMaxDepth()) {
            skipped.add(table + " (profondeur maximale " + properties.getMaxDepth() + " atteinte)");
            return;
        }

        path.push(table);
        try {
            for (ForeignKey fk : foreignKeys) {
                if (!fk.parentTable().equals(table) || fk.childTable().equals(table)) {
                    continue;
                }
                String childPredicate = quote(fk.childColumn()) + " IN (SELECT " + quote(fk.parentColumn())
                        + " FROM " + quote(table) + " WHERE " + predicate + ")";
                deleteCascade(fk.childTable(), childPredicate, args, foreignKeys, path, counts, skipped, dryRun);
            }

            String sql = dryRun
                    ? "SELECT COUNT(*) FROM " + quote(table) + " WHERE " + predicate
                    : "DELETE FROM " + quote(table) + " WHERE " + predicate;
            int affected = dryRun
                    ? orZero(jdbc.queryForObject(sql, Integer.class, args.toArray()))
                    : jdbc.update(sql, args.toArray());
            if (affected > 0) {
                counts.merge(table, affected, Integer::sum);
            }
        } catch (RuntimeException ex) {
            // Une table peut etre injoignable (vue materialisee, droits) sans invalider le reste.
            skipped.add(table + " (" + ex.getClass().getSimpleName() + " : " + rootMessage(ex) + ")");
            log.warn("Purge · table {} ignoree : {}", table, rootMessage(ex));
        } finally {
            path.pop();
        }
    }

    // =================================================================================
    // Introspection du schema
    // =================================================================================

    /**
     * Table -> colonne de code a utiliser, pour les tables du schema public. Le filtre sur le type
     * de donnee ecarte les homonymes numeriques : {@code member_id} porte le code du membre dans
     * la table {@code member}, mais peut etre une cle etrangere entiere ailleurs.
     *
     * <p>La priorite entre plusieurs colonnes candidates d'une meme table est resolue en Java,
     * dans l'ordre de {@link #CODE_COLUMNS}, plutot qu'en SQL : passer une liste Java a
     * {@code ANY(?)} obligerait a construire un tableau SQL cote pilote.
     */
    private Map<String, String> tablesCarryingACode() {
        String placeholders = String.join(", ", java.util.Collections.nCopies(CODE_COLUMNS.size(), "?"));
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT c.table_name, c.column_name
                FROM information_schema.columns c
                JOIN information_schema.tables t
                  ON t.table_schema = c.table_schema AND t.table_name = c.table_name
                WHERE c.table_schema = 'public'
                  AND t.table_type = 'BASE TABLE'
                  AND c.data_type IN ('character varying', 'text', 'character')
                  AND c.column_name IN (""" + placeholders + """
                )
                ORDER BY c.table_name
                """, CODE_COLUMNS.toArray());

        Map<String, String> found = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String table = String.valueOf(row.get("table_name"));
            String column = String.valueOf(row.get("column_name"));
            if (PROTECTED_TABLES.contains(table)) {
                continue;
            }
            String current = found.get(table);
            if (current == null || CODE_COLUMNS.indexOf(column) < CODE_COLUMNS.indexOf(current)) {
                found.put(table, column);
            }
        }
        return found;
    }

    private List<ForeignKey> foreignKeys() {
        return jdbc.query("""
                SELECT child.relname   AS child_table,
                       child_col.attname  AS child_column,
                       parent.relname  AS parent_table,
                       parent_col.attname AS parent_column
                FROM pg_constraint con
                JOIN pg_class child  ON child.oid  = con.conrelid
                JOIN pg_class parent ON parent.oid = con.confrelid
                JOIN pg_namespace ns ON ns.oid = child.relnamespace
                JOIN pg_attribute child_col
                  ON child_col.attrelid = con.conrelid AND child_col.attnum = con.conkey[1]
                JOIN pg_attribute parent_col
                  ON parent_col.attrelid = con.confrelid AND parent_col.attnum = con.confkey[1]
                WHERE con.contype = 'f'
                  AND ns.nspname = 'public'
                  AND array_length(con.conkey, 1) = 1
                """, (rs, i) -> new ForeignKey(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)));
    }

    /**
     * Active ou desactive les gardes d'immutabilite. Renvoie vrai si au moins un trigger a
     * effectivement bascule · un trigger absent (migration non encore passee) n'est pas une erreur.
     */
    private boolean setImmutabilityTriggers(boolean enable) {
        boolean any = false;
        for (String entry : IMMUTABILITY_TRIGGERS) {
            String[] parts = entry.split(":", 2);
            try {
                jdbc.execute("ALTER TABLE " + quote(parts[0]) + (enable ? " ENABLE TRIGGER " : " DISABLE TRIGGER ")
                        + quote(parts[1]));
                any = true;
            } catch (RuntimeException ex) {
                log.warn("Purge · trigger {} non {} : {}", entry, enable ? "reactive" : "desactive", rootMessage(ex));
            }
        }
        return any;
    }

    // =================================================================================
    // Resolution du membre
    // =================================================================================

    private MemberRef resolve(String code) {
        List<MemberRef> found = jdbc.query("""
                        SELECT m.member_id, m.user_id, m.customer_id, m.email, c.customer_code
                        FROM member m
                        LEFT JOIN customer c ON c.id = m.customer_id
                        WHERE UPPER(TRIM(m.member_id)) = ?
                        """,
                (rs, i) -> new MemberRef(
                        rs.getString(1),
                        rs.getObject(2) == null ? null : rs.getLong(2),
                        rs.getObject(3) == null ? null : rs.getLong(3),
                        rs.getString(4),
                        rs.getString(5)),
                code);

        if (found.isEmpty()) {
            throw new ResourceNotFoundException("Aucun membre pour le code " + code);
        }
        return found.getFirst();
    }

    private void ensureNotAdministrator(MemberRef member) {
        if (member.userId() == null) {
            return;
        }
        Integer adminRoles = jdbc.queryForObject("""
                SELECT COUNT(*) FROM role_user ru
                JOIN role r ON r.id = ru.role_id
                WHERE ru.user_id = ? AND r.name IN ('ADMIN', 'SUPER_ADMIN')
                """, Integer.class, member.userId());
        if (orZero(adminRoles) > 0) {
            throw new BadRequestException("Le membre " + member.memberCode()
                    + " est rattache a un compte administrateur · purge refusee");
        }
    }

    private boolean isCustomerShared(MemberRef member) {
        Integer others = jdbc.queryForObject(
                "SELECT COUNT(*) FROM member WHERE customer_id = ? AND UPPER(TRIM(member_id)) <> ?",
                Integer.class, member.customerId(), member.memberCode());
        boolean shared = orZero(others) > 0;
        if (shared) {
            log.info("Purge · client {} conserve : {} autre(s) membre(s) le partagent",
                    member.customerCode(), others);
        }
        return shared;
    }

    // =================================================================================
    // Utilitaires
    // =================================================================================

    private String normalize(String memberCode) {
        if (!StringUtils.hasText(memberCode)) {
            throw new BadRequestException("Le code membre est obligatoire");
        }
        return memberCode.trim().toUpperCase(Locale.ROOT);
    }

    /** Les identifiants viennent tous d'{@code information_schema} · le guillemet suffit. */
    private String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private int orZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String rootMessage(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null ? current.getClass().getSimpleName() : message.split("\n")[0];
    }

    private Map<String, Integer> sortedByCountDesc(Map<String, Integer> counts) {
        Map<String, Integer> sorted = new LinkedHashMap<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted;
    }

    // =================================================================================
    // Types
    // =================================================================================

    private record MemberRef(String memberCode, Long userId, Long customerId, String email, String customerCode) {}

    private record ForeignKey(String childTable, String childColumn, String parentTable, String parentColumn) {}

    public record MemberCandidate(String memberCode, String email, String firstname, String lastname,
                                  Instant createdAt) {}

    /**
     * @param rowsByTable lignes supprimees (ou comptees en previsualisation) par table
     * @param skippedTables tables ecartees, avec la raison · a lire avant de conclure que tout est parti
     */
    public record PurgeReport(String memberCode, String email, boolean dryRun, long rowsAffected,
                              Map<String, Integer> rowsByTable, List<String> skippedTables, long durationMs) {}

    public record BatchPurgeReport(int membersPurged, long rowsAffected, List<PurgeReport> reports) {}
}
