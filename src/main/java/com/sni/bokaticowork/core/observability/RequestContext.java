package com.sni.bokaticowork.core.observability;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Contexte d'une requete (centre d'audit, lot A0) : son identifiant et l'adresse reelle du client.
 *
 * <p>L'identifiant vient de l'en-tete {@code X-Request-Id} s'il est sain, sinon il est cree ; il est
 * renvoye dans la reponse, porte dans les journaux (MDC {@code requestId}), dans chaque ligne d'audit,
 * dans les evenements de la boite de sortie et dans les taches asynchrones. L'adresse du client est
 * celle que resout {@code ClientIpResolver} (derriere le proxy Heroku), pas celle du proxy.</p>
 */
public final class RequestContext {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_CLIENT_IP = "clientIp";
    public static final String MDC_USER = "user";
    static final String ATTRIBUTE_REQUEST_ID = RequestContext.class.getName() + ".requestId";
    static final String ATTRIBUTE_CLIENT_IP = RequestContext.class.getName() + ".clientIp";
    private static final Pattern SAFE_ID = Pattern.compile("^[A-Za-z0-9._-]{8,64}$");

    private RequestContext() {
    }

    /** Identifiant recu s'il est sain (8 a 64 caracteres sans espace ni symbole), sinon un nouveau. */
    static String requestIdFrom(String header) {
        return header != null && SAFE_ID.matcher(header.trim()).matches() ? header.trim() : UUID.randomUUID().toString();
    }

    /** Identifiant de la requete ou de la tache en cours (lu dans le MDC), ou {@code null}. */
    public static String currentRequestId() {
        return MDC.get(MDC_REQUEST_ID);
    }

    /** Adresse reelle du client de cette requete ; a defaut, l'adresse du pair direct. */
    public static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object resolved = request.getAttribute(ATTRIBUTE_CLIENT_IP);
        if (resolved instanceof String ip && StringUtils.hasText(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    /** Adresse reelle du client de la requete en cours, s'il y en a une. */
    public static String currentClientIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return clientIp(attributes.getRequest());
        }
        return MDC.get(MDC_CLIENT_IP);
    }
}
