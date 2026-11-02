package com.sni.bokaticowork.features.ressource.service.support;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.ressource.model.ResourcePolicy;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * La reservation faite par l'equipe passe outre le preavis de la ressource.
 *
 * <p>Un membre de l'administration, de la gerance ou des operations saisit une reservation pour un
 * client qui n'a pas pu la faire en ligne : dans l'heure qui court, une minute avant le debut, ou
 * sur des creneaux deja passes et restes libres, pour la facturer. Seules les politiques qui
 * l'interdisent ({@code staff_override_allowed = false} · la stricte et l'evenementielle) gardent
 * leur preavis pour tout le monde.</p>
 *
 * <p>La duree minimale et maximale de la politique s'appliquent toujours. Le portail client ne
 * beneficie jamais de la derogation, meme quand le compte connecte a aussi un role d'equipe.</p>
 */
@Component
public class StaffBookingOverride {

    /** Les roles qui peuvent reserver hors preavis. */
    public static final Set<String> ROLES = Set.of("ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_MANAGER", "ROLE_OPERATIONS_AGENT");

    private final int maxBackdateDays;

    public StaffBookingOverride(@Value("${bokati.booking.staff.max-backdate-days:31}") int maxBackdateDays) {
        this.maxBackdateDays = maxBackdateDays;
    }

    /** Le membre de l'equipe qui reserve, s'il peut deroger · vide sinon (client, portail, systeme). */
    public java.util.Optional<String> currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || fromPortal()) {
            return java.util.Optional.empty();
        }
        boolean staff = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(ROLES::contains);
        return staff ? java.util.Optional.of(authentication.getName()) : java.util.Optional.empty();
    }

    /** La politique admet-elle la derogation · une ressource sans politique l'admet. */
    public static boolean allowedBy(ResourcePolicy policy) {
        return policy == null || !Boolean.FALSE.equals(policy.getStaffOverrideAllowed());
    }

    /** Le membre de l'equipe qui reserve, si la politique de la ressource le permet. */
    public java.util.Optional<String> actorFor(ResourcePolicy policy) {
        return allowedBy(policy) ? currentActor() : java.util.Optional.empty();
    }

    /** Une reservation saisie apres coup ne remonte pas au-dela de ce nombre de jours. */
    public LocalDateTime earliestStart(LocalDateTime now) {
        return now.toLocalDate().minusDays(maxBackdateDays).atStartOfDay();
    }

    public int maxBackdateDays() {
        return maxBackdateDays;
    }

    private static boolean fromPortal() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return false;
        }
        HttpServletRequest request = attributes.getRequest();
        String path = request.getRequestURI();
        String context = request.getContextPath() == null ? "" : request.getContextPath();
        String relative = path != null && path.startsWith(context) ? path.substring(context.length()) : path;
        return relative != null && (relative.startsWith(ApiPath.V1 + "/client/") || relative.startsWith(ApiPath.V1 + "/portal/"));
    }
}
