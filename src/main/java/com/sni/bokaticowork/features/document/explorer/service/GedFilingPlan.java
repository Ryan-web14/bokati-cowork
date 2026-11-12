package com.sni.bokaticowork.features.document.explorer.service;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentCategory;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Le plan de classement de la GED : ou range-t-on un document, d'apres son proprietaire et sa nature.
 *
 * <pre>
 * Membres / Awa Ndoye · MBR-000012 / KYC | Contrats | Domiciliation | Finances | Juridique | Documents
 * Clients / ...                      (meme decoupage)
 * Entreprises / ...                  (meme decoupage)
 * Contrats | Facturation | Commercial | Equipements / &lt;annee&gt;   (documents rattaches a un objet)
 * </pre>
 *
 * <p>Un document interne n'a pas de place imposee : il reste la ou on l'a depose.</p>
 */
public final class GedFilingPlan {

    private GedFilingPlan() {
    }

    /** Un niveau du chemin : sa cle systeme et son nom affiche. */
    public record Level(String key, String name) {}

    /** Le chemin d'un document, de la racine a son dossier ; vide s'il n'est pas classe automatiquement. */
    public static List<Level> pathFor(DocumentOwnerType ownerType, Long ownerId, String ownerLabel,
                                      DocumentCategory category, String typeCode, Instant uploadedAt) {
        if (ownerType == null || ownerType == DocumentOwnerType.INTERNAL) {
            return List.of();
        }
        return switch (ownerType) {
            case MEMBER, CUSTOMER, BUSINESS -> {
                if (ownerId == null) yield List.of();
                String owner = "owner:" + ownerType.name() + ":" + ownerId;
                String section = section(category, typeCode);
                yield List.of(
                        new Level("root:" + ownerType.name(), rootName(ownerType)),
                        new Level(owner, ownerLabel),
                        new Level(owner + ":" + section, sectionName(section)));
            }
            default -> {
                String year = String.valueOf((uploadedAt == null ? Instant.now() : uploadedAt).atZone(ZoneOffset.UTC).getYear());
                // Factures et paiements partagent la meme racine.
                String root = "root:" + (ownerType == DocumentOwnerType.PAYMENT ? DocumentOwnerType.INVOICE.name() : ownerType.name());
                yield List.of(
                        new Level(root, rootName(ownerType)),
                        new Level(root + ":" + year, year));
            }
        };
    }

    static String section(DocumentCategory category, String typeCode) {
        String type = typeCode == null ? "" : typeCode.toUpperCase();
        if (category == DocumentCategory.KYC) return "KYC";
        if (type.startsWith("CONTRACT")) return "CONTRACTS";
        if (type.startsWith("DOMICILIATION")) return "DOMICILIATION";
        if (category == DocumentCategory.FINANCIAL) return "FINANCE";
        if (category == DocumentCategory.LEGAL) return "LEGAL";
        return "OTHER";
    }

    static String sectionName(String section) {
        return switch (section) {
            case "KYC" -> "KYC";
            case "CONTRACTS" -> "Contrats";
            case "DOMICILIATION" -> "Domiciliation";
            case "FINANCE" -> "Finances";
            case "LEGAL" -> "Juridique";
            default -> "Documents";
        };
    }

    static String rootName(DocumentOwnerType ownerType) {
        return switch (ownerType) {
            case MEMBER -> "Membres";
            case CUSTOMER -> "Clients";
            case BUSINESS -> "Entreprises";
            case CONTRACT -> "Contrats";
            case INVOICE, PAYMENT -> "Facturation";
            case PROPOSAL -> "Commercial";
            case ASSET -> "Équipements";
            case INTERNAL -> "Interne";
        };
    }
}
