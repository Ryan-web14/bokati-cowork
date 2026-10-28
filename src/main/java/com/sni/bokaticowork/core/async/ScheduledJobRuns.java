package com.sni.bokaticowork.core.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Un passage par periode, quel que soit le nombre d'instances.
 *
 * <p>Les deux dynos executent les memes taches planifiees. Pour une tache qui relit l'operateur ou
 * envoie un rapport, deux passages, c'est deux fois les appels et deux courriels identiques. La
 * premiere instance qui inscrit le passage ({@code job_name}, {@code run_key}) le fait ; l'autre
 * trouve la ligne et s'abstient.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledJobRuns {

    private final JdbcTemplate jdbcTemplate;

    /** Vrai si ce passage revient a cette instance · faux s'il a deja ete pris. */
    public boolean claim(String jobName, String runKey) {
        int inserted = jdbcTemplate.update(
                "INSERT INTO scheduled_job_run (job_name, run_key) VALUES (?, ?) ON CONFLICT DO NOTHING",
                jobName, runKey);
        if (inserted == 0) {
            log.info("Tache {} · passage {} deja pris par une autre instance", jobName, runKey);
        }
        return inserted > 0;
    }

    /** Consigne la fin du passage et ce qu'il a donne. */
    public void finish(String jobName, String runKey, String summary) {
        String trimmed = summary == null ? null : summary.length() > 2000 ? summary.substring(0, 2000) : summary;
        jdbcTemplate.update("UPDATE scheduled_job_run SET finished_at = NOW(), summary = ? WHERE job_name = ? AND run_key = ?",
                trimmed, jobName, runKey);
    }
}
