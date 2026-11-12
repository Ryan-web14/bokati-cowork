package com.sni.bokaticowork.features.document.kyc.service.support;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Libelles francais du rapport KYC imprime ({@code kyc/kyc-case-report.html}, variable {@code fmt}).
 *
 * <p>Le rapport affichait les valeurs d'enumeration brutes (UNDER_REVIEW, VERY_HIGH, CUSTOMER) et
 * les instants ISO. Les reponses JSON de l'API ne changent pas · seule la piece imprimee traduit.</p>
 */
public final class KycReportFormatter {

    static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Map<String, String> LABELS = Map.ofEntries(
            // Statut du dossier
            Map.entry("NOT_STARTED", "Non commencé"),
            Map.entry("IN_PROGRESS", "En cours"),
            Map.entry("SUBMITTED", "Soumis"),
            Map.entry("UNDER_REVIEW", "En cours d'examen"),
            Map.entry("APPROVED", "Approuvé"),
            Map.entry("REJECTED", "Rejeté"),
            Map.entry("PENDING_CORRECTION", "Correction demandée"),
            Map.entry("RENEWAL_REQUIRED", "Renouvellement requis"),
            Map.entry("EXPIRED", "Expiré"),
            // Statut d'un document
            Map.entry("PENDING", "En attente"),
            Map.entry("VERIFIED", "Vérifié"),
            // Niveau de risque
            Map.entry("LOW", "Faible"),
            Map.entry("MEDIUM", "Moyen"),
            Map.entry("HIGH", "Élevé"),
            Map.entry("VERY_HIGH", "Très élevé"),
            // Type de titulaire
            Map.entry("CUSTOMER", "Client"),
            Map.entry("MEMBER", "Membre"),
            Map.entry("BUSINESS", "Entreprise"),
            // Actions du fil
            Map.entry("CASE_CREATED", "Dossier créé"),
            Map.entry("CASE_SUBMITTED", "Dossier soumis"),
            Map.entry("DOCUMENT_UPLOADED", "Document déposé"),
            Map.entry("DOCUMENT_PENDING", "Document en attente"),
            Map.entry("DOCUMENT_VERIFIED", "Document vérifié"),
            Map.entry("DOCUMENT_REJECTED", "Document rejeté"),
            Map.entry("DOCUMENT_EXPIRED", "Document expiré"),
            Map.entry("NOTE_ADDED", "Note ajoutée"),
            Map.entry("SYSTEM", "Système"));

    private static final Pattern DOCUMENT_SUBMITTED = Pattern.compile("^Document (.+) submitted$");
    private static final Pattern EVENT_PUBLISHED = Pattern.compile("^Event (.+) published$");

    /** Libelle francais d'une valeur d'enumeration ; la valeur brute si elle est inconnue. */
    public String label(Object value) {
        if (value == null) {
            return "";
        }
        String key = value instanceof Enum<?> e ? e.name() : value.toString();
        return LABELS.getOrDefault(key, key);
    }

    /** Description du fil, traduite quand elle vient du systeme. */
    public String description(String text) {
        if (text == null) {
            return "";
        }
        switch (text) {
            case "KYC case created": return "Dossier KYC créé";
            case "KYC case submitted": return "Dossier KYC soumis";
            case "Document review recorded": return "Examen du document enregistré";
            default: break;
        }
        Matcher document = DOCUMENT_SUBMITTED.matcher(text);
        if (document.matches()) {
            return "Document " + document.group(1) + " déposé";
        }
        Matcher event = EVENT_PUBLISHED.matcher(text);
        if (event.matches()) {
            return "Événement " + event.group(1) + " publié";
        }
        return text;
    }

    public String date(LocalDate date) {
        return date == null ? "" : DATE.format(date);
    }

    public String dateTime(Instant instant) {
        return instant == null ? "" : DATE_TIME.format(instant.atZone(ZONE));
    }
}
