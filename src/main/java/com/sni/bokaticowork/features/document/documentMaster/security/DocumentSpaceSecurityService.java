package com.sni.bokaticowork.features.document.documentMaster.security;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("docSpaceSecurity")
public class DocumentSpaceSecurityService {

    public boolean canReview(Authentication authentication, DocumentSpace space) {
        if (authentication == null) return false;
        if (hasAuthority(authentication, "DOCUMENT_REVIEW") || hasAuthority(authentication, "DOCUMENT:REVIEW")) {
            return true;
        }
        if (space == null) return false;
        return hasAuthority(authentication, "DOCUMENT_REVIEW_" + space.name())
                || hasAuthority(authentication, "DOCUMENT:REVIEW_" + space.name());
    }

    /** Revue d'au moins un espace (lot KG0). */
    public boolean canReviewAny(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .anyMatch(a -> a != null && (a.startsWith("DOCUMENT:REVIEW") || a.startsWith("DOCUMENT_REVIEW")));
    }

    /** Espaces que ce compte peut revoir ; {@code null} : tous (DOCUMENT:REVIEW ou super administrateur). */
    public java.util.Set<DocumentSpace> reviewableSpaces(Authentication authentication) {
        if (authentication == null) return java.util.Set.of();
        if (hasAuthority(authentication, "DOCUMENT_REVIEW") || hasAuthority(authentication, "DOCUMENT:REVIEW")
                || hasAuthority(authentication, com.sni.bokaticowork.security.authorization.SecurityRoles.SUPER_ADMIN)) {
            return null;
        }
        java.util.Set<DocumentSpace> spaces = java.util.EnumSet.noneOf(DocumentSpace.class);
        for (DocumentSpace space : DocumentSpace.values()) {
            if (canReview(authentication, space)) spaces.add(space);
        }
        return spaces;
    }

    private boolean hasAuthority(Authentication auth, String authority) {
        return auth.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }
}
