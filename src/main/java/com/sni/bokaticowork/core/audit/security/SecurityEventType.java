package com.sni.bokaticowork.core.audit.security;

/** Types d'evenements de securite (lot A2). */
public enum SecurityEventType {
    /** Connexion reussie (mot de passe ou code a usage unique, detail {@code method}). */
    LOGIN_SUCCESS,
    /** Echec de connexion : identifiants faux, compte inconnu ou desactive (detail {@code reason}). */
    LOGIN_FAILURE,
    /** Connexion refusee : compte verrouille. */
    LOGIN_REFUSED_LOCKED,
    /** Connexion a l'espace d'administration refusee : compte sans role d'equipe. */
    LOGIN_REFUSED_BACK_OFFICE,
    /** Compte verrouille apres trop d'echecs (detail {@code lockedUntil}). */
    ACCOUNT_LOCKED,
    /** Compte deverrouille (detail {@code by} : soi-meme, administrateur, reinitialisation). */
    ACCOUNT_UNLOCKED,
    /** Jeton de rafraichissement deja utilise presente de nouveau : toutes les sessions sont fermees. */
    REFRESH_TOKEN_REUSE,
    LOGOUT,
    /** Session fermee par un administrateur (detail {@code sessionId}, {@code reason}). */
    SESSION_REVOKED,
    PASSWORD_RESET_REQUESTED,
    PASSWORD_RESET_COMPLETED,
    PASSWORD_RESET_BY_ADMIN,
    /** Refus d'acces (403) a une route. */
    ACCESS_DENIED,
    /** Trop de requetes (429). */
    RATE_LIMITED
}
