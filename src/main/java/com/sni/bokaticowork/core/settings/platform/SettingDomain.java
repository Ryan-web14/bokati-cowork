package com.sni.bokaticowork.core.settings.platform;

/**
 * Les domaines de parametres · {@code SUPER_ADMIN} les a tous, {@code ADMIN} les operations
 * courantes. L'argent, la securite et le technique restent au {@code SUPER_ADMIN}.
 */
public enum SettingDomain {
    BOOKING("Réservation", true),
    RESOURCES("Ressources et horaires", true),
    SUBSCRIPTIONS("Abonnements", true),
    PASSES("Passes", true),
    MARKETPLACE("Marketplace", true),
    PROMOTIONS("Promotions", true),
    DOMICILIATION("Domiciliation", true),
    CONTRACTS("Contrats", true),
    SUPPORT("Support", true),
    TASKS("Tâches", true),
    CRM("CRM", true),
    EVENTS("Événements", true),
    PORTAL("Portail", true),
    COMPANIES("Entreprises", false),
    WALLET("Portefeuille", false),
    PAYMENT("Paiement et caisses", false),
    ACCOUNTING("Comptabilité", false),
    BILLING("Facturation", false),
    COMPLIANCE("Conformité", false),
    DOCUMENTS("Documents", false),
    LOCALE("Langue et téléphone", false),
    SECURITY("Sécurité", false),
    NOTIFICATIONS("Notifications et e-mails", false),
    WEBHOOKS("Webhooks", false),
    OUTBOX("Boîte d'envoi", false),
    GRAPH_API("API Graph", false),
    WEBSOCKET("WebSocket", false),
    ENVIRONMENT("Adresses et environnement", false);

    private final String label;
    private final boolean adminEditable;

    SettingDomain(String label, boolean adminEditable) {
        this.label = label;
        this.adminEditable = adminEditable;
    }

    public String label() {
        return label;
    }

    public boolean adminEditable() {
        return adminEditable;
    }
}
