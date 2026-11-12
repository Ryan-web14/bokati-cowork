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
    void shouldSeparateMaintenanceReadsFromMaintenanceWrites() {
        // Previsualiser ce qu'une purge emporterait ne modifie rien : exiger SYSTEM:SETTINGS pour
        // cette lecture empechait de verifier avant de supprimer. La suppression elle-meme reste
        // sur le droit de configuration systeme.
        assertThat(requiredPermissionFor("GET", BASE + "/admin/maintenance/members/MBR-001/purge-preview"))
                .isEqualTo("ADMIN:ACCESS");
        assertThat(requiredPermissionFor("GET", BASE + "/admin/maintenance/members/purge-candidates"))
                .isEqualTo("ADMIN:ACCESS");

        assertThat(requiredPermissionFor("DELETE", BASE + "/admin/maintenance/members/MBR-001"))
                .isEqualTo("SYSTEM:SETTINGS");
        assertThat(requiredPermissionFor("POST", BASE + "/admin/maintenance/members/purge-batch"))
                .isEqualTo("SYSTEM:SETTINGS");
        assertThat(requiredPermissionFor("DELETE", BASE + "/admin/maintenance/purge-data"))
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

    @Test
    void shouldRequireTheKycDecisionPermissionsForBulkReviews() {
        // Regression : seuls /approve et /reject etaient reconnus. Les validations et refus en
        // masse tombaient sur KYC:READ, un simple droit de lecture suffisait a les decider.
        assertThat(requiredPermissionFor("POST", BASE + "/kyc/documents/bulk-approve"))
                .isEqualTo("KYC:APPROVE");
        assertThat(requiredPermissionFor("POST", BASE + "/kyc/cases/KYC-1/documents/bulk-approve"))
                .isEqualTo("KYC:APPROVE");
        assertThat(requiredPermissionFor("POST", BASE + "/kyc/documents/bulk-reject"))
                .isEqualTo("KYC:REJECT");
        assertThat(requiredPermissionFor("POST", BASE + "/kyc/documents/bulk-disapprove"))
                .isEqualTo("KYC:REJECT");
        assertThat(requiredPermissionFor("POST", BASE + "/kyc/documents/bulk-desapprove"))
                .isEqualTo("KYC:REJECT");
        assertThat(requiredPermissionFor("POST", BASE + "/kyc/cases/KYC-1/documents/bulk-reject"))
                .isEqualTo("KYC:REJECT");
        assertThat(requiredPermissionFor("GET", BASE + "/kyc/documents/review-queue"))
                .isEqualTo("KYC:READ");
    }

    @Test
    void shouldRequireTheExportPermissionForEveryReportDownload() {
        // Regression : la regle attendait .csv ou /export, or les rapports se telechargent par
        // /csv et /pdf. REPORT:VIEW suffisait donc a exporter.
        List<String> exports = List.of(
                "/reports/activity/csv", "/reporting/activity/csv",
                "/reports/activity/pdf", "/reporting/activity/pdf",
                "/reports/debt-recovery/csv", "/reporting/debt-recovery/csv",
                "/reports/debt-recovery/pdf", "/reporting/debt-recovery/pdf",
                "/reports/finance/dashboard/pdf", "/reporting/dashboard/pdf", "/reporting/finance/dashboard/pdf",
                "/reports/member-profile/12/pdf", "/reporting/member-profile/12/pdf",
                "/reports/occupancy/pdf", "/reporting/occupancy/pdf",
                "/reports/resource-utilization/pdf", "/reporting/resource-utilization/pdf",
                "/reports/stock-valuation/pdf", "/reporting/stock-valuation/pdf",
                "/reports/subscriptions/pdf", "/reporting/subscriptions/pdf");
        for (String export : exports) {
            assertThat(requiredPermissionFor("GET", BASE + export)).as(export).isEqualTo("REPORT:EXPORT");
        }
        assertThat(requiredPermissionFor("GET", BASE + "/reports/activity")).isEqualTo("REPORT:VIEW");
        assertThat(requiredPermissionFor("GET", BASE + "/reporting/finance/dashboard")).isEqualTo("REPORT:VIEW");
        assertThat(requiredPermissionFor("GET", BASE + "/analytics/overview")).isEqualTo("REPORT:VIEW");
    }

    /** Renvoie la permission exigee en refusant l'appel avec un compte sans aucune permission. */
    @Test
    void shouldGiveEachCashActionItsOwnPermission() {
        // Lot C0 : tout tombait sur CASH:ADJUST, que chaque caissier avait, y compris la
        // validation d'un ecart et la creation d'une caisse.
        assertThat(requiredPermissionFor("GET", BASE + "/cash-registers/sessions")).isEqualTo("CASH:READ");
        assertThat(requiredPermissionFor("GET", BASE + "/cash-registers/statistics/overview")).isEqualTo("CASH:READ");
        assertThat(requiredPermissionFor("POST", BASE + "/cash-registers")).isEqualTo("CASH_REGISTER:MANAGE");
        assertThat(requiredPermissionFor("PATCH", BASE + "/cash-registers/CSR-1/deactivate")).isEqualTo("CASH_REGISTER:MANAGE");
        assertThat(requiredPermissionFor("POST", BASE + "/cash-registers/sessions")).isEqualTo("CASH_SESSION:OPEN");
        assertThat(requiredPermissionFor("PATCH", BASE + "/cash-registers/sessions/CSS-1/close")).isEqualTo("CASH_SESSION:CLOSE");
        assertThat(requiredPermissionFor("POST", BASE + "/cash-registers/sessions/CSS-1/closing-request")).isEqualTo("CASH_SESSION:CLOSE");
        assertThat(requiredPermissionFor("PATCH", BASE + "/cash-registers/sessions/CSS-1/approve-variance")).isEqualTo("CASH_VARIANCE:DECIDE");
        assertThat(requiredPermissionFor("PATCH", BASE + "/cash-registers/anomalies/CAF-1/review")).isEqualTo("CASH_VARIANCE:DECIDE");
        assertThat(requiredPermissionFor("POST", BASE + "/cash-registers/sessions/CSS-1/movements")).isEqualTo("CASH_MOVEMENT:CREATE");
        assertThat(requiredPermissionFor("POST", BASE + "/cash-registers/sessions/CSS-1/exit-vouchers")).isEqualTo("CASH_MOVEMENT:CREATE");
        assertThat(requiredPermissionFor("POST", BASE + "/cash-registers/sessions/CSS-1/requests")).isEqualTo("CASH_MOVEMENT:CREATE");
        assertThat(requiredPermissionFor("PATCH", BASE + "/cash-registers/requests/CRQ-1/approve")).isEqualTo("CASH_MOVEMENT:APPROVE");
        assertThat(requiredPermissionFor("PATCH", BASE + "/cash-registers/requests/CRQ-1/reject")).isEqualTo("CASH_MOVEMENT:APPROVE");
    }

    @Test
    void shouldProtectTheCashRegisterReferential() {
        // Lot C1 : la fiche et le cycle de vie relevent de la gestion des caisses, le personnel
        // (affectations, remises) de la gestion des caissiers.
        assertThat(requiredPermissionFor("PATCH", BASE + "/cash-registers/CSR-1")).isEqualTo("CASH_REGISTER:MANAGE");
        assertThat(requiredPermissionFor("PATCH", BASE + "/cash-registers/CSR-1/suspend")).isEqualTo("CASH_REGISTER:MANAGE");
        assertThat(requiredPermissionFor("PATCH", BASE + "/cash-registers/CSR-1/close")).isEqualTo("CASH_REGISTER:MANAGE");
        assertThat(requiredPermissionFor("PATCH", BASE + "/cash-registers/CSR-1/devices/POS-7")).isEqualTo("CASH_REGISTER:MANAGE");
        assertThat(requiredPermissionFor("PUT", BASE + "/cash-registers/reference-lists/VARIANCE_REASON")).isEqualTo("CASH_REGISTER:MANAGE");
        assertThat(requiredPermissionFor("PUT", BASE + "/cash-registers/CSR-1/assignments")).isEqualTo("CASH_STAFF:MANAGE");
        assertThat(requiredPermissionFor("POST", BASE + "/cash-registers/CSR-1/emergency-assignments")).isEqualTo("CASH_STAFF:MANAGE");
        assertThat(requiredPermissionFor("POST", BASE + "/cash-registers/CSR-1/responsibility-transfers")).isEqualTo("CASH_STAFF:MANAGE");
        assertThat(requiredPermissionFor("PATCH", BASE + "/cash-registers/responsibility-transfers/PRC-1/sign")).isEqualTo("CASH_SESSION:OPEN");
        assertThat(requiredPermissionFor("PATCH", BASE + "/cash-registers/movements/CSM-1/sign")).isEqualTo("CASH_MOVEMENT:CREATE");
        assertThat(requiredPermissionFor("GET", BASE + "/cash-registers/CSR-1/history")).isEqualTo("CASH:READ");
    }

    @Test
    void shouldGuardTheCashOperationsAndDashboard() {
        // Lots C2, C3 et C6.
        assertThat(requiredPermissionFor("PATCH", BASE + "/cash-registers/sessions/CSS-1/suspend")).isEqualTo("CASH_SESSION:SUSPEND");
        assertThat(requiredPermissionFor("PATCH", BASE + "/cash-registers/sessions/CSS-1/force-close")).isEqualTo("CASH_SESSION:FORCE_CLOSE");
        assertThat(requiredPermissionFor("POST", BASE + "/cash-registers/sessions/CSS-1/counts")).isEqualTo("CASH_SESSION:CLOSE");
        assertThat(requiredPermissionFor("PATCH", BASE + "/cash-registers/sessions/CSS-1/pause")).isEqualTo("CASH_SESSION:PAUSE");
        assertThat(requiredPermissionFor("PATCH", BASE + "/cash-registers/handovers/PVP-1/accept")).isEqualTo("CASH_SESSION:OPEN");
        assertThat(requiredPermissionFor("PATCH", BASE + "/cash-registers/movements/CSM-1/approve")).isEqualTo("CASH_MOVEMENT:APPROVE");
        assertThat(requiredPermissionFor("POST", BASE + "/cash-registers/movements/CSM-1/reverse")).isEqualTo("CASH_MOVEMENT:CREATE");
        assertThat(requiredPermissionFor("POST", BASE + "/cash-registers/transfers")).isEqualTo("CASH_TRANSFER:SEND");
        assertThat(requiredPermissionFor("PATCH", BASE + "/cash-registers/transfers/OTI-1/receive")).isEqualTo("CASH_TRANSFER:RECEIVE");
        assertThat(requiredPermissionFor("POST", BASE + "/cash-registers/absences")).isEqualTo("CASH_STAFF:DECLARE_ABSENCE");
        assertThat(requiredPermissionFor("PATCH", BASE + "/cash-registers/absences/ABS-1/approve")).isEqualTo("CASH_STAFF:MANAGE");
        assertThat(requiredPermissionFor("GET", BASE + "/cash-registers/position")).isEqualTo("CASH_REPORT:READ");
        assertThat(requiredPermissionFor("GET", BASE + "/cash-registers/reports/daily-blotter")).isEqualTo("CASH_REPORT:READ");
    }

    @Test
    void shouldGuardTheAccountingRoutes() {
        // Lot F1.
        assertThat(requiredPermissionFor("GET", BASE + "/accounting/reports/trial-balance")).isEqualTo("ACCOUNTING_REPORT:READ");
        assertThat(requiredPermissionFor("POST", BASE + "/accounting/accounts")).isEqualTo("ACCOUNTING_ACCOUNT:CREATE");
        assertThat(requiredPermissionFor("PATCH", BASE + "/accounting/accounts/6055/deactivate")).isEqualTo("ACCOUNTING_ACCOUNT:MANAGE");
        assertThat(requiredPermissionFor("POST", BASE + "/accounting/entries")).isEqualTo("ACCOUNTING_ENTRY:CREATE");
        assertThat(requiredPermissionFor("PATCH", BASE + "/accounting/entries/BR-1/validate")).isEqualTo("ACCOUNTING_ENTRY:VALIDATE");
        assertThat(requiredPermissionFor("PATCH", BASE + "/accounting/entries/OD-2026-000001/reverse")).isEqualTo("ACCOUNTING_ENTRY:REVERSE");
        assertThat(requiredPermissionFor("PATCH", BASE + "/accounting/entries/BR-1/reject")).isEqualTo("ACCOUNTING_ENTRY:BALANCE");
        assertThat(requiredPermissionFor("PATCH", BASE + "/accounting/periods/2026-01/lock")).isEqualTo("ACCOUNTING_PERIOD:LOCK");
        assertThat(requiredPermissionFor("PATCH", BASE + "/accounting/periods/2026-01/close")).isEqualTo("ACCOUNTING_PERIOD:CLOSE");
        assertThat(requiredPermissionFor("POST", BASE + "/accounting/journals")).isEqualTo("ACCOUNTING_JOURNAL:CREATE");
        assertThat(requiredPermissionFor("POST", BASE + "/accounting/lettering")).isEqualTo("ACCOUNTING_LETTERING:MANUAL");
        assertThat(requiredPermissionFor("PATCH", BASE + "/accounting/lettering/5/unletter")).isEqualTo("ACCOUNTING_LETTERING:UNLETTER");
        // Lot F2.
        assertThat(requiredPermissionFor("GET", BASE + "/accounting/posting/events")).isEqualTo("ACCOUNTING_POSTING:READ");
        assertThat(requiredPermissionFor("POST", BASE + "/accounting/posting/preview")).isEqualTo("ACCOUNTING_POSTING:READ");
        assertThat(requiredPermissionFor("POST", BASE + "/accounting/posting/rules")).isEqualTo("ACCOUNTING_POSTING:MANAGE");
        assertThat(requiredPermissionFor("PATCH", BASE + "/accounting/posting/rules/4/activate")).isEqualTo("ACCOUNTING_POSTING:MANAGE");
        assertThat(requiredPermissionFor("PATCH", BASE + "/accounting/posting/events/4/regenerate")).isEqualTo("ACCOUNTING_POSTING:REPLAY");
        assertThat(requiredPermissionFor("POST", BASE + "/accounting/posting/drafts/validate")).isEqualTo("ACCOUNTING_POSTING:REPLAY");
        // Lot F3.
        assertThat(requiredPermissionFor("GET", BASE + "/accounting/cash/registers/CSR-1/journals")).isEqualTo("CASH_MOVEMENT:CREATE");
        assertThat(requiredPermissionFor("POST", BASE + "/accounting/cash/registers/CSR-1/journals")).isEqualTo("ACCOUNTING_JOURNAL:CREATE");
        assertThat(requiredPermissionFor("PUT", BASE + "/accounting/cash/registers/CSR-1/journals/OD")).isEqualTo("ACCOUNTING_JOURNAL:MANAGE");
        assertThat(requiredPermissionFor("GET", BASE + "/accounting/cash/reconciliation")).isEqualTo("ACCOUNTING_REPORT:READ");
        assertThat(requiredPermissionFor("POST", BASE + "/accounting/cash/feed")).isEqualTo("ACCOUNTING_POSTING:REPLAY");
        // Lots F5 et F6.
        assertThat(requiredPermissionFor("POST", BASE + "/fund-requests")).isEqualTo("FUND_REQUEST:CREATE");
        assertThat(requiredPermissionFor("PATCH", BASE + "/fund-requests/DDF-1/approve")).isEqualTo("FUND_REQUEST:CREATE");
        assertThat(requiredPermissionFor("POST", BASE + "/fund-requests/DDF-1/disburse")).isEqualTo("FUND_REQUEST:DISBURSE");
        assertThat(requiredPermissionFor("PATCH", BASE + "/fund-requests/DDF-1/expenses/JUS-1/validate")).isEqualTo("FUND_JUSTIFICATION:VALIDATE");
        assertThat(requiredPermissionFor("GET", BASE + "/fund-requests")).isEqualTo("FUND_REQUEST:READ");
        assertThat(requiredPermissionFor("PATCH", BASE + "/fund-needs/EDB-1/validate")).isEqualTo("FUND_REQUEST:APPROVE_L1");
        // Lots F10 et C5.
        assertThat(requiredPermissionFor("POST", BASE + "/accounting/bank-accounts/BQ/statements")).isEqualTo("BANK_STATEMENT:IMPORT");
        assertThat(requiredPermissionFor("POST", BASE + "/accounting/bank-accounts/BQ/matching")).isEqualTo("BANK_RECONCILIATION:MANAGE");
        assertThat(requiredPermissionFor("PATCH", BASE + "/accounting/reconciliations/3/validate")).isEqualTo("BANK_RECONCILIATION:VALIDATE");
        assertThat(requiredPermissionFor("POST", BASE + "/accounting/bank-accounts")).isEqualTo("BANK_ACCOUNT:MANAGE");
        assertThat(requiredPermissionFor("POST", BASE + "/accounting/exchange-rates")).isEqualTo("CASH_EXCHANGE_RATE:MANAGE");
        assertThat(requiredPermissionFor("POST", BASE + "/accounting/exchange-rates/fetch")).isEqualTo("CASH_EXCHANGE_RATE:MANAGE");
        assertThat(requiredPermissionFor("GET", BASE + "/accounting/exchange-rates/board")).isEqualTo("CASH:READ");
        assertThat(requiredPermissionFor("GET", BASE + "/accounting/exchange-rates/convert")).isEqualTo("CASH:READ");
        assertThat(requiredPermissionFor("GET", BASE + "/accounting/reconciliations/3/pdf")).isEqualTo("ACCOUNTING_REPORT:READ");
        assertThat(requiredPermissionFor("GET", BASE + "/accounting/cash/registers/CSR-1/balances")).isEqualTo("CASH:READ");
        // Lot F11.
        assertThat(requiredPermissionFor("GET", BASE + "/accounting/vat-returns/4/pdf")).isEqualTo("VAT_RETURN:READ");
        assertThat(requiredPermissionFor("POST", BASE + "/accounting/vat-returns")).isEqualTo("VAT_RETURN:PREPARE");
        assertThat(requiredPermissionFor("PATCH", BASE + "/accounting/vat-returns/4/validate")).isEqualTo("VAT_RETURN:VALIDATE");
        assertThat(requiredPermissionFor("POST", BASE + "/accounting/vat-returns/4/liquidate")).isEqualTo("VAT_RETURN:VALIDATE");
        assertThat(requiredPermissionFor("DELETE", BASE + "/accounting/vat-returns/4")).isEqualTo("VAT_RETURN:PREPARE");
        // Lot F16.
        assertThat(requiredPermissionFor("GET", BASE + "/accounting/budget/reports/follow-up")).isEqualTo("BUDGET_REPORT:READ");
        assertThat(requiredPermissionFor("POST", BASE + "/accounting/budget/forecasts")).isEqualTo("BUDGET_FORECAST:MANAGE");
        assertThat(requiredPermissionFor("PATCH", BASE + "/accounting/budget/5/approve")).isEqualTo("BUDGET:PREPARE");
        // Lot F9.
        assertThat(requiredPermissionFor("GET", BASE + "/accounting/analytic/reports/dashboard")).isEqualTo("ANALYTIC_REPORT:READ");
        assertThat(requiredPermissionFor("POST", BASE + "/accounting/analytic/axes/POSTE/sections")).isEqualTo("ANALYTIC_ITEM:CREATE");
        assertThat(requiredPermissionFor("POST", BASE + "/accounting/analytic/reclass")).isEqualTo("ANALYTIC:RECLASS");
        assertThat(requiredPermissionFor("POST", BASE + "/accounting/analytic/rules")).isEqualTo("ANALYTIC_SETTINGS:MANAGE");
        // Lot F4.
        assertThat(requiredPermissionFor("GET", BASE + "/accounting/sales/checks")).isEqualTo("ACCOUNTING_REPORT:READ");
        assertThat(requiredPermissionFor("POST", BASE + "/accounting/sales/auto-letter")).isEqualTo("ACCOUNTING_LETTERING:MANUAL");
        assertThat(requiredPermissionFor("POST", BASE + "/accounting/sales/feed")).isEqualTo("ACCOUNTING_POSTING:REPLAY");
    }

    @Test
    void shouldNotLetACashierWithOnlyTheOldAdjustRightValidateAVariance() {
        List<String> cashier = List.of("ROLE_ADMIN", "CASH_READ", "CASH:READ", "CASH_ADJUST", "CASH:ADJUST",
                "CASH_SESSION_OPEN", "CASH_SESSION:OPEN", "CASH_SESSION_CLOSE", "CASH_SESSION:CLOSE",
                "CASH_MOVEMENT_CREATE", "CASH_MOVEMENT:CREATE");

        assertThat(granted(cashier, "PATCH", BASE + "/cash-registers/sessions/CSS-1/approve-variance")).isFalse();
        assertThat(granted(cashier, "POST", BASE + "/cash-registers")).isFalse();
        assertThat(granted(cashier, "POST", BASE + "/cash-registers/sessions")).isTrue();
        assertThat(granted(cashier, "POST", BASE + "/cash-registers/sessions/CSS-1/movements")).isTrue();
    }

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
