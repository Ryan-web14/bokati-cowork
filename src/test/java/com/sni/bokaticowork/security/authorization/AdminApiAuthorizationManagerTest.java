package com.sni.bokaticowork.security.authorization;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdminApiAuthorizationManagerTest {

    private static final String BASE = "/sni/api/v1";

    private final AdminApiAuthorizationManager manager = new AdminApiAuthorizationManager();

    /**
     * Les autorites d'un ADMIN telles que {@code UserPrincipal.resolveAuthorities} les construit :
     * le role, puis chaque permission sous ses deux formes (nom et MODULE:ACTION).
     */
    private static final List<String> ADMIN_AUTHORITIES = List.of(
            "ROLE_ADMIN",
            "BILLING_CANCEL", "BILLING:CANCEL",
            "BILLING_CREATE", "BILLING:CREATE",
            "BILLING_READ", "BILLING:READ",
            "BILLING_SEND", "BILLING:SEND",
            "BILLING_UPDATE", "BILLING:UPDATE",
            "BOOKING_UPDATE", "BOOKING:UPDATE",
            "SUBSCRIPTION_UPDATE", "SUBSCRIPTION:UPDATE"
    );

    @Test
    void shouldLetAnAdminCancelABillingDocument() {
        // Regression : DELETE se resolvait en BILLING:DELETE, permission absente du referentiel,
        // donc refusee pour tout le monde. L'administrateur recevait un 403 sur une annulation
        // qu'il avait le droit de faire, avant meme d'atteindre le controleur.
        assertThat(granted("DELETE", BASE + "/billing/documents/INV-001")).isTrue();
    }

    @Test
    void shouldLetAnAdminRemoveABookingParticipantAndASubscriptionSeat() {
        // Meme defaut sur deux modules qui n'ont pas non plus de permission DELETE en base.
        assertThat(granted("DELETE", BASE + "/bookings/BK-001/participants/12")).isTrue();
        assertThat(granted("DELETE", BASE + "/subscriptions/SUB-001/seats/MBR-1")).isTrue();
    }

    @Test
    void shouldStillRequireTheCancelPermissionAndNotSettleForUpdate() {
        // Annuler est plus lourd que corriger : un profil qui ne peut que modifier ne doit pas
        // pouvoir annuler une facture.
        Collection<String> withoutCancel = ADMIN_AUTHORITIES.stream()
                .filter(authority -> !authority.contains("BILLING_CANCEL") && !authority.contains("BILLING:CANCEL"))
                .toList();

        assertThat(granted(withoutCancel, "DELETE", BASE + "/billing/documents/INV-001")).isFalse();
        assertThat(granted(withoutCancel, "PUT", BASE + "/billing/documents/INV-001")).isTrue();
    }

    @Test
    void shouldKeepResolvingTheOtherBillingActions() {
        assertThat(granted("GET", BASE + "/billing/documents/INV-001")).isTrue();
        assertThat(granted("POST", BASE + "/billing/documents")).isTrue();
        assertThat(granted("PATCH", BASE + "/billing/documents/INV-001/send")).isTrue();
        assertThat(granted("POST", BASE + "/billing/invoices/INV-001/credit-note")).isTrue();
    }

    @Test
    void shouldRequireBillingUpdateToIssueAnInvoice() {
        assertThat(requiredPermissionFor("PATCH", BASE + "/billing/documents/INV-001/issue"))
                .isEqualTo("BILLING:UPDATE");
        assertThat(requiredPermissionFor("PATCH", BASE + "/billing/documents/INV-001/validate"))
                .isEqualTo("BILLING:UPDATE");
    }

    @Test
    void shouldNotRequireSystemSettingsToReadTheNotificationBell() {
        // Regression : AdminNotificationController est monte sous /admin/notifications, donc le
        // repli /admin/ exigeait SYSTEM:SETTINGS pour afficher le compteur de non-lus. Un admin
        // sans droit de configuration systeme recevait un 403 sur sa propre cloche.
        assertThat(requiredPermissionFor("GET", BASE + "/admin/notifications/unread-count"))
                .isEqualTo("ADMIN:ACCESS");
        assertThat(requiredPermissionFor("GET", BASE + "/admin/notifications/unread"))
                .isEqualTo("ADMIN:ACCESS");

        // Les autres routes /admin/ restent sur SYSTEM:SETTINGS.
        assertThat(requiredPermissionFor("GET", BASE + "/admin/settings"))
                .isEqualTo("SYSTEM:SETTINGS");
    }

    @Test
    void shouldExposeTheRequiredPermissionOnDenialSoA403IsDiagnosable() {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", BASE + "/billing/documents/INV-001/issue");
        manager.authorize(() -> authentication(List.of("ROLE_ADMIN")), new RequestAuthorizationContext(request));

        // Sans cet attribut, un 403 en production ne permet pas de distinguer un droit
        // reellement absent d'une permission mal resolue par le mapping de chemins.
        assertThat(request.getAttribute(AdminApiAuthorizationManager.REQUIRED_PERMISSION_ATTRIBUTE))
                .isEqualTo("BILLING:UPDATE");
    }

    @Test
    void shouldReportTheGenericFallbackWhenNoPathRuleMatches() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", BASE + "/unmapped-module/items");
        manager.authorize(() -> authentication(List.of("ROLE_ADMIN")), new RequestAuthorizationContext(request));

        assertThat(request.getAttribute(AdminApiAuthorizationManager.REQUIRED_PERMISSION_ATTRIBUTE))
                .isEqualTo("ADMIN:ACCESS");
    }

    /** Renvoie la permission exigee en refusant l'appel avec un compte sans aucune permission. */
    private String requiredPermissionFor(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        manager.authorize(() -> authentication(List.of("ROLE_ADMIN")), new RequestAuthorizationContext(request));
        return (String) request.getAttribute(AdminApiAuthorizationManager.REQUIRED_PERMISSION_ATTRIBUTE);
    }

    @Test
    void shouldStillDenyAnAuthenticatedUserWithoutAnyAdminRealmRole() {
        assertThat(granted(List.of("ROLE_MEMBER"), "DELETE", BASE + "/billing/documents/INV-001")).isFalse();
    }

    private boolean granted(String method, String uri) {
        return granted(ADMIN_AUTHORITIES, method, uri);
    }

    private boolean granted(Collection<String> authorities, String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        AuthorizationResult result = manager.authorize(
                () -> authentication(authorities),
                new RequestAuthorizationContext(request));
        return result instanceof AuthorizationDecision decision && decision.isGranted();
    }

    private Authentication authentication(Collection<String> authorities) {
        List<GrantedAuthority> granted = authorities.stream()
                .map(authority -> (GrantedAuthority) new SimpleGrantedAuthority(authority))
                .toList();
        return new TestingAuthentication(granted);
    }

    private record TestingAuthentication(Collection<? extends GrantedAuthority> authorities)
            implements Authentication {

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getDetails() {
            return null;
        }

        @Override
        public Object getPrincipal() {
            return "admin@example.com";
        }

        @Override
        public boolean isAuthenticated() {
            return true;
        }

        @Override
        public void setAuthenticated(boolean isAuthenticated) {
            // immuable dans ce test
        }

        @Override
        public String getName() {
            return "admin@example.com";
        }
    }
}
