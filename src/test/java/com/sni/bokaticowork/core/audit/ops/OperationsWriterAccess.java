package com.sni.bokaticowork.core.audit.ops;

import com.sni.bokaticowork.core.audit.traffic.TrafficRecorder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** Acces de test a l'ecriture par minute du centre d'audit. */
public final class OperationsWriterAccess {

    private OperationsWriterAccess() {
    }

    /** Ecrit tout ce qui attend, minutes en cours comprises (comme a l'arret). */
    public static void flush(JdbcTemplate jdbc, TrafficRecorder recorder) {
        OperationsWriter writer = new OperationsWriter(jdbc, new ThreadPoolTaskScheduler(), "web.1", recorder);
        writer.flushOnShutdown();
    }
}
