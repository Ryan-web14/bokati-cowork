package com.sni.bokaticowork.core.audit.service.implementation;

import java.util.Map;

/** Acces de test au resume des changements de l'export CSV. */
public final class AuditServiceImplCsvAccess {

    private AuditServiceImplCsvAccess() {
    }

    public static String changes(Map<String, Object> diff) {
        return AuditServiceImpl.changes(diff);
    }
}
