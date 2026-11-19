package com.sni.bokaticowork.security.superadmin;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/** Page ouverte par le lien d'invitation : creation du super-administrateur (memes protections que la reinitialisation). */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/auth/super-admin-setup")
public class SuperAdminSetupFormController {

    private static final String CSRF_COOKIE = "SUPER_ADMIN_SETUP_CSRF";
    private static final String COOKIE_PATH = ApiPath.V1 + "/auth/super-admin-setup";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SuperAdminInvitationService invitations;

    @org.springframework.beans.factory.annotation.Value("${app.admin-access-url:https://admin.elleaose.com}")
    private String adminAccessUrl;

    private com.sni.bokaticowork.core.settings.platform.PlatformSettings platformSettings;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setPlatformSettings(com.sni.bokaticowork.core.settings.platform.PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    /** Connexion de l'administration (reglage app.admin-access-url). */
    String adminLoginUrl() {
        String url = platformSettings == null ? adminAccessUrl : platformSettings.text("app.admin-access-url", adminAccessUrl);
        return StringUtils.hasText(url) ? url.trim() : "/";
    }

    @GetMapping("/form")
    public String form(@RequestParam(required = false) String token, Model model, HttpServletRequest request, HttpServletResponse response) {
        headers(model, response);
        if (!invitations.isValid(token)) {
            model.addAttribute("reason", "Ce lien est invalide, expiré ou déjà utilisé. Reconnectez-vous avec le compte invité pour en obtenir un nouveau.");
            return "auth/super-admin-setup-error";
        }
        model.addAttribute("token", token);
        model.addAttribute("_csrf", csrf(request, response));
        return "auth/super-admin-setup-form";
    }

    @PostMapping("/form")
    public String submit(@RequestParam String token, @RequestParam(required = false) String firstname, @RequestParam(required = false) String lastname,
                         @RequestParam(required = false) String email, @RequestParam(required = false) String password,
                         @RequestParam(required = false) String confirmPassword, @RequestParam(name = "_csrf", required = false) String csrfField,
                         HttpServletRequest request, HttpServletResponse response, Model model) {
        headers(model, response);
        if (!csrfMatches(request, csrfField)) {
            model.addAttribute("reason", "Votre session a expiré. Rouvrez le lien et réessayez.");
            return "auth/super-admin-setup-error";
        }
        if (!StringUtils.hasText(password) || !password.equals(confirmPassword)) {
            return again(model, request, response, token, firstname, lastname, email, "Les mots de passe ne correspondent pas.");
        }
        try {
            invitations.complete(token, firstname, lastname, email, password, request.getRemoteAddr());
        } catch (com.sni.bokaticowork.core.exception.customs.BadRequestException ex) {
            return again(model, request, response, token, firstname, lastname, email, ex.getMessage());
        } catch (RuntimeException ex) {
            log.warn("Creation du super-administrateur refusee : {}", ex.getMessage());
            model.addAttribute("reason", ex.getMessage());
            return "auth/super-admin-setup-error";
        }
        clear(request, response);
        model.addAttribute("email", email.trim().toLowerCase());
        model.addAttribute("loginUrl", adminLoginUrl());
        return "auth/super-admin-setup-success";
    }

    private String again(Model model, HttpServletRequest request, HttpServletResponse response, String token, String firstname, String lastname,
                         String email, String error) {
        model.addAttribute("token", token);
        model.addAttribute("firstname", firstname);
        model.addAttribute("lastname", lastname);
        model.addAttribute("email", email);
        model.addAttribute("error", error);
        model.addAttribute("_csrf", csrf(request, response));
        return "auth/super-admin-setup-form";
    }

    private String csrf(HttpServletRequest request, HttpServletResponse response) {
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(CSRF_COOKIE, value).httpOnly(true).secure(request.isSecure())
                .sameSite("Strict").path(COOKIE_PATH).maxAge(Duration.ofMinutes(30)).build().toString());
        return value;
    }

    private boolean csrfMatches(HttpServletRequest request, String field) {
        if (!StringUtils.hasText(field) || request.getCookies() == null) {
            return false;
        }
        for (Cookie cookie : request.getCookies()) {
            if (CSRF_COOKIE.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return MessageDigest.isEqual(cookie.getValue().getBytes(StandardCharsets.UTF_8), field.getBytes(StandardCharsets.UTF_8));
            }
        }
        return false;
    }

    private void clear(HttpServletRequest request, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(CSRF_COOKIE, "").httpOnly(true).secure(request.isSecure())
                .sameSite("Strict").path(COOKIE_PATH).maxAge(0).build().toString());
    }

    private void headers(Model model, HttpServletResponse response) {
        byte[] raw = new byte[16];
        RANDOM.nextBytes(raw);
        String nonce = Base64.getEncoder().withoutPadding().encodeToString(raw);
        model.addAttribute("nonce", nonce);
        response.setHeader("Content-Security-Policy", "default-src 'none'; base-uri 'none'; frame-ancestors 'none'; "
                + "form-action 'self'; img-src data:; style-src 'unsafe-inline'; script-src 'nonce-" + nonce + "'");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "no-store");
    }
}
