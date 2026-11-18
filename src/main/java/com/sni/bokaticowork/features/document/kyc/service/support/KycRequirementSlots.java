package com.sni.bokaticowork.features.document.kyc.service.support;

import com.sni.bokaticowork.features.document.documentMaster.model.DocumentRequirement;
import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Exigences KYC en ET / OU (17/11/2026). Une « place » est soit une exigence seule, soit un groupe
 * d'alternatives (meme {@code alternativeGroup}) : une seule piece du groupe suffit (OU), et toutes
 * les places obligatoires doivent etre remplies (ET). Un groupe est obligatoire si l'une de ses
 * exigences l'est.
 *
 * <p>Sans aucune exigence obligatoire mais avec des exigences facultatives, l'ensemble forme une
 * seule place : au moins une piece (regle du 17/11/2026, conservee).</p>
 *
 * <p>Seul calcul des places : fiche du dossier, recalcul automatique et portail du titulaire.</p>
 */
public final class KycRequirementSlots {

    private KycRequirementSlots() {
    }

    /** Une place a remplir : une exigence seule ou un groupe d'alternatives. */
    public record Slot(String key, String label, boolean required, boolean group, List<DocumentRequirement> members) {

        public List<String> codes() {
            return members.stream().map(r -> code(r.getDocumentTypeCode())).toList();
        }
    }

    /**
     * @param complete           toutes les places a remplir ont une piece (verso compris)
     * @param allVerified        toutes les places a remplir ont une piece verifiee
     * @param blockingRejection  une piece refusee laisse sa place non verifiee
     * @param missing            places a remplir sans piece : code seul, {@code CODE_BACK} pour un verso
     *                           manquant, codes separes par {@code |} pour un groupe (une au choix)
     * @param anyRequired        au moins une place obligatoire existe
     */
    public record Evaluation(boolean complete, boolean allVerified, boolean blockingRejection, List<String> missing,
                             List<Slot> slots, boolean anyRequired) {

        /** La place de ce type est deja remplie par une autre piece de son groupe. */
        public boolean satisfiedByAlternative(String documentTypeCode, List<KycDocument> documents) {
            String code = code(documentTypeCode);
            return slots.stream().filter(Slot::group).filter(slot -> slot.codes().contains(code)).anyMatch(slot -> documents.stream()
                    .anyMatch(doc -> !code(doc.getDocumentType()).equals(code) && slot.codes().contains(code(doc.getDocumentType()))
                            && doc.getStatus() != KycDocumentVerificationStatus.REJECTED
                            && doc.getStatus() != KycDocumentVerificationStatus.EXPIRED));
        }
    }

    public static List<Slot> slots(List<DocumentRequirement> requirements) {
        Map<String, List<DocumentRequirement>> byKey = new LinkedHashMap<>();
        for (DocumentRequirement requirement : requirements) {
            String group = group(requirement);
            String key = group != null ? "GROUP:" + group : "TYPE:" + code(requirement.getDocumentTypeCode());
            byKey.computeIfAbsent(key, k -> new ArrayList<>()).add(requirement);
        }
        List<Slot> slots = new ArrayList<>();
        byKey.forEach((key, members) -> {
            boolean group = key.startsWith("GROUP:");
            boolean required = members.stream().anyMatch(r -> Boolean.TRUE.equals(r.getRequired()));
            String label = group
                    ? members.stream().map(DocumentRequirement::getAlternativeGroupLabel).filter(StringUtils::hasText).findFirst()
                            .orElse(members.stream().map(DocumentRequirement::getDocumentTypeName).collect(Collectors.joining(" ou ")))
                    : members.get(0).getDocumentTypeName();
            slots.add(new Slot(key, label, required, group, List.copyOf(members)));
        });
        return slots;
    }

    /**
     * @param requiresBack   le type de piece exige un verso
     * @param backVerified   le verso de la piece est approuve (ou n'est pas exige)
     */
    public static Evaluation evaluate(List<DocumentRequirement> requirements, List<KycDocument> documents,
                                      Predicate<String> requiresBack, Predicate<KycDocument> backVerified) {
        List<Slot> slots = slots(requirements);
        boolean anyRequired = slots.stream().anyMatch(Slot::required);
        // Toutes facultatives : une seule place, une piece au moins.
        List<Slot> toFill = anyRequired ? slots.stream().filter(Slot::required).toList()
                : (requirements.isEmpty() ? List.of() : List.of(new Slot("ANY", "Au moins une pièce", true, true, List.copyOf(requirements))));

        List<String> missing = new ArrayList<>();
        boolean allVerified = true;
        for (Slot slot : toFill) {
            List<KycDocument> pieces = piecesOf(slot, documents);
            boolean verified = pieces.stream().anyMatch(doc -> doc.getStatus() == KycDocumentVerificationStatus.VERIFIED && backVerified.test(doc));
            allVerified &= verified;
            Optional<KycDocument> usable = pieces.stream()
                    .filter(doc -> doc.getStatus() != KycDocumentVerificationStatus.REJECTED && doc.getStatus() != KycDocumentVerificationStatus.EXPIRED)
                    .findFirst();
            KycDocument candidate = usable.orElse(pieces.isEmpty() ? null : pieces.get(0));
            if (candidate == null) {
                missing.add(slot.group() ? String.join("|", slot.codes()) : slot.codes().get(0));
            } else if (requiresBack.test(code(candidate.getDocumentType())) && candidate.getBackDocument() == null && !verified) {
                missing.add(code(candidate.getDocumentType()) + "_BACK");
            }
        }
        boolean blocking = false;
        for (Slot slot : anyRequired ? slots : toFill) {
            List<KycDocument> pieces = piecesOf(slot, documents);
            boolean verified = pieces.stream().anyMatch(doc -> doc.getStatus() == KycDocumentVerificationStatus.VERIFIED && backVerified.test(doc));
            boolean usable = pieces.stream().anyMatch(doc -> doc.getStatus() != KycDocumentVerificationStatus.REJECTED
                    && doc.getStatus() != KycDocumentVerificationStatus.EXPIRED);
            if (!verified && !usable && pieces.stream().anyMatch(doc -> doc.getStatus() == KycDocumentVerificationStatus.REJECTED)) {
                blocking = true;
            }
        }
        boolean complete = missing.isEmpty();
        return new Evaluation(complete, !toFill.isEmpty() ? allVerified : true, blocking, List.copyOf(missing), slots, anyRequired);
    }

    private static List<KycDocument> piecesOf(Slot slot, List<KycDocument> documents) {
        List<String> codes = slot.codes();
        return documents.stream().filter(doc -> codes.contains(code(doc.getDocumentType()))).toList();
    }

    static String group(DocumentRequirement requirement) {
        String group = requirement.getAlternativeGroup();
        return StringUtils.hasText(group) ? group.trim().toUpperCase(Locale.ROOT) : null;
    }

    static String code(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
