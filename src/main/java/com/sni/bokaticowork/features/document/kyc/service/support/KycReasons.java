package com.sni.bokaticowork.features.document.kyc.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.document.kyc.model.KycReason;
import com.sni.bokaticowork.features.document.kyc.repository.KycReasonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lot KG7 : liste des motifs de refus et de correction. Un motif choisi doit exister, etre actif et
 * convenir a la decision ; « Autre, preciser » exige un texte. Le libelle enregistre sur la decision
 * est celui de la liste, suivi du texte libre.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class KycReasons {

    public static final String REJECTION = "REJECTION";
    public static final String CORRECTION = "CORRECTION";
    public static final String BOTH = "BOTH";
    private static final Set<String> KINDS = Set.of(REJECTION, CORRECTION, BOTH);

    private final KycReasonRepository repository;

    public record ReasonView(String code, String label, String kind, boolean requiresText, boolean active,
                             int sortOrder, boolean systemReason) {
        static ReasonView of(KycReason reason) {
            return new ReasonView(reason.getCode(), reason.getLabel(), reason.getKind(), Boolean.TRUE.equals(reason.getRequiresText()),
                    Boolean.TRUE.equals(reason.getActive()), reason.getSortOrder() == null ? 0 : reason.getSortOrder(),
                    Boolean.TRUE.equals(reason.getSystemReason()));
        }
    }

    public record ReasonRequest(String code, String label, String kind, Boolean requiresText, Boolean active, Integer sortOrder) {
    }

    /** Motifs, filtres par decision ({@code REJECTION} ou {@code CORRECTION}) ; actifs seulement si demande. */
    @Transactional(readOnly = true)
    public List<ReasonView> list(String kind, boolean activeOnly) {
        String wanted = StringUtils.hasText(kind) ? kind.trim().toUpperCase(Locale.ROOT) : null;
        return repository.findAllByOrderBySortOrderAscLabelAsc().stream()
                .filter(r -> !activeOnly || Boolean.TRUE.equals(r.getActive()))
                .filter(r -> wanted == null || BOTH.equals(r.getKind()) || wanted.equals(r.getKind()))
                .map(ReasonView::of)
                .toList();
    }

    /**
     * Libelle a enregistrer pour une decision : libelle du motif, suivi du texte s'il y en a un ;
     * sans motif, le texte seul (obligatoire).
     */
    @Transactional(readOnly = true)
    public String describe(String code, String text, String kind) {
        String precision = StringUtils.hasText(text) ? text.trim() : null;
        if (!StringUtils.hasText(code)) {
            if (precision == null) {
                throw new BadRequestException("Le motif est requis (reasonCode dans la liste, ou comment)");
            }
            return precision;
        }
        KycReason reason = repository.findByCode(code.trim().toUpperCase(Locale.ROOT))
                .filter(r -> Boolean.TRUE.equals(r.getActive()))
                .orElseThrow(() -> new BadRequestException("Motif inconnu ou désactivé : " + code));
        if (!BOTH.equals(reason.getKind()) && !reason.getKind().equals(kind)) {
            throw new BadRequestException("Le motif « " + reason.getLabel() + " » ne s'emploie pas pour "
                    + (REJECTION.equals(kind) ? "un refus définitif" : "une demande de correction"));
        }
        if (Boolean.TRUE.equals(reason.getRequiresText()) && precision == null) {
            throw new BadRequestException("Précisez le motif « " + reason.getLabel() + " » dans comment");
        }
        return precision == null ? reason.getLabel() : reason.getLabel() + " : " + precision;
    }

    @Transactional(readOnly = true)
    public String label(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        return repository.findByCode(code).map(KycReason::getLabel).orElse(code);
    }

    public ReasonView create(ReasonRequest request) {
        if (request == null || !StringUtils.hasText(request.code()) || !StringUtils.hasText(request.label())) {
            throw new BadRequestException("Le code et le libellé du motif sont requis");
        }
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (repository.findByCode(code).isPresent()) {
            throw new BadRequestException("Le motif " + code + " existe déjà");
        }
        KycReason reason = KycReason.builder().code(code).label(request.label().trim()).kind(kind(request.kind()))
                .requiresText(Boolean.TRUE.equals(request.requiresText()))
                .active(request.active() == null || request.active())
                .sortOrder(request.sortOrder() == null ? 500 : request.sortOrder())
                .build();
        return ReasonView.of(repository.save(reason));
    }

    public ReasonView update(String code, ReasonRequest request) {
        KycReason reason = find(code);
        if (request.label() != null && StringUtils.hasText(request.label())) {
            reason.setLabel(request.label().trim());
        }
        if (request.kind() != null) {
            reason.setKind(kind(request.kind()));
        }
        if (request.requiresText() != null) {
            reason.setRequiresText(request.requiresText());
        }
        if (request.active() != null) {
            reason.setActive(request.active());
        }
        if (request.sortOrder() != null) {
            reason.setSortOrder(request.sortOrder());
        }
        return ReasonView.of(repository.save(reason));
    }

    public void delete(String code) {
        KycReason reason = find(code);
        if (Boolean.TRUE.equals(reason.getSystemReason())) {
            throw new BadRequestException("Motif préenregistré : le désactiver plutôt que le supprimer");
        }
        repository.delete(reason);
    }

    private KycReason find(String code) {
        return repository.findByCode(code == null ? "" : code.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Motif introuvable : " + code));
    }

    private static String kind(String value) {
        String kind = StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : BOTH;
        if (!KINDS.contains(kind)) {
            throw new BadRequestException("Type de motif invalide : " + value + " (REJECTION, CORRECTION ou BOTH)");
        }
        return kind;
    }
}
