package com.sni.bokaticowork.core.audit.timeline;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Frise d'audit filtree sur une seule source : le tri final ne doit pas suivre directement la branche. */
class TimelineSqlTest {

    @Test
    void aSingleSourceIsWrappedSoPostgresSeesOnlyOneOrderByPerLevel() {
        String sql = TimelineService.timelineSql(List.of("(SELECT 1 AS occurred_at FROM request_log ORDER BY created_at DESC LIMIT 51)"), 50);

        assertThat(sql).startsWith("SELECT * FROM (\n(SELECT")
                .contains("LIMIT 51)\n) AS timeline\nORDER BY occurred_at DESC, source DESC, ref_id DESC LIMIT 51");
    }

    @Test
    void severalSourcesAreJoinedInsideTheSameWrapper() {
        String sql = TimelineService.timelineSql(List.of("(SELECT a)", "(SELECT b)"), 10);

        assertThat(sql).isEqualTo("SELECT * FROM (\n(SELECT a)\nUNION ALL\n(SELECT b)\n) AS timeline"
                + "\nORDER BY occurred_at DESC, source DESC, ref_id DESC LIMIT 11");
    }
}
