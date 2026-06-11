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

    private boolean hasAuthority(Authentication auth, String authority) {
        return auth.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }
}
