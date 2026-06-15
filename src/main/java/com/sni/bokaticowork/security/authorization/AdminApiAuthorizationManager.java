package com.sni.bokaticowork.security.authorization;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import jakarta.servlet.http.HttpServletRequest;
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

@Component
public class AdminApiAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private static final String SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final Set<String> ADMIN_REALM_ROLES = Set.of(
            SUPER_ADMIN,
            "ROLE_ADMIN",
            "ROLE_MANAGER",
            "ROLE_FINANCE",
            "ROLE_CASHIER",
            "ROLE_STAFF",
            "ROLE_SUPPORT",
            "ROLE_KYC_REVIEWER",
            "ROLE_AUDITOR",
            "ROLE_VIEWER",
            "ROLE_OPERATIONS_AGENT"
    );

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
        if (ADMIN_REALM_ROLES.stream().noneMatch(role -> has(authorities, role))) {
            return new AuthorizationDecision(false);
        }

        String requiredPermission = resolvePermission(context.getRequest());
        if (!StringUtils.hasText(requiredPermission)) {
            return new AuthorizationDecision(has(authorities, "ADMIN:ACCESS") || has(authorities, "ADMIN_ACCESS"));
        }


        return new AuthorizationDecision(has(authorities, requiredPermission) || has(authorities, requiredPermission.replace(':', '_')));
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
        if (path.startsWith("/admin/")) {
            return permission("SYSTEM", "SETTINGS");
        }

        if (path.startsWith("/billing")) {
            if (path.contains("/send")) return permission("BILLING", "SEND");
            if (path.contains("/pay")) return permission("PAYMENT", "PROCESS");
            if (path.contains("/cancel") || path.contains("/credit-note")) return permission("BILLING", "CANCEL");
            return permission("BILLING", actionFor(method));
        }
        if (path.startsWith("/payments")) {
            if (path.contains("refund")) return permission("PAYMENT", "REFUND");
            return permission("PAYMENT", HttpMethod.GET.matches(method) ? "READ" : "PROCESS");
        }
        if (path.startsWith("/cash-registers")) {
            if (path.contains("/open")) return permission("CASH", "OPEN_SESSION");
            if (path.contains("/close")) return permission("CASH", "CLOSE_SESSION");
            if (HttpMethod.GET.matches(method)) return permission("CASH", "READ");
            return permission("CASH", "ADJUST");
        }
        if (path.startsWith("/bookings")) {
            if (path.contains("/check-in") || path.contains("/check-out")) return permission("BOOKING", "CHECKIN");
            if (path.contains("/cancel") || path.contains("/reject") || path.contains("/no-show")) return permission("BOOKING", "CANCEL");
            return permission("BOOKING", actionFor(method));
        }
        if (path.startsWith("/customers") || path.startsWith("/members")) {
            return permission("CLIENT", actionFor(method));
        }
        if (path.startsWith("/kyc")) {
            if (path.contains("/approve")) return permission("KYC", "APPROVE");
            if (path.contains("/reject")) return permission("KYC", "REJECT");
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
            return permission("SUBSCRIPTION", actionFor(method));
        }
        if (path.startsWith("/inventory")) {
            if (path.contains("/approve")) return permission("INVENTORY", "APPROVE");
            return permission("INVENTORY", actionFor(method));
        }
        if (path.startsWith("/resources") || path.startsWith("/resource-")) {
            if (path.contains("price") || path.contains("pricing")) return permission("RESOURCE", "PRICE");
            if (path.contains("/gallery") || path.contains("/photos")) return permission("RESOURCE", "GALLERY");
            return permission("RESOURCE", actionFor(method));
        }
        if (path.startsWith("/analytics") || path.startsWith("/reports") || path.startsWith("/reporting")) {
            return path.endsWith(".csv") || path.contains("/export")
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
        if (path.startsWith("/countries") || path.startsWith("/currency") || path.startsWith("/currencies")) {
            return permission("SYSTEM", "SETTINGS");
        }
        if (path.startsWith("/wallets") || path.startsWith("/wallet-holds")
                || path.startsWith("/payment-links") || path.startsWith("/billable-items")) {
            if (path.contains("refund")) return permission("PAYMENT", "REFUND");
            return permission("PAYMENT", HttpMethod.GET.matches(method) ? "READ" : "PROCESS");
        }
        if (path.startsWith("/promotions")) {
            return permission("BILLING", actionFor(method));
        }
        if (path.startsWith("/contracts")) {
            return permission("BILLING", actionFor(method));
        }
        if (path.startsWith("/notifications")) {
            return permission("ADMIN", "ACCESS");
        }
        if (path.startsWith("/businesses") || path.startsWith("/document-types")
                || path.startsWith("/document-requirements") || path.startsWith("/document-retention-policies")) {
            return permission("SYSTEM", "SETTINGS");
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

    private String actionFor(String method) {
        if (HttpMethod.GET.matches(method)) return "READ";
        if (HttpMethod.POST.matches(method)) return "CREATE";
        if (HttpMethod.DELETE.matches(method)) return "DELETE";
        return "UPDATE";
    }

    private String permission(String module, String action) {
        return module + ":" + action;
    }

    private boolean has(Collection<? extends GrantedAuthority> authorities, String authority) {
        return authorities.stream().anyMatch(item -> authority.equals(item.getAuthority()));
    }
}
