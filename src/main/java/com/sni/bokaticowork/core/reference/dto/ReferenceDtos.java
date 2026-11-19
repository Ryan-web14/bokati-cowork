package com.sni.bokaticowork.core.reference.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Echanges du socle des listes de choix (lot L0). */
public final class ReferenceDtos {

    private ReferenceDtos() {
    }

    /** Une valeur fixe (enumeration du code) : code, libelle francais, ordre, selectionnable. */
    public record EnumValue(String code, String label, String description, int order, boolean selectable) {
    }

    /** Un ensemble de valeurs fixes d'un module. */
    public record EnumGroup(String module, String group, String label, List<EnumValue> values) {
    }

    /** Une liste de l'administrateur, vide comprise. */
    public record ListSummary(String code, String module, String label, String description, boolean strict, int valueCount,
                              int activeValueCount, int autoAddedCount, long usageCount, long otherUsageCount) {
    }

    /** Une valeur d'une liste de l'administrateur. */
    public record Value(String listCode, String code, String label, Integer sortOrder, Boolean active, Boolean requiresText,
                        Boolean systemValue, Boolean autoAdded, Long usageCount) {
    }

    /** Ajout ou remplacement d'une valeur ; le code est mis en majuscules. */
    public record ValueRequest(@NotBlank @Size(max = 60) String code, @NotBlank @Size(max = 255) String label, Integer sortOrder,
                               Boolean active, Boolean requiresText) {
    }

    /** Modification d'une valeur : seuls les champs fournis changent. */
    public record ValuePatch(@Size(max = 255) String label, Integer sortOrder, Boolean active, Boolean requiresText) {
    }

    /** Reglage d'une liste : libelle, description, mode. */
    public record ListPatch(@Size(max = 160) String label, @Size(max = 500) String description, Boolean strict) {
    }
}
