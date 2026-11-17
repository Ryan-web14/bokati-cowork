package com.sni.bokaticowork.security.authorization;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Centre KYC et GED, lot KG0 : droits des routes KYC selon la methode et le suffixe exact,
 * ecritures documentaires reservees au droit d'ecriture, revue ouverte aux droits d'espace.
 */
class KycGedRightsKg0Test {

    private static final String BASE = "/sni/api/v1";
    private final AdminApiAuthorizationManager manager = new AdminApiAuthorizationManager();

    private String required(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        manager.authorize(() -> auth(List.of("ROLE_ADMIN")), new RequestAuthorizationContext(request));
        return (String) request.getAttribute(AdminApiAuthorizationManager.REQUIRED_PERMISSION_ATTRIBUTE);
    }

    private boolean granted(List<String> authorities, String method, String uri) {
        AuthorizationResult result = manager.authorize(() -> auth(authorities),
                new RequestAuthorizationContext(new MockHttpServletRequest(method, uri)));
        return result instanceof AuthorizationDecision decision && decision.isGranted();
    }

    private static TestingAuthenticationToken auth(List<String> authorities) {
        TestingAuthenticationToken token = new TestingAuthenticationToken("awa@example.com", null, authorities.toArray(String[]::new));
        token.setAuthenticated(true);
        return token;
    }

    @Test
    void kycReadsNeedReadAndAliasesAreReads() {
        assertThat(required("GET", BASE + "/kyc/documents/disapproved")).isEqualTo("KYC:READ");
        assertThat(required("GET", BASE + "/kyc/documents/desapproved")).isEqualTo("KYC:READ");
        assertThat(required("GET", BASE + "/kyc/documents/approved")).isEqualTo("KYC:READ");
        assertThat(required("GET", BASE + "/kyc/cases/KYC-1")).isEqualTo("KYC:READ");
    }

    @Test
    void kycWritesNeedManageAndDecisionsTheirOwnRight() {
        assertThat(required("POST", BASE + "/kyc/cases")).isEqualTo("KYC:MANAGE");
        assertThat(required("POST", BASE + "/kyc/cases/KYC-1/submit")).isEqualTo("KYC:MANAGE");
        assertThat(required("PATCH", BASE + "/kyc/cases/KYC-1/assign")).isEqualTo("KYC:MANAGE");
        assertThat(required("POST", BASE + "/kyc/cases/KYC-1/notes")).isEqualTo("KYC:MANAGE");
        assertThat(required("POST", BASE + "/kyc/cases/KYC-1/requirements")).isEqualTo("KYC:MANAGE");
        assertThat(required("POST", BASE + "/kyc/documents/admin/upload")).isEqualTo("KYC:MANAGE");
        assertThat(required("POST", BASE + "/kyc/cases/KYC-1/approve")).isEqualTo("KYC:APPROVE");
        assertThat(required("POST", BASE + "/kyc/documents/bulk-approve")).isEqualTo("KYC:APPROVE");
        assertThat(required("POST", BASE + "/kyc/cases/KYC-1/reject")).isEqualTo("KYC:REJECT");
        assertThat(required("POST", BASE + "/kyc/documents/bulk-desapprove")).isEqualTo("KYC:REJECT");
        assertThat(required("POST", BASE + "/kyc/cases/KYC-1/documents/request-correction")).isEqualTo("KYC:REJECT");
        // Un lecteur KYC ne cree plus de dossier.
        assertThat(granted(List.of("ROLE_ADMIN", "KYC_READ", "KYC:READ"), "POST", BASE + "/kyc/cases")).isFalse();
        assertThat(granted(List.of("ROLE_ADMIN", "KYC_MANAGE"), "POST", BASE + "/kyc/cases")).isTrue();
    }

    @Test
    void documentWritesNeedWriteAndSigningOnlyRead() {
        assertThat(required("PATCH", BASE + "/documents/DOC-1/metadata")).isEqualTo("DOCUMENT:WRITE");
        assertThat(required("DELETE", BASE + "/documents/DOC-1/metadata/ref")).isEqualTo("DOCUMENT:WRITE");
        assertThat(required("DELETE", BASE + "/documents/DOC-1/tags/URGENT")).isEqualTo("DOCUMENT:WRITE");
        assertThat(required("PATCH", BASE + "/documents/DOC-1/signatures/4/sign")).isEqualTo("DOCUMENT:READ");
        assertThat(required("POST", BASE + "/documents/upload")).isEqualTo("DOCUMENT:UPLOAD");
        assertThat(required("GET", BASE + "/documents/DOC-1")).isEqualTo("DOCUMENT:READ");
        assertThat(required("POST", BASE + "/document-tags")).isEqualTo("DOCUMENT:WRITE");
        assertThat(required("POST", BASE + "/document-workflows")).isEqualTo("SYSTEM:SETTINGS");
        assertThat(required("POST", BASE + "/spaces/financial/documents/upload")).isEqualTo("DOCUMENT:UPLOAD");
    }

    @Test
    void aSpaceReviewRightOpensReviewRoutes() {
        List<String> financeReviewer = List.of("ROLE_ADMIN", "DOCUMENT_REVIEW_FINANCIAL_SPACE");
        assertThat(granted(financeReviewer, "POST", BASE + "/documents/DOC-1/approve")).isTrue();
        assertThat(granted(financeReviewer, "GET", BASE + "/review/queue")).isTrue();
        assertThat(granted(financeReviewer, "POST", BASE + "/review/FINANCIAL_SPACE/bulk-approve")).isTrue();
        assertThat(granted(List.of("ROLE_ADMIN", "DOCUMENT_READ"), "POST", BASE + "/documents/DOC-1/approve")).isFalse();
        assertThat(required("POST", BASE + "/documents/DOC-1/approve")).isEqualTo("DOCUMENT:REVIEW (ou un droit de revue d'espace)");
    }
}
