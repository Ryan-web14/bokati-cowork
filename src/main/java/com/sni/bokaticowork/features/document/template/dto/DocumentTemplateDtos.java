package com.sni.bokaticowork.features.document.template.dto;

import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Echanges des routes des modeles de documents (lot D2). */
public final class DocumentTemplateDtos {

    private DocumentTemplateDtos() {
    }

    /** Ligne de la liste des modeles. */
    public record Summary(String code, String label, String family, String domain, boolean active, Integer publishedVersion,
                          Instant publishedAt, boolean hasDraft) {
    }

    public record VersionSummary(int versionNumber, String status, String note, String createdBy, Instant createdAt, String publishedBy,
                                 Instant publishedAt, Instant archivedAt) {
    }

    public record Version(int versionNumber, String status, String pageFormat, List<Map<String, Object>> copies, Map<String, Object> style,
                          List<Map<String, Object>> blocks, List<Map<String, Object>> fields, String note, String createdBy, Instant createdAt, Instant updatedAt,
                          String publishedBy, Instant publishedAt, Instant archivedAt) {
    }

    /** Fiche d'un modele : version publiee (nulle : gabarit d'origine), brouillon, historique. */
    public record Detail(String code, String label, String family, String domain, String fallbackTemplate, String description, boolean active,
                         Version published, Version draft, List<VersionSummary> versions) {
    }

    /**
     * Brouillon ou contenu d'apercu : format, exemplaires, style ({@code accent}), blocs et champs
     * ajoutes (lot D3, references dans les blocs par {@code champ.<cle>}).
     */
    public record Content(String pageFormat, @Size(max = 4) List<Map<String, Object>> copies, Map<String, Object> style,
                          @Size(max = 40) List<Map<String, Object>> blocks, @Size(max = 20) List<Map<String, Object>> fields,
                          @Size(max = 500) String note) {
        /** Forme du lot D2, sans champs ajoutes. */
        public Content(String pageFormat, List<Map<String, Object>> copies, Map<String, Object> style, List<Map<String, Object>> blocks,
                       String note) {
            this(pageFormat, copies, style, blocks, List.of(), note);
        }
    }

    /** Champ ajoute d'une piece, avec sa valeur saisie (lot D3). */
    public record PieceField(String key, String blockKey, String label, String type, String mode, boolean required, List<String> options,
                             String help, String value, String enteredBy, Instant enteredAt) {
    }

    /** Champs ajoutes d'une piece selon la version publiee de son modele, et ceux qui restent a saisir. */
    public record PieceFields(String templateCode, Integer publishedVersion, String sourceCode, List<PieceField> fields,
                              List<String> missingRequired) {
    }

    /** Valeurs saisies : cle du champ, valeur (vide ou nulle pour effacer). */
    public record PieceFieldsRequest(Map<String, Object> values) {
    }

    public record FieldEntry(String key, String label, String sample) {
    }

    public record ColumnEntry(String key, String label, boolean numeric) {
    }

    public record TableEntry(String key, String title, List<ColumnEntry> columns) {
    }

    public record SignerEntry(String key, String role) {
    }

    /** Ce que les blocs d'un modele peuvent designer, avec des valeurs d'exemple. */
    public record Catalog(String code, List<FieldEntry> fields, List<TableEntry> tables, List<SignerEntry> signers, List<String> commonTokens,
                          List<String> blockTypes, List<String> pageFormats, boolean realPreview, String previewNumberHint) {
    }
}
