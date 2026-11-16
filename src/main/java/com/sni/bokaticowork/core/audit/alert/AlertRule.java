package com.sni.bokaticowork.core.audit.alert;

/**
 * Regles d'alerte du centre d'audit (lot A6). Chaque seuil est un parametre modifiable a chaud
 * ({@code bokati.audit.alerts.<cle>}, domaine {@code AUDIT}) ; un seuil a 0 coupe la regle.
 * L'alerte s'ouvre quand la mesure atteint le seuil, et se ferme seule quand elle redescend.
 */
public enum AlertRule {

    ERROR_RATE("error-rate-percent", 5, "CRITICAL", "Taux d'erreurs serveur élevé",
            "%s %% des requêtes en erreur serveur sur les 5 dernières minutes (seuil %s %%)"),
    SERVER_ERRORS("server-errors", 10, "CRITICAL", "Erreurs serveur nombreuses",
            "%s erreurs serveur en 15 minutes (seuil %s)"),
    SLOW_RESPONSES("p95-ms", 3_000, "WARNING", "Réponses lentes",
            "95 %% des requêtes répondent en moins de %s ms sur 15 minutes (seuil %s ms)"),
    FAILED_LOGINS("failed-logins", 20, "WARNING", "Échecs de connexion nombreux",
            "%s échecs de connexion en 15 minutes (seuil %s)"),
    LOCKED_ACCOUNTS("locked-accounts", 3, "WARNING", "Comptes verrouillés",
            "%s comptes verrouillés dans l'heure (seuil %s)"),
    OUTBOX_DEAD("outbox-dead", 1, "CRITICAL", "Messages abandonnés dans la boîte d'envoi",
            "%s messages abandonnés dans l'heure (seuil %s)"),
    OUTBOX_WAITING("outbox-wait-minutes", 15, "CRITICAL", "Boîte d'envoi en retard",
            "Le plus ancien message attend depuis %s minutes (seuil %s)"),
    TASK_FAILURES("task-failures", 3, "WARNING", "Tâche planifiée en échec",
            "Une tâche a échoué %s fois de suite (seuil %s)"),
    EMAIL_FAILURES("email-failures", 5, "WARNING", "Courriels en échec",
            "%s courriels en échec dans l'heure (seuil %s)"),
    WHATSAPP_FAILURES("whatsapp-failures", 5, "WARNING", "Messages WhatsApp en échec",
            "%s messages WhatsApp refusés ou non remis dans l'heure (seuil %s)"),
    PAWAPAY_FAILURES("pawapay-failures", 3, "WARNING", "Rappels PawaPay en échec",
            "%s rappels PawaPay en échec dans l'heure (seuil %s)");

    private final String key;
    private final long defaultThreshold;
    private final String severity;
    private final String title;
    private final String message;

    AlertRule(String key, long defaultThreshold, String severity, String title, String message) {
        this.key = key;
        this.defaultThreshold = defaultThreshold;
        this.severity = severity;
        this.title = title;
        this.message = message;
    }

    public String settingKey() {
        return "bokati.audit.alerts." + key;
    }

    public long defaultThreshold() {
        return defaultThreshold;
    }

    public String severity() {
        return severity;
    }

    public String title() {
        return title;
    }

    public String message(Object value, Object threshold) {
        return message.formatted(value, threshold);
    }
}
