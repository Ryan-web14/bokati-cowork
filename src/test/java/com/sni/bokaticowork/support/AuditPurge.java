package com.sni.bokaticowork.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Suppression de test dans un journal en ajout seul (journal des actions, evenements de securite) :
 * comme la purge de conservation, dans une transaction qui le declare ({@code bokati.audit_purge}).
 */
public final class AuditPurge {

    private AuditPurge() {
    }

    public static int delete(JdbcTemplate jdbc, PlatformTransactionManager transactions, String sql, Object... args) {
        Integer deleted = new TransactionTemplate(transactions).execute(status -> {
            jdbc.execute("SET LOCAL bokati.audit_purge = 'on'");
            return jdbc.update(sql, args);
        });
        return deleted == null ? 0 : deleted;
    }
}
