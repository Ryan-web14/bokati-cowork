package com.sni.bokaticowork.security.ratelimit;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Quel plafond s'applique a quelle route.
 *
 * <p>La regle precedente ne reconnaissait que les chemins commencant par {@code ApiPath.V1} ·
 * tout le reste n'avait <b>aucun</b> plafond. Etaient donc illimites :</p>
 * <ul>
 *   <li>{@code /verify/**}, qui confirme l'existence d'un document a partir de son numero · les
 *       numeros etant sequentiels, le volume de facturation s'enumerait sans contrainte ;</li>
 *   <li>{@code POST /verify/doc/{n}/compare}, qui accepte un <b>fichier sans authentification</b>
 *       et compare des PDF · de quoi epuiser la machine avec quelques requetes ;</li>
 *   <li>{@code /images/**} et {@code /public/quotes/sign}.</li>
 * </ul>
 *
 * <p>Et au-dessus de {@code ApiPath.V1}, deux routes d'authentification et deux ecritures
 * anonymes n'avaient que le plafond generique de six cents requetes par minute · largement de quoi
 * deverrouiller un compte en boucle, inonder le fichier client ou declencher des envois de
 * courriels en masse.</p>
 */
@Component
public class RateLimitRules {

    @Value("${app.security.rate-limit.login.max-requests:5}")
    private int loginMax;
    @Value("${app.security.rate-limit.login.window-seconds:60}")
    private long loginWindow;

    @Value("${app.security.rate-limit.otp.max-requests:3}")
    private int otpMax;
    @Value("${app.security.rate-limit.otp.window-seconds:600}")
    private long otpWindow;

    @Value("${app.security.rate-limit.callback.max-requests:120}")
    private int callbackMax;
    @Value("${app.security.rate-limit.callback.window-seconds:60}")
    private long callbackWindow;

    /** Les ecritures que n'importe qui peut declencher · un prospect, une inscription. */
    @Value("${app.security.rate-limit.public-write.max-requests:10}")
    private int publicWriteMax;
    @Value("${app.security.rate-limit.public-write.window-seconds:600}")
    private long publicWriteWindow;

    /** La lecture des pages de verification · lutte contre l'enumeration des numeros. */
    @Value("${app.security.rate-limit.verify.max-requests:60}")
    private int verifyMax;
    @Value("${app.security.rate-limit.verify.window-seconds:600}")
    private long verifyWindow;

    /** Le depot de fichier non authentifie · chaque appel fait analyser un PDF. */
    @Value("${app.security.rate-limit.upload.max-requests:5}")
    private int uploadMax;
    @Value("${app.security.rate-limit.upload.window-seconds:600}")
    private long uploadWindow;

    @Value("${app.security.rate-limit.admin.max-requests:600}")
    private int adminMax;
    @Value("${app.security.rate-limit.admin.window-seconds:60}")
    private long adminWindow;

    /** Le filet de securite · tout ce qui n'est pas nomme ailleurs. */
    @Value("${app.security.rate-limit.default.max-requests:300}")
    private int defaultMax;
    @Value("${app.security.rate-limit.default.window-seconds:60}")
    private long defaultWindow;

    /**
     * Le plafond applicable · jamais {@code null}.
     *
     * <p>Rendre {@code null} signifiait « aucun plafond », et c'est ce que recevait tout chemin
     * hors de {@code ApiPath.V1}. Une route oubliee doit retomber sur un plafond, pas sur
     * l'absence de plafond.</p>
     */
    public Rule ruleFor(String method, String uri, String clientIp) {
        String path = uri == null ? "" : uri.toLowerCase(Locale.ROOT);
        String verb = method == null ? "GET" : method.toUpperCase(Locale.ROOT);

        if (path.equals(ApiPath.V1 + "/auth/login")) {
            return new Rule("login:" + clientIp, loginMax, loginWindow);
        }
        if (path.startsWith(ApiPath.V1 + "/auth/ott/")
                || path.startsWith(ApiPath.V1 + "/auth/password-reset/")
                // Le deverrouillage de compte et le renvoi de verification valent un code a usage
                // unique · sans plafond propre, le verrouillage apres cinq echecs se contournait,
                // et le renvoi servait a inonder la boite d'un tiers.
                || path.startsWith(ApiPath.V1 + "/auth/unlock-account")
                || path.equals(ApiPath.V1 + "/auth/email/verify/resend")) {
            return new Rule("otp:" + clientIp + ":" + path, otpMax, otpWindow);
        }
        if (path.startsWith(ApiPath.V1 + "/payments/mobile-money/")
                && (path.contains("callback") || path.endsWith("/return"))) {
            return new Rule("callback:" + clientIp + ":" + path, callbackMax, callbackWindow);
        }
        // Un depot de fichier non authentifie · le plus couteux de toutes les routes publiques.
        if (path.startsWith("/verify/") && path.endsWith("/compare")) {
            return new Rule("upload:" + clientIp, uploadMax, uploadWindow);
        }
        if (path.startsWith("/verify/")) {
            return new Rule("verify:" + clientIp, verifyMax, verifyWindow);
        }
        if (isPublicWrite(verb, path)) {
            return new Rule("public-write:" + clientIp + ":" + path, publicWriteMax, publicWriteWindow);
        }
        if (path.startsWith(ApiPath.V1)) {
            return new Rule("admin:" + clientIp, adminMax, adminWindow);
        }
        return new Rule("default:" + clientIp, defaultMax, defaultWindow);
    }

    /** Une ecriture qu'un inconnu peut declencher · prospect, inscription, enquete. */
    private boolean isPublicWrite(String verb, String path) {
        if (!"POST".equals(verb) && !"PUT".equals(verb) && !"PATCH".equals(verb)) {
            return false;
        }
        return path.startsWith(ApiPath.V1 + "/public/") || path.startsWith("/public/");
    }

    /** Un plafond · une cle de comptage, un nombre de requetes, une fenetre. */
    public record Rule(String key, int maxRequests, long windowSeconds) {
    }
}
