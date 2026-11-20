package com.sni.bokaticowork.security.superadmin;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.communication.mailService.enums.EmailPriority;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.security.admin.role.service.interfaces.RoleUserService;
import com.sni.bokaticowork.security.admin.support.PrivilegeGuard;
import com.sni.bokaticowork.security.admin.user.dto.request.UserRequest;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.admin.user.repository.UserRepository;
import com.sni.bokaticowork.security.admin.user.service.interfaces.UserService;
import com.sni.bokaticowork.security.service.passwordResetService.support.PasswordPolicyValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Creation d'un super-administrateur par invitation a usage unique.
 *
 * <ol>
 *   <li>Une invitation PENDING est posee en base pour un compte precis (migration).</li>
 *   <li>A la connexion reussie de ce compte, le serveur genere un jeton aleatoire (seule son
 *       empreinte SHA-256 est gardee), valable 30 minutes ; le lien est renvoye dans la reponse
 *       de connexion et envoye par courriel a ce compte. Une nouvelle connexion remplace le lien
 *       tant que l'invitation n'est pas utilisee.</li>
 *   <li>La page ouverte par le lien cree l'utilisateur, lui donne le role SUPER_ADMIN, l'active,
 *       puis clot l'invitation : le lien ne sert plus jamais.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SuperAdminInvitationService {

    static final Duration TTL = Duration.ofMinutes(30);
    static final String FORM_PATH = "/sni/api/v1/auth/super-admin-setup/form?token=";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SuperAdminInvitationRepository invitations;
    private final UserRepository users;
    private final UserService userService;
    private final RoleUserService roleUserService;
    private final PasswordPolicyValidator passwordPolicy;

    @Value("${app.api-base-url:}")
    private String apiBaseUrl;

    private PlatformSettings platformSettings;
    private DefaultEmailSender emailSender;

    @Autowired(required = false)
    void setOptional(PlatformSettings platformSettings, DefaultEmailSender emailSender) {
        this.platformSettings = platformSettings;
        this.emailSender = emailSender;
    }

    /** A la connexion : lien a usage unique si ce compte a une invitation ouverte. Ne fait jamais echouer la connexion. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<String> issueOnLogin(Users user) {
        if (user == null || !StringUtils.hasText(user.getEmail())) {
            return Optional.empty();
        }
        List<SuperAdminInvitation> open = invitations.findOpenFor(user.getEmail().trim());
        if (open.isEmpty()) {
            return Optional.empty();
        }
        SuperAdminInvitation invitation = open.get(0);
        String raw = newToken();
        Instant now = Instant.now();
        invitation.setTokenHash(hash(raw));
        invitation.setIssuedAt(now);
        invitation.setExpiresAt(now.plus(TTL));
        invitation.setStatus(SuperAdminInvitation.ISSUED);
        invitation.setIssueCount(invitation.getIssueCount() == null ? 1 : invitation.getIssueCount() + 1);
        invitations.save(invitation);
        String url = baseUrl() + FORM_PATH + raw;
        log.warn("Invitation super-administrateur : lien emis pour {} (valable {} min)", user.getEmail(), TTL.toMinutes());
        mail(user.getEmail(), url);
        return Optional.of(url);
    }

    @Transactional(readOnly = true)
    public boolean isValid(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            return false;
        }
        return invitations.findByTokenHash(hash(rawToken.trim())).filter(SuperAdminInvitationService::usable).isPresent();
    }

    /** Cree le super-administrateur et clot l'invitation. */
    @Transactional
    public Users complete(String rawToken, String firstname, String lastname, String email, String password, String ip) {
        if (!StringUtils.hasText(rawToken)) {
            throw new ForbiddenException("Lien invalide.");
        }
        SuperAdminInvitation invitation = invitations.lockByTokenHash(hash(rawToken.trim()))
                .filter(SuperAdminInvitationService::usable)
                .orElseThrow(() -> new ForbiddenException("Ce lien est invalide, expiré ou déjà utilisé. Reconnectez-vous avec le compte invité pour en obtenir un nouveau."));
        if (!StringUtils.hasText(email) || !email.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new BadRequestException("Saisissez une adresse électronique valide.");
        }
        if (!StringUtils.hasText(firstname) || !StringUtils.hasText(lastname)) {
            throw new BadRequestException("Saisissez le prénom et le nom.");
        }
        String normalized = email.trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(normalized)) {
            throw new BadRequestException("Un compte existe déjà avec l'adresse " + normalized + ". Choisissez une autre adresse.");
        }
        passwordPolicy.validate(password).ifPresent(message -> {
            throw new BadRequestException(message);
        });
        Users created = userService.createUser(UserRequest.builder().email(normalized).firstname(firstname.trim()).lastname(lastname.trim())
                .password(password).build());
        roleUserService.addRoleToUser(created.getId(), PrivilegeGuard.SUPER_ADMIN, "INVITATION:" + invitation.getTargetEmail());
        userService.activateUser(created.getEmail());
        invitation.setStatus(SuperAdminInvitation.USED);
        invitation.setUsedAt(Instant.now());
        invitation.setUsedIp(ip);
        invitation.setCreatedUserEmail(normalized);
        invitation.setTokenHash(null);
        invitations.save(invitation);
        log.warn("Invitation super-administrateur utilisee : {} cree par l'invitation de {}, depuis {}", normalized, invitation.getTargetEmail(), ip);
        return created;
    }

    static boolean usable(SuperAdminInvitation invitation) {
        return SuperAdminInvitation.ISSUED.equals(invitation.getStatus()) && invitation.getExpiresAt() != null
                && invitation.getExpiresAt().isAfter(Instant.now());
    }

    static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String baseUrl() {
        String base = platformSettings == null ? apiBaseUrl : platformSettings.text("app.api-base-url", apiBaseUrl);
        return base == null ? "" : base.replaceAll("/+$", "");
    }

    private void mail(String to, String url) {
        if (emailSender == null) {
            return;
        }
        String html = "<p>Bonjour,</p><p>Un lien à usage unique a été généré à votre connexion pour créer un compte super-administrateur ELLE A OSE.</p>"
                + "<p><a href=\"" + url + "\">Créer le super-administrateur</a></p>"
                + "<p>Ce lien expire dans 30 minutes et ne sert qu'une fois. Une nouvelle connexion du compte le remplace : utilisez toujours le courriel le plus récent. Si vous n'êtes pas à l'origine de cette connexion, changez votre mot de passe.</p>";
        try {
            // Sujet date a la minute : chaque connexion remplace le lien, et l'anti-doublon des courriels
            // (meme destinataire, meme sujet, une heure) bloquait les suivants, laissant en boite des
            // liens deja remplaces.
            String at = java.time.ZonedDateTime.now(java.time.ZoneId.of("Africa/Brazzaville"))
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            emailSender.sendHtmlEmail(to, "Création du super-administrateur : lien du " + at, html, EmailPriority.CRITICAL);
        } catch (Exception ex) {
            log.warn("Courriel d'invitation super-administrateur non envoye : {}", ex.getMessage());
        }
    }
}
