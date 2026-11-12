package com.sni.bokaticowork.security.authorization;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

@Slf4j
@Component
public class AdminApiAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private static final String SUPER_ADMIN = SecurityRoles.SUPER_ADMIN;
    /** La liste vit dans {@link SecurityRoles} · elle etait recopiee ici et ailleurs. */
    private static final Set<String> ADMIN_REALM_ROLES = SecurityRoles.ADMIN_REALM;

    /**
     * Seuls modules qui declarent reellement une permission {@code <MODULE>_DELETE} en base.
     * Toute resolution vers une permission absente du referentiel equivaut a un refus definitif,
     * quelle que soit la configuration des roles : cette liste doit rester alignee sur la table
     * {@code permission}.
     */
    private static final Set<String> MODULES_WITH_DELETE_PERMISSION = Set.of(
            "CLIENT",
            "INVENTORY",
            "RESOURCE"
    );

    /**
     * Attribut de requete portant la permission exigee lorsque l'acces est refuse.
     * <p>
     * Lu par le {@code accessDeniedHandler} de {@code SecurityConfig} pour que la reponse 403
     * nomme la permission manquante. Sans cela le client ne recoit qu'un "you do not have
     * permission to access this resource" indifferencie : impossible de distinguer un droit
     * reellement absent d'une permission mal resolue par le mapping de chemins, ce qui rend tout
     * incident de production non diagnosticable sans acces a la base.
     */
    public static final String REQUIRED_PERMISSION_ATTRIBUTE = "bokati.security.requiredPermission";

    @Override
    public AuthorizationResult authorize(Supplier<? extends Authentication> authentication, RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        if (auth == null || !auth.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }

        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (has(authorities, SUPER_ADMIN)) {
            return new AuthorizationDecision(true);
        }
        HttpServletRequest request = context.getRequest();
        if (ADMIN_REALM_ROLES.stream().noneMatch(role -> has(authorities, role))) {
            deny(request, auth, null, "aucun role du realm d'administration");
            return new AuthorizationDecision(false);
        }

        String requiredPermission = resolvePermission(request);
        if (!StringUtils.hasText(requiredPermission)) {
            boolean granted = has(authorities, "ADMIN:ACCESS") || has(authorities, "ADMIN_ACCESS");
            if (!granted) {
                // Aucune regle de chemin n'a matche : c'est le repli generique. Le signaler
                // explicitement evite de chercher un droit metier qui n'est jamais consulte.
                deny(request, auth, "ADMIN:ACCESS", "aucune regle de chemin ne couvre cette URL");
            }
            return new AuthorizationDecision(granted);
        }

        boolean granted = has(authorities, requiredPermission)
                || has(authorities, requiredPermission.replace(':', '_'));
        if (!granted) {
            deny(request, auth, requiredPermission, "permission absente des autorites de l'utilisateur");
        }
        return new AuthorizationDecision(granted);
    }

    private void deny(HttpServletRequest request, Authentication auth, String requiredPermission, String cause) {
        if (requiredPermission != null) {
            request.setAttribute(REQUIRED_PERMISSION_ATTRIBUTE, requiredPermission);
        }
        // Les autorites sont journalisees cote serveur uniquement : la reponse ne renvoie que la
        // permission exigee, pas la liste des droits du compte.
        log.warn("Acces refuse · {} {} · permission exigee={} · cause={} · utilisateur={} · autorites={}",
                request.getMethod(), request.getRequestURI(), requiredPermission, cause,
                auth.getName(), auth.getAuthorities());
    }

    /**
     * Droits fins des caisses (plan finance, lot C0).
     *
     * <p>Avant, tout ce qui n'etait ni une lecture ni un chemin contenant {@code /open} ou
     * {@code /close} tombait sur {@code CASH:ADJUST} : l'ouverture de session ({@code POST /sessions},
     * sans {@code /open}), la creation de caisse, les mouvements et la validation d'un ecart
     * exigeaient le meme droit, que tout caissier avait. Chaque action a desormais le sien.</p>
     */
    /**
     * Demandes de fonds (lots F5 et F6). Les controles fins (niveau de validation, demandeur,
     * beneficiaire) sont faits par le service ; ici, le droit d'entree.
     */
    private String fundPermission(String path, String method) {
        if (path.equals("/fund-requests") && HttpMethod.GET.matches(method)) return permission("FUND_REQUEST", "READ");
        if (path.equals("/fund-requests/advances")) return permission("FUND_REQUEST", "READ");
        if (path.equals("/fund-requests/justifications-to-validate")) return permission("FUND_JUSTIFICATION", "VALIDATE");
        if (path.equals("/fund-requests/to-disburse")) return permission("FUND_REQUEST", "DISBURSE");
        if (path.equals("/fund-requests/import-legacy")) return permission("FUND_REQUEST", "READ");
        if (path.endsWith("/disburse") || path.endsWith("/return") || path.endsWith("/complement") || path.endsWith("/transfer")) {
            return permission("FUND_REQUEST", "DISBURSE");
        }
        if (path.matches("/fund-requests/[^/]+/expenses/[^/]+/(validate|reject)")) return permission("FUND_JUSTIFICATION", "VALIDATE");
        if (path.matches("/fund-needs/[^/]+/(validate|reject)")) return permission("FUND_REQUEST", "APPROVE_L1");
        return permission("FUND_REQUEST", "CREATE");
    }

    /** Droits de la comptabilite (plan finance, section 16). */
    private String accountingPermission(String path, String method) {
        if (path.startsWith("/accounting/bank") || path.startsWith("/accounting/reconciliations") || path.startsWith("/accounting/cheques")
                || path.startsWith("/accounting/deposit-slips") || path.startsWith("/accounting/exchange-rates")
                || path.matches("/accounting/cash/registers/[^/]+/balances")) {
            // Lots F10 et C5.
            if (path.matches("/accounting/cash/registers/[^/]+/balances")) return permission("CASH", "READ");
            if (path.startsWith("/accounting/exchange-rates")) {
                return HttpMethod.GET.matches(method) ? permission("CASH", "READ") : permission("CASH_EXCHANGE_RATE", "MANAGE");
            }
            if (HttpMethod.GET.matches(method)) return permission("ACCOUNTING_REPORT", "READ");
            if (path.startsWith("/accounting/bank-accounts") && path.endsWith("/statements")) return permission("BANK_STATEMENT", "IMPORT");
            if (path.endsWith("/statements/manual")) return permission("BANK_STATEMENT", "IMPORT");
            if (path.startsWith("/accounting/reconciliations") && path.endsWith("/validate")) return permission("BANK_RECONCILIATION", "VALIDATE");
            if (path.equals("/accounting/bank-accounts") || path.startsWith("/accounting/bank-import-templates") || path.startsWith("/accounting/bank-assignment-rules")) {
                return permission("BANK_ACCOUNT", "MANAGE");
            }
            return permission("BANK_RECONCILIATION", "MANAGE");
        }
        if (path.startsWith("/accounting/budget")) {
            // Lot F16 : les droits fins (preparer, consolider, approuver, virer, deroger) sont dans le service.
            if (HttpMethod.GET.matches(method) || path.equals("/accounting/budget/check")) return permission("BUDGET_REPORT", "READ");
            if (path.startsWith("/accounting/budget/items")) return permission("BUDGET", "PREPARE");
            if (path.startsWith("/accounting/budget/forecasts")) return permission("BUDGET_FORECAST", "MANAGE");
            if (path.startsWith("/accounting/budget/transfers")) return permission("BUDGET", "TRANSFER_REQUEST");
            return permission("BUDGET", "PREPARE");
        }
        if (path.startsWith("/accounting/analytic")) {
            // Lot F9 : les controles fins (branche du responsable, reclassement) sont dans le service.
            if (HttpMethod.GET.matches(method)) return permission("ANALYTIC_REPORT", "READ");
            if (path.equals("/accounting/analytic/reclass")) return permission("ANALYTIC", "RECLASS");
            if (path.matches("/accounting/analytic/axes/[^/]+/sections") && HttpMethod.POST.matches(method)) {
                return permission("ANALYTIC_ITEM", "CREATE");
            }
            return permission("ANALYTIC_SETTINGS", "MANAGE");
        }
        if (path.startsWith("/accounting/sales")) {
            // Lot F4 : ventes et encaissements.
            if (HttpMethod.GET.matches(method)) return permission("ACCOUNTING_REPORT", "READ");
            if (path.equals("/accounting/sales/auto-letter")) return permission("ACCOUNTING_LETTERING", "MANUAL");
            return permission("ACCOUNTING_POSTING", "REPLAY");
        }
        if (path.startsWith("/accounting/cash")) {
            // Lot F3 : caisse comptable.
            if (path.matches("/accounting/cash/registers/[^/]+/journals") && HttpMethod.GET.matches(method)) {
                return permission("CASH_MOVEMENT", "CREATE");
            }
            if (path.matches("/accounting/cash/registers/[^/]+/journals") && HttpMethod.POST.matches(method)) {
                return permission("ACCOUNTING_JOURNAL", "CREATE");
            }
            if (path.contains("/journals/")) return permission("ACCOUNTING_JOURNAL", "MANAGE");
            if (path.equals("/accounting/cash/feed")) return permission("ACCOUNTING_POSTING", "REPLAY");
            return permission("ACCOUNTING_REPORT", "READ");
        }
        if (path.startsWith("/accounting/posting")) {
            // Lot F2 : moteur d'imputation.
            if (HttpMethod.GET.matches(method) || path.equals("/accounting/posting/preview")) {
                return permission("ACCOUNTING_POSTING", "READ");
            }
            if (path.startsWith("/accounting/posting/rules")) return permission("ACCOUNTING_POSTING", "MANAGE");
            return permission("ACCOUNTING_POSTING", "REPLAY");
        }
        if (HttpMethod.GET.matches(method)) return permission("ACCOUNTING_REPORT", "READ");
        if (path.equals("/accounting/numbering-checks")) return permission("ACCOUNTING_REPORT", "READ");
        if (path.startsWith("/accounting/accounts")) {
            if (path.endsWith("/deactivate") || path.endsWith("/activate") || path.endsWith("/merge")) {
                return permission("ACCOUNTING_ACCOUNT", "MANAGE");
            }
            return permission("ACCOUNTING_ACCOUNT", "CREATE");
        }
        if (path.startsWith("/accounting/fiscal-years")) return permission("ACCOUNTING_PERIOD", "CLOSE");
        if (path.startsWith("/accounting/periods")) {
            return path.endsWith("/close") ? permission("ACCOUNTING_PERIOD", "CLOSE") : permission("ACCOUNTING_PERIOD", "LOCK");
        }
        if (path.startsWith("/accounting/journals")) {
            return HttpMethod.POST.matches(method) ? permission("ACCOUNTING_JOURNAL", "CREATE") : permission("ACCOUNTING_JOURNAL", "MANAGE");
        }
        if (path.startsWith("/accounting/entries")) {
            if (path.endsWith("/validate")) return permission("ACCOUNTING_ENTRY", "VALIDATE");
            if (path.endsWith("/reverse")) return permission("ACCOUNTING_ENTRY", "REVERSE");
            if (path.endsWith("/reject") || path.endsWith("/regularize")) return permission("ACCOUNTING_ENTRY", "BALANCE");
            return permission("ACCOUNTING_ENTRY", "CREATE");
        }
        if (path.startsWith("/accounting/lettering")) {
            return path.endsWith("/unletter") ? permission("ACCOUNTING_LETTERING", "UNLETTER") : permission("ACCOUNTING_LETTERING", "MANUAL");
        }
        return permission("ACCOUNTING_SETTINGS", "MANAGE");
    }

    private String cashPermission(String path, String method) {
        // Position et etats (lot C6) : lecture reservee a l'encadrement.
        if (path.equals("/cash-registers/position") || path.startsWith("/cash-registers/reports/")) {
            return permission("CASH_REPORT", "READ");
        }
        if (HttpMethod.GET.matches(method)) return permission("CASH", "READ");
        if (path.equals("/cash-registers") || path.startsWith("/cash-registers/reference-lists")) {
            return permission("CASH_REGISTER", "MANAGE");
        }
        // Le titulaire sortant et l'entrant signent eux-memes le PV de remise : un droit de caissier suffit.
        if (path.startsWith("/cash-registers/responsibility-transfers/") && path.endsWith("/sign")) {
            return permission("CASH_SESSION", "OPEN");
        }
        if (path.equals("/cash-registers/sessions")) return permission("CASH_SESSION", "OPEN");
        if (path.startsWith("/cash-registers/sessions/")) {
            if (path.endsWith("/close") || path.endsWith("/closing-request") || path.endsWith("/start-closing")
                    || path.endsWith("/counts") || path.endsWith("/handover") || path.endsWith("/confirm-opening")
                    || path.endsWith("/cancel") || path.endsWith("/contest")) {
                return permission("CASH_SESSION", "CLOSE");
            }
            if (path.endsWith("/pause") || path.endsWith("/resume")) return permission("CASH_SESSION", "PAUSE");
            if (path.endsWith("/suspend") || path.endsWith("/unsuspend")) return permission("CASH_SESSION", "SUSPEND");
            if (path.endsWith("/force-close")) return permission("CASH_SESSION", "FORCE_CLOSE");
            if (path.endsWith("/spot-checks")) return permission("CASH_SESSION", "SPOT_CHECK");
            if (path.endsWith("/approve-variance") || path.endsWith("/variance-decision") || path.endsWith("/recount")) {
                return permission("CASH_VARIANCE", "DECIDE");
            }
            return permission("CASH_MOVEMENT", "CREATE");
        }
        // L'entrant accepte ou conteste la passation, le sortant l'annule : un droit de caissier suffit.
        if (path.startsWith("/cash-registers/handovers/")) return permission("CASH_SESSION", "OPEN");
        if (path.startsWith("/cash-registers/variance-reviews")) return permission("CASH_VARIANCE", "DECIDE");
        if (path.startsWith("/cash-registers/denominations")) return permission("CASH_REGISTER", "MANAGE");
        if (path.startsWith("/cash-registers/absences")) {
            if (path.endsWith("/approve") || path.endsWith("/end")) return permission("CASH_STAFF", "MANAGE");
            return permission("CASH_STAFF", "DECLARE_ABSENCE");
        }
        // Un reviseur delegue ses propres revues : le droit de decider suffit.
        if (path.startsWith("/cash-registers/delegations")) return permission("CASH_VARIANCE", "DECIDE");
        if (path.startsWith("/cash-registers/anomalies")) return permission("CASH_VARIANCE", "DECIDE");
        if (path.startsWith("/cash-registers/requests/")
                && (path.endsWith("/approve") || path.endsWith("/reject"))) {
            return permission("CASH_MOVEMENT", "APPROVE");
        }
        if (path.startsWith("/cash-registers/movements/") && path.endsWith("/approve")) return permission("CASH_MOVEMENT", "APPROVE");
        // Le caissier contre-passe seul dans un court delai et sous un plafond ; au-dela, le service
        // exige CASH_MOVEMENT:REVERSE. La route ne demande donc que le droit de saisir.
        if (path.endsWith("/reverse")) return permission("CASH_MOVEMENT", "CREATE");
        if (path.startsWith("/cash-registers/natures")) return permission("CASH_REGISTER", "MANAGE");
        if (path.equals("/cash-registers/transfers")) return permission("CASH_TRANSFER", "SEND");
        if (path.startsWith("/cash-registers/transfers/")) {
            return path.endsWith("/receive") ? permission("CASH_TRANSFER", "RECEIVE") : permission("CASH_TRANSFER", "SEND");
        }
        if (path.contains("/petty-cash/")) return permission("CASH_TRANSFER", "SEND");
        if (path.startsWith("/cash-registers/movements/") || path.startsWith("/cash-registers/requests/")) {
            return permission("CASH_MOVEMENT", "CREATE");
        }
        // Fiche d'une caisse : /cash-registers/{code} et ses actions de cycle de vie et de personnel.
        String[] segments = path.substring("/cash-registers/".length()).split("/");
        if (segments.length == 2 && (segments[1].equals("assignments") || segments[1].equals("emergency-assignments")
                || segments[1].equals("responsibility-transfers") || segments[1].equals("seal") || segments[1].equals("unseal"))) {
            return permission("CASH_STAFF", "MANAGE");
        }
        if ((segments.length == 1 && HttpMethod.PATCH.matches(method))
                || (segments.length == 2 && (segments[1].equals("activate") || segments[1].equals("deactivate")
                || segments[1].equals("suspend") || segments[1].equals("reactivate") || segments[1].equals("close")))
                || (segments.length == 3 && segments[1].equals("devices"))) {
            return permission("CASH_REGISTER", "MANAGE");
        }
        return permission("CASH_MOVEMENT", "CREATE");
    }

    private String resolvePermission(HttpServletRequest request) {
        String path = normalizePath(request.getRequestURI());
        String method = request.getMethod();

        if (path.startsWith("/admin/users")) {
            return permission("SYSTEM", "USERS");
        }
        if (path.startsWith("/admin/roles")) {
            return permission("SYSTEM", "ROLES");
        }
        if (path.startsWith("/admin/permissions")) {
            return permission("SYSTEM", "PERMISSIONS");
        }
        if (path.startsWith("/admin/audit") || path.startsWith("/admin/settings-audit")) {
            return permission("SYSTEM", "AUDIT");
        }
        // AdminNotificationController est monte sous /admin/notifications : sans cette regle il
        // tombe dans le repli /admin/ et la cloche de notifications exige SYSTEM:SETTINGS, un
        // droit de configuration systeme. On aligne sur /notifications, qui demande ADMIN:ACCESS.
        if (path.startsWith("/admin/notifications")) {
            return permission("ADMIN", "ACCESS");
        }
        // Maintenance : les lectures (previsualisation d'une purge, liste des candidats) ne
        // modifient rien et doivent rester consultables sans droit de configuration systeme,
        // sans quoi on ne peut pas verifier ce qu'une suppression emporterait avant de la lancer.
        // Les verbes destructeurs restent, eux, sur SYSTEM:SETTINGS via le repli ci-dessous.
        if (path.startsWith("/admin/maintenance") && HttpMethod.GET.matches(method)) {
            return permission("ADMIN", "ACCESS");
        }
        if (path.startsWith("/admin/")) {
            return permission("SYSTEM", "SETTINGS");
        }

        if (path.startsWith("/billing")) {
            if (path.contains("/send")) return permission("BILLING", "SEND");
            if (path.contains("/pay")) return permission("PAYMENT", "PROCESS");
            if (path.contains("/cancel") || path.contains("/credit-note")) return permission("BILLING", "CANCEL");
            // Le module facturation n'a pas de suppression dure : l'immuabilite fiscale impose
            // qu'un DELETE sur un document soit une annulation (directe si le document n'est pas
            // scelle, via avoir automatique sinon). C'est donc BILLING:CANCEL qui s'applique, et
            // surtout pas BILLING:UPDATE - annuler est plus lourd que corriger.
            if (HttpMethod.DELETE.matches(method)) return permission("BILLING", "CANCEL");
            return permission("BILLING", actionFor("BILLING", method));
        }
        if (path.startsWith("/payments")) {
            if (path.contains("refund")) return permission("PAYMENT", "REFUND");
            return permission("PAYMENT", HttpMethod.GET.matches(method) ? "READ" : "PROCESS");
        }
        if (path.startsWith("/accounting")) {
            return accountingPermission(path, method);
        }
        if (path.startsWith("/fund-needs") || path.startsWith("/fund-requests")) {
            return fundPermission(path, method);
        }
        if (path.startsWith("/cash-registers")) {
            return cashPermission(path, method);
        }
        if (path.startsWith("/bookings")) {
            if (path.contains("/check-in") || path.contains("/check-out")) return permission("BOOKING", "CHECKIN");
            if (path.contains("/cancel") || path.contains("/reject") || path.contains("/no-show")) return permission("BOOKING", "CANCEL");
            return permission("BOOKING", actionFor("BOOKING", method));
        }
        if (path.startsWith("/customers") || path.startsWith("/members")) {
            return permission("CLIENT", actionFor("CLIENT", method));
        }
        if (path.startsWith("/kyc")) {
            // Les decisions en masse (bulk-approve, bulk-reject et ses alias) ne contiennent ni
            // /approve ni /reject : elles tombaient sur KYC:READ, une lecture suffisait a valider.
            if (path.contains("/approve") || path.contains("/bulk-approve")) return permission("KYC", "APPROVE");
            if (path.contains("/reject") || path.contains("/bulk-reject")
                    || path.contains("/bulk-disapprove") || path.contains("/bulk-desapprove")) {
                return permission("KYC", "REJECT");
            }
            return permission("KYC", "READ");
        }
        if (path.startsWith("/documents")) {
            if (path.contains("/approve") || path.contains("/reject") ||
                path.contains("/request-correction") || path.contains("/restore") ||
                path.contains("/archive") || path.contains("/analytics") ||
                path.contains("/export") || path.contains("/access-logs")) {
                return permission("DOCUMENT", "REVIEW");
            }
            if (HttpMethod.POST.matches(method)) return permission("DOCUMENT", "UPLOAD");
            return permission("DOCUMENT", "READ");
        }
        if (path.startsWith("/subscriptions") || path.startsWith("/subscription")
                || path.startsWith("/passes") || path.startsWith("/usage-records")) {
            if (path.contains("/activate") || path.contains("/renew") || path.contains("/resume")) return permission("SUBSCRIPTION", "ACTIVATE");
            if (path.contains("/cancel") || path.contains("/suspend") || path.contains("/pause")) return permission("SUBSCRIPTION", "CANCEL");
            return permission("SUBSCRIPTION", actionFor("SUBSCRIPTION", method));
        }
        if (path.startsWith("/inventory")) {
            if (path.contains("/approve")) return permission("INVENTORY", "APPROVE");
            return permission("INVENTORY", actionFor("INVENTORY", method));
        }
        if (path.startsWith("/resources") || path.startsWith("/resource-")) {
            if (path.contains("price") || path.contains("pricing")) return permission("RESOURCE", "PRICE");
            if (path.contains("/gallery") || path.contains("/photos")) return permission("RESOURCE", "GALLERY");
            return permission("RESOURCE", actionFor("RESOURCE", method));
        }
        if (path.startsWith("/analytics") || path.startsWith("/reports") || path.startsWith("/reporting")) {
            // Les rapports se telechargent par un suffixe /csv ou /pdf : la regle n'attendait que
            // .csv et /export, REPORT:VIEW suffisait donc a exporter.
            return path.endsWith(".csv") || path.endsWith("/csv")
                    || path.endsWith(".pdf") || path.endsWith("/pdf")
                    || path.contains("/export")
                    ? permission("REPORT", "EXPORT")
                    : permission("REPORT", "VIEW");
        }
        if (path.startsWith("/visitors")) {
            if (path.contains("/check-in") || path.contains("/check-out")) return permission("VISITOR", "CHECKIN");
            return permission("VISITOR", HttpMethod.GET.matches(method) ? "READ" : "WRITE");
        }
        if (path.startsWith("/support")) {
            if (path.contains("/assign")) return permission("SUPPORT", "ASSIGN");
            if (path.contains("/metrics") || path.contains("/analytics") || path.contains("/performance")) {
                return permission("SUPPORT", "METRICS");
            }
            return permission("SUPPORT", HttpMethod.GET.matches(method) ? "READ" : "WRITE");
        }
        if (path.startsWith("/tasks")) {
            if (path.contains("/assign")) return permission("TASK", "ASSIGN");
            return permission("TASK", HttpMethod.GET.matches(method) ? "READ" : "WRITE");
        }
        if (path.startsWith("/crm")) {
            if (path.contains("/convert")) return permission("CRM", "CONVERT");
            return permission("CRM", HttpMethod.GET.matches(method) ? "READ" : "WRITE");
        }
        if (path.startsWith("/event-registrations")) {
            if (path.contains("/validate") || path.contains("/reject")) return permission("CRM", "CONVERT");
            return permission("CRM", HttpMethod.GET.matches(method) ? "READ" : "WRITE");
        }
        if (path.startsWith("/countries") || path.startsWith("/currency") || path.startsWith("/currencies")) {
            return permission("SYSTEM", "SETTINGS");
        }
        if (path.startsWith("/wallets") || path.startsWith("/wallet-holds")
                || path.startsWith("/payment-links") || path.startsWith("/billable-items")) {
            if (path.contains("refund")) return permission("PAYMENT", "REFUND");
            return permission("PAYMENT", HttpMethod.GET.matches(method) ? "READ" : "PROCESS");
        }
        // Coupons et parrainages sont des promotions : memes droits
        if (path.startsWith("/promotions") || path.startsWith("/coupons") || path.startsWith("/referrals")) {
            return permission("BILLING", actionFor("BILLING", method));
        }
        if (path.startsWith("/contracts")) {
            return permission("BILLING", actionFor("BILLING", method));
        }
        if (path.startsWith("/notifications")) {
            return permission("ADMIN", "ACCESS");
        }
        if (path.startsWith("/businesses") || path.startsWith("/document-types")
                || path.startsWith("/document-requirements") || path.startsWith("/document-retention-policies")) {
            return permission("SYSTEM", "SETTINGS");
        }
        // Explorateur de la GED : lire, deposer, puis tout le reste (ranger, renommer, corbeille).
        if (path.startsWith("/ged/") || path.equals("/ged")) {
            if (HttpMethod.GET.matches(method)) return permission("DOCUMENT", "READ");
            if ((path.equals("/ged/files") || path.equals("/ged/scans")) && HttpMethod.POST.matches(method)) return permission("DOCUMENT", "UPLOAD");
            return permission("DOCUMENT", "WRITE");
        }
        if (path.startsWith("/review")) {
            return permission("DOCUMENT", "REVIEW");
        }

        return null;
    }

    private String normalizePath(String requestUri) {
        String uri = requestUri == null ? "" : requestUri;
        if (uri.startsWith(ApiPath.V1)) {
            uri = uri.substring(ApiPath.V1.length());
        }
        if (!uri.startsWith("/")) {
            uri = "/" + uri;
        }
        return uri.toLowerCase(Locale.ROOT);
    }

    /**
     * Resout l'action attendue pour une methode HTTP, en tenant compte des modules qui ne
     * definissent pas de permission de suppression.
     * <p>
     * Renvoyer systematiquement {@code DELETE} produisait une permission inexistante pour
     * BILLING, BOOKING et SUBSCRIPTION : aucun role ne pouvant la porter, la decision etait
     * refusee pour <b>tout le monde sauf SUPER_ADMIN</b> (seul a beneficier du court-circuit de
     * role plus haut), et l'appel remontait un 403 avant meme d'atteindre le controleur. Un
     * administrateur voyait donc une erreur de permission sur une action autorisee, et le defaut
     * paraissait lie a son role alors qu'il venait du referentiel.
     * <p>
     * Pour ces modules, supprimer est une variante de modification et retombe sur {@code UPDATE}
     * (la facturation est traitee a part, voir {@link #resolvePermission}).
     */
    private String actionFor(String module, String method) {
        if (HttpMethod.GET.matches(method)) return "READ";
        if (HttpMethod.POST.matches(method)) return "CREATE";
        if (HttpMethod.DELETE.matches(method)) {
            return MODULES_WITH_DELETE_PERMISSION.contains(module) ? "DELETE" : "UPDATE";
        }
        return "UPDATE";
    }

    private String permission(String module, String action) {
        return module + ":" + action;
    }

    private boolean has(Collection<? extends GrantedAuthority> authorities, String authority) {
        return authorities.stream().anyMatch(item -> authority.equals(item.getAuthority()));
    }
}
