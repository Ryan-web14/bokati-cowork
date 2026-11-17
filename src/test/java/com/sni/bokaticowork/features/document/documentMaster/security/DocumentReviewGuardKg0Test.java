package com.sni.bokaticowork.features.document.documentMaster.security;

import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import com.sni.bokaticowork.security.admin.user.model.Users;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lot KG0 : une piece KYC se decide avec les droits KYC (le droit de revue documentaire ne suffit
 * plus a faire aboutir un dossier) ; un autre document avec le droit de revue de son espace.
 */
class DocumentReviewGuardKg0Test {

    private final DocumentReviewGuard guard = new DocumentReviewGuard(new DocumentSpaceSecurityService());

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void login(String... authorities) {
        Users user = Users.builder().id(7L).email("awa@example.com").build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(user), null,
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }

    @Test
    void aDocumentReviewerCannotDecideOnAKycPiece() {
        login("DOCUMENT:REVIEW", "DOCUMENT_REVIEW");
        assertThatThrownBy(() -> guard.check(DocumentSpace.KYC_SPACE, DocumentReviewGuard.Decision.APPROVE))
                .isInstanceOf(ForbiddenException.class).hasMessageContaining("KYC:APPROVE");
        assertThatThrownBy(() -> guard.check(DocumentSpace.KYC_SPACE, DocumentReviewGuard.Decision.CORRECTION))
                .isInstanceOf(ForbiddenException.class).hasMessageContaining("KYC:REJECT");
    }

    @Test
    void kycRightsDecideOnKycPieces() {
        login("KYC_APPROVE", "KYC:APPROVE");
        assertThatCode(() -> guard.check(DocumentSpace.KYC_SPACE, DocumentReviewGuard.Decision.APPROVE)).doesNotThrowAnyException();
        assertThatThrownBy(() -> guard.check(DocumentSpace.KYC_SPACE, DocumentReviewGuard.Decision.REJECT)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void aSpaceReviewerDecidesOnlyInItsSpace() {
        login("DOCUMENT_REVIEW_FINANCIAL_SPACE");
        assertThatCode(() -> guard.check(DocumentSpace.FINANCIAL_SPACE, DocumentReviewGuard.Decision.REJECT)).doesNotThrowAnyException();
        assertThatThrownBy(() -> guard.check(DocumentSpace.CONTRACT_SPACE, DocumentReviewGuard.Decision.APPROVE))
                .isInstanceOf(ForbiddenException.class).hasMessageContaining("CONTRACT_SPACE");
    }

    @Test
    void systemTasksAreNotChecked() {
        assertThatCode(() -> guard.check(DocumentSpace.KYC_SPACE, DocumentReviewGuard.Decision.APPROVE)).doesNotThrowAnyException();
    }
}
