package com.sni.bokaticowork.core.audit.retention;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Verification du chainage par empreinte (lot A7) du journal des actions et des evenements de
 * securite : chaque ligne est recalculee (son contenu et l'empreinte precedente) et reliee a la
 * precedente. Une ligne modifiee, inseree ou supprimee hors de la purge est signalee.
 *
 * <p>La premiere ligne gardee n'est pas reliee a la precedente : celle-ci peut avoir ete purgee.</p>
 */
@Service
public class AuditIntegrity {

    static final int MAX_BREAKS = 20;

    private final JdbcTemplate jdbc;

    public AuditIntegrity(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Une rupture : numero dans la chaine et nature ({@code CONTENT} modifie, {@code LINK} lien rompu). */
    public record Break(long chainSeq, String kind) {
    }

    public record Report(String journal, long checked, Long firstSeq, Long lastSeq, long unchained, boolean intact,
                         List<Break> breaks) {
    }

    public List<Report> verify() {
        return List.of(verify("actions", "audit_log", "audit_log_hash"), verify("security", "security_event", "security_event_hash"));
    }

    Report verify(String journal, String table, String function) {
        Map<String, Object> stats = jdbc.queryForMap("SELECT count(*) FILTER (WHERE chain_seq IS NOT NULL) AS checked, "
                + "count(*) FILTER (WHERE chain_seq IS NULL) AS unchained, min(chain_seq) AS first_seq, max(chain_seq) AS last_seq FROM " + table);
        List<Break> breaks = jdbc.query("""
                WITH c AS (
                    SELECT chain_seq, previous_hash, current_hash,
                           lag(current_hash) OVER (ORDER BY chain_seq) AS expected_previous,
                           lag(chain_seq) OVER (ORDER BY chain_seq) AS previous_seq,
                           %s(previous_hash, t) AS recomputed
                    FROM %s t WHERE chain_seq IS NOT NULL)
                SELECT chain_seq, CASE WHEN recomputed IS DISTINCT FROM current_hash THEN 'CONTENT' ELSE 'LINK' END AS kind
                FROM c
                WHERE recomputed IS DISTINCT FROM current_hash
                   OR (previous_seq IS NOT NULL AND (previous_hash IS DISTINCT FROM expected_previous OR chain_seq <> previous_seq + 1))
                ORDER BY chain_seq LIMIT %d""".formatted(function, table, MAX_BREAKS),
                (rs, i) -> new Break(rs.getLong("chain_seq"), rs.getString("kind")));
        long checked = ((Number) stats.get("checked")).longValue();
        return new Report(journal, checked, stats.get("first_seq") == null ? null : ((Number) stats.get("first_seq")).longValue(),
                stats.get("last_seq") == null ? null : ((Number) stats.get("last_seq")).longValue(),
                ((Number) stats.get("unchained")).longValue(), breaks.isEmpty(), breaks);
    }
}
