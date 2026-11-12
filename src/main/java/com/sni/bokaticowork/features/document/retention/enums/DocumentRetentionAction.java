package com.sni.bokaticowork.features.document.retention.enums;

public enum DocumentRetentionAction {
    ARCHIVE,
    /**
     * A detruire : le document rejoint la liste « a detruire » de la GED ; un administrateur
     * confirme sa destruction definitive, ou le garde.
     */
    PURGE_FLAG
}
