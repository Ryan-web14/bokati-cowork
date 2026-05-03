package com.sni.bokaticowork.security.admin.user.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.generator.password.GeneratorOfPassword;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.security.admin.role.model.RoleUser;
import com.sni.bokaticowork.security.admin.role.repository.RoleUserRepository;
import com.sni.bokaticowork.security.admin.role.service.interfaces.RoleUserService;
import com.sni.bokaticowork.security.admin.user.dto.request.AdminCreateUserRequest;
import com.sni.bokaticowork.security.admin.user.dto.request.AdminResetUserPasswordRequest;
import com.sni.bokaticowork.security.admin.user.dto.request.AdminUpdateUserRequest;
import com.sni.bokaticowork.security.admin.user.dto.request.UserRequest;
import com.sni.bokaticowork.security.admin.user.dto.response.AdminUserResponse;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.admin.user.repository.UserRepository;
import com.sni.bokaticowork.security.admin.user.service.interfaces.UserAdminService;
import com.sni.bokaticowork.security.admin.user.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserAdminServiceImpl implements UserAdminService {

    private static final String DEFAULT_STAFF_ROLE = "STAFF";
    private static final int DEFAULT_PASSWORD_LENGTH = 8;

    private final UserService userService;
    private final UserRepository userRepository;
    private final RoleUserService roleUserService;
    private final RoleUserRepository roleUserRepository;
    private final DefaultEmailSender emailSender;

    @Value("${app.admin-access-url:https://admin.elleaose.com}")
    private String adminAccessUrl;

    @Override
    public AdminUserResponse createUser(AdminCreateUserRequest request, String assignedBy) {
        boolean generatePassword = request.getGeneratePassword() == null || request.getGeneratePassword();
        UserRequest userRequest = UserRequest.builder()
                .email(request.getEmail())
                .password(generatePassword ? "GENERATED" : request.getPassword())
                .generatePassword(generatePassword)
                .build();

        Users user = userService.createUser(userRequest);
        List<String> requestedRoles = normalizeRoles(request.getRoleNames());
        if (requestedRoles.isEmpty()) {
            requestedRoles = List.of(DEFAULT_STAFF_ROLE);
        }
        for (String roleName : requestedRoles) {
            roleUserService.addRoleToUser(user.getId(), roleName, assignedBy);
        }
        userService.activateUser(user.getEmail());

        String generatedPassword = generatePassword
                ? userService.updateUserPasswordByAdmin(user.getEmail(), null, true)
                : null;

        Users createdUser = userService.getUserByIdForService(user.getId());
        sendAdminUserCreatedEmailAfterCommit(createdUser, requestedRoles);
        return toAdminResponse(createdUser, generatedPassword);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<AdminUserResponse> list(String email, Boolean enabled, Boolean locked, Boolean deleted, Pageable pageable) {
        Specification<Users> spec = (root, query, cb) -> cb.conjunction();
        if (StringUtils.hasText(email)) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("email")), "%" + email.trim().toLowerCase(Locale.ROOT) + "%"));
        }
        if (enabled != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isAccountEnabled"), enabled));
        }
        if (locked != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isAccountLocked"), locked));
        }
        if (deleted != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("deleted"), deleted));
        }
        Page<Users> page = userRepository.findAll(spec, pageable);
        return new PaginatedResponse<>(page.map(user -> toAdminResponse(user, null)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserResponse> searchByName(String query) {
        if (!StringUtils.hasText(query)) {
            return userRepository.findAllByDeletedFalse(Pageable.ofSize(20)).stream()
                    .map(user -> toAdminResponse(user, null))
                    .toList();
        }
        return userRepository.searchAdminUsers(query.trim()).stream()
                .map(user -> toAdminResponse(user, null))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getById(Long id) {
        return toAdminResponse(userService.getUserByIdForService(id), null);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getByEmail(String email) {
        return toAdminResponse(userService.getUserByEmailForService(email), null);
    }

    @Override
    public AdminUserResponse update(Long id, AdminUpdateUserRequest request, String assignedBy) {
        Users user = userService.getUserByIdForService(id);
        if (StringUtils.hasText(request.getEmail())
                && !user.getEmail().equalsIgnoreCase(request.getEmail())
                && userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BadRequestException("Un utilisateur existe deja avec cet email");
        }
        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        }
        if (request.getIsAccountEnabled() != null) {
            user.setIsAccountEnabled(request.getIsAccountEnabled());
        }
        if (request.getIsAccountLocked() != null) {
            user.setIsAccountLocked(request.getIsAccountLocked());
        }
        userRepository.save(user);

        if (request.getRoleNames() != null) {
            syncRoles(user, request.getRoleNames(), assignedBy);
        }
        return toAdminResponse(userService.getUserByIdForService(id), null);
    }

    @Override
    public AdminUserResponse activate(Long id) {
        Users user = userService.getUserByIdForService(id);
        userService.activateUser(user.getEmail());
        return toAdminResponse(userService.getUserByIdForService(id), null);
    }

    @Override
    public AdminUserResponse deactivate(Long id) {
        Users user = userService.getUserByIdForService(id);
        userService.deactivateUser(user.getEmail());
        return toAdminResponse(userService.getUserByIdForService(id), null);
    }

    @Override
    public AdminUserResponse unlock(Long id) {
        Users user = userService.getUserByIdForService(id);
        user.setIsAccountLocked(false);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
        return toAdminResponse(userService.getUserByIdForService(id), null);
    }

    @Override
    public AdminUserResponse resetPassword(Long id, AdminResetUserPasswordRequest request) {
        Users user = userService.getUserByIdForService(id);
        boolean generate = request == null || request.getGeneratePassword() == null || request.getGeneratePassword();
        String generatedPassword = null;
        if (generate) {
            generatedPassword = GeneratorOfPassword.generatePassword(DEFAULT_PASSWORD_LENGTH);
            userService.resetUserPassword(user, generatedPassword);
        } else {
            if (request == null || !StringUtils.hasText(request.getNewPassword())) {
                throw new BadRequestException("Le nouveau mot de passe est requis si la generation automatique est desactivee");
            }
            userService.resetUserPassword(user, request.getNewPassword());
        }
        user.setFailedLoginAttempts(0);
        user.setIsAccountLocked(false);
        userRepository.save(user);
        return toAdminResponse(userService.getUserByIdForService(id), generatedPassword);
    }

    @Override
    public void archive(Long id) {
        Users user = userService.getUserByIdForService(id);
        userService.softDeleteUser(user.getEmail());
    }

    private void syncRoles(Users user, List<String> roleNames, String assignedBy) {
        List<String> normalized = normalizeRoles(roleNames);
        List<RoleUser> currentAssignments = roleUserRepository.findByUserId(user.getId());
        for (RoleUser assignment : currentAssignments) {
            if (!normalized.contains(assignment.getRole().getName())) {
                roleUserService.deleteRoleFromUser(user.getId(), assignment.getRole().getId());
            }
        }
        for (String roleName : normalized) {
            roleUserService.addRoleToUser(user.getId(), roleName, assignedBy);
        }
    }

    private List<String> normalizeRoles(List<String> roles) {
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
                .filter(StringUtils::hasText)
                .map(item -> item.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private AdminUserResponse toAdminResponse(Users user, String generatedPassword) {
        List<String> roleNames = roleUserRepository.findByUserId(user.getId()).stream()
                .map(roleUser -> roleUser.getRole().getName())
                .distinct()
                .sorted()
                .toList();
        return AdminUserResponse.builder()
                .id(user.getId())
                .userId(user.getUserId())
                .email(user.getEmail())
                .accountEnabled(user.getIsAccountEnabled())
                .accountLocked(user.getIsAccountLocked())
                .accountExpired(user.getIsAccountExpired())
                .deleted(user.isDeleted())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .roleNames(roleNames)
                .generatedPassword(generatedPassword)
                .build();
    }

    private void sendAdminUserCreatedEmailAfterCommit(Users user, List<String> roleNames) {
        if (user == null || !StringUtils.hasText(user.getEmail())) {
            return;
        }
        Runnable task = () -> {
            try {
                String html = buildAdminWelcomeEmail(user, roleNames);
                emailSender.sendHtmlEmail(user.getEmail(), "Votre acces administrateur Bokati", html);
            } catch (MessagingException ex) {
                log.warn("Unable to send admin user creation email to {}", user.getEmail(), ex);
            }
        };
        runAfterCommit(task);
    }

    private String buildAdminWelcomeEmail(Users user, List<String> roleNames) {
        String roles = roleNames == null || roleNames.isEmpty()
                ? "Aucun role specifique"
                : String.join(", ", roleNames);
        String accessUrl = StringUtils.hasText(adminAccessUrl) ? adminAccessUrl.trim() : "https://admin.elleaose.com";
        return """
                <!doctype html>
                <html>
                <body style="font-family:Arial,sans-serif;color:#1f2937;line-height:1.5;margin:0;padding:24px;background:#f8fafc;">
                  <div style="max-width:640px;margin:0 auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:8px;padding:24px;">
                    <h2 style="margin:0 0 16px;color:#111827;">Votre acces administrateur est pret</h2>
                    <p>Bonjour,</p>
                    <p>Un compte administrateur Bokati a ete cree pour vous. Vous pouvez acceder a l'espace d'administration avec les informations ci-dessous.</p>
                    <table style="width:100%%;border-collapse:collapse;margin:18px 0;">
                      <tr><td style="padding:8px;border-bottom:1px solid #e5e7eb;color:#6b7280;">Adresse email</td><td style="padding:8px;border-bottom:1px solid #e5e7eb;"><strong>%s</strong></td></tr>
                      <tr><td style="padding:8px;border-bottom:1px solid #e5e7eb;color:#6b7280;">Roles</td><td style="padding:8px;border-bottom:1px solid #e5e7eb;">%s</td></tr>
                      <tr><td style="padding:8px;border-bottom:1px solid #e5e7eb;color:#6b7280;">Adresse d'acces</td><td style="padding:8px;border-bottom:1px solid #e5e7eb;"><a href="%s">%s</a></td></tr>
                    </table>
                    <p>Pour des raisons de securite, le mot de passe n'est pas inclus dans cet email. Utilisez le mot de passe communique par votre administrateur ou la procedure de reinitialisation si necessaire.</p>
                    <p style="margin-top:24px;color:#6b7280;font-size:13px;">Si vous n'etes pas a l'origine de cette demande, veuillez contacter l'administration.</p>
                  </div>
                </body>
                </html>
                """.formatted(
                HtmlUtils.htmlEscape(user.getEmail()),
                HtmlUtils.htmlEscape(roles),
                HtmlUtils.htmlEscape(accessUrl),
                HtmlUtils.htmlEscape(accessUrl)
        );
    }

    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
            return;
        }
        task.run();
    }
}
