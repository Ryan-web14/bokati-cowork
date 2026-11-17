package com.sni.bokaticowork.features.document.documentMaster.security;

import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import com.sni.bokaticowork.security.authorization.SecurityRoles;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Droit de decider sur un document (centre KYC et GED, lot KG0), controle par le service pour
 * toutes les voies : route unitaire, operations en masse, circuits.
 *
 * <ul>
 *   <li>piece de l'espace KYC : {@code KYC:APPROVE} pour approuver, {@code KYC:REJECT} pour refuser ou
 *   demander une correction (le droit de revue documentaire ne suffit plus a faire aboutir un dossier
 *   KYC) ;</li>
 *   <li>autre document : {@code DOCUMENT:REVIEW} ou le droit de revue de son espace
 *   ({@code DOCUMENT:REVIEW_<ESPACE>}).</li>
 * </ul>
 * Une tache du systeme (sans compte connecte) n'est pas controlee.
 */
@Component
@RequiredArgsConstructor
public class DocumentReviewGuard {

    public enum Decision { APPROVE, REJECT, CORRECTION }

    private final DocumentSpaceSecurityService spaceSecurity;

    private static final ThreadLocal<Boolean> WORKFLOW = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * Lot KG5 : la decision finale d'un circuit d'approbation est prise par l'approbateur designe
     * de l'etape (controle par le circuit), meme sans droit de revue de l'espace.
     */
    public static void asWorkflow(Runnable decision) {
        boolean previous = WORKFLOW.get();
        WORKFLOW.set(Boolean.TRUE);
        try {
            decision.run();
        } finally {
            WORKFLOW.set(previous);
        }
    }

    public void check(DocumentSpace space, Decision decision) {
        if (Boolean.TRUE.equals(WORKFLOW.get())) {
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            return;
        }
        if (has(authentication, SecurityRoles.SUPER_ADMIN)) {
            return;
        }
        if (space == DocumentSpace.KYC_SPACE) {
            String needed = decision == Decision.APPROVE ? "KYC:APPROVE" : "KYC:REJECT";
            if (has(authentication, needed) || has(authentication, needed.replace(':', '_'))) {
                return;
            }
            throw new ForbiddenException("Décision sur une pièce KYC refusée : le droit " + needed + " est nécessaire");
        }
        if (!spaceSecurity.canReview(authentication, space)) {
            throw new ForbiddenException("Décision refusée : pas de droit de revue pour l'espace " + (space == null ? "inconnu" : space.name()));
        }
    }

    private static boolean has(Authentication authentication, String authority) {
        for (GrantedAuthority granted : authentication.getAuthorities()) {
            if (authority.equals(granted.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
