package com.sni.bokaticowork.security.admin.support;

import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.security.admin.role.model.Role;
import com.sni.bokaticowork.security.admin.role.repository.RoleUserRepository;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import com.sni.bokaticowork.security.admin.user.model.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Qui peut changer les droits de qui.
 *
 * <p>La seule barriere etait une permission ({@code SYSTEM:ROLES}, {@code SYSTEM:USERS}). Qui la
 * detenait pouvait s'attribuer le role de super-administrateur, ajouter des permissions a son propre
 * role, ou reinitialiser le mot de passe d'un super-administrateur, le nouveau mot de passe lui etant
 * rendu dans la reponse. Les regles d'ici s'ajoutent a la permission, elles ne la remplacent pas :</p>
 * <ul>
 *   <li>seuls un administrateur ou un super-administrateur gerent les roles ;</li>
 *   <li>personne ne modifie ses propres roles, ni les permissions d'un role qu'il porte, sauf un
 *       super-administrateur ;</li>
 *   <li>on n'attribue ni ne retire un role plus puissant que le sien · le role super-administrateur,
 *       ou un role dont une permission manque a l'acteur ;</li>
 *   <li>seul un super-administrateur agit sur le compte d'un super-administrateur ;</li>
 *   <li>le dernier super-administrateur actif ne peut etre ni retire, ni desactive, ni archive.</li>
 * </ul>
 *
 * <p>Appele aux points d'entree de l'administration seulement · l'inscription d'un membre ou
 * l'amorcage du premier administrateur attribuent des roles sans acteur, et ne passent pas ici.</p>
 */
@Component
@RequiredArgsConstructor
public class PrivilegeGuard {

    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ADMIN = "ADMIN";

    private final RoleUserRepository roleUserRepository;

    /** L'acteur connecte · son identifiant et ses autorites, telles que le filtre les a chargees. */
    public record Actor(Long userId, String email, Set<String> authorities) {

        public boolean isSuperAdmin() {
            return authorities.contains("ROLE_" + SUPER_ADMIN);
        }

        public boolean isAdmin() {
            return isSuperAdmin() || authorities.contains("ROLE_" + ADMIN);
        }

        public boolean holdsRole(String roleName) {
            return authorities.contains("ROLE_" + roleName);
        }
    }

    public Actor currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)
                || principal.getUser() == null) {
            throw new ForbiddenException("Action reservee a un compte d'administration connecte.");
        }
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
        return new Actor(principal.getUser().getId(), principal.getUser().getEmail(), authorities);
    }

    /** Gerer les roles et les permissions · administrateur ou super-administrateur seulement. */
    public Actor assertMayManageRoles() {
        Actor actor = currentActor();
        if (!actor.isAdmin()) {
            throw new ForbiddenException("Seuls un administrateur ou un super-administrateur peuvent gerer les roles.");
        }
        return actor;
    }

    /** On ne change pas ses propres roles · meme un super-administrateur passe par un pair. */
    public void assertNotSelf(Actor actor, Users target) {
        if (target != null && Objects.equals(actor.userId(), target.getId())) {
            throw new ForbiddenException("Vous ne pouvez pas modifier vos propres roles.");
        }
    }

    /** Attribuer ou retirer ce role · interdit s'il est plus puissant que l'acteur. */
    public void assertMayGrant(Actor actor, Role role) {
        if (actor.isSuperAdmin() || role == null) {
            return;
        }
        if (SUPER_ADMIN.equalsIgnoreCase(role.getName())) {
            throw new ForbiddenException("Seul un super-administrateur peut attribuer ou retirer le role "
                    + SUPER_ADMIN + ".");
        }
        assertHoldsAll(actor, roleUserRepository.findActivePermissionNamesByRoleId(role.getId()),
                "Le role " + role.getName() + " donne des droits que vous n'avez pas");
    }

    /** Agir sur ce compte · un super-administrateur n'est gere que par un super-administrateur. */
    public void assertMayActOn(Actor actor, Users target) {
        if (actor.isSuperAdmin() || target == null) {
            return;
        }
        if (isSuperAdmin(target)) {
            throw new ForbiddenException("Seul un super-administrateur peut agir sur le compte d'un super-administrateur.");
        }
    }

    /** Ce compte est-il le dernier super-administrateur actif · on ne le retire pas. */
    public void assertKeepsASuperAdmin(Users target) {
        if (target != null && isSuperAdmin(target)
                && roleUserRepository.countOtherEnabledHolders(SUPER_ADMIN, target.getId()) == 0) {
            throw new ForbiddenException("Ce compte est le dernier super-administrateur actif · "
                    + "designez-en un autre avant de le retirer.");
        }
    }

    /**
     * Modifier un role · ses permissions, son etat, sa suppression.
     *
     * @param newPermissionNames les permissions qu'il portera, ou {@code null} si elles ne changent pas
     */
    public Actor assertMayEditRole(Role role, Collection<String> newPermissionNames) {
        Actor actor = assertMayManageRoles();
        if (actor.isSuperAdmin() || role == null) {
            return actor;
        }
        if (SUPER_ADMIN.equalsIgnoreCase(role.getName())) {
            throw new ForbiddenException("Seul un super-administrateur peut modifier le role " + SUPER_ADMIN + ".");
        }
        if (actor.holdsRole(role.getName())) {
            throw new ForbiddenException("Vous ne pouvez pas modifier un role que vous portez · "
                    + "ce serait changer vos propres droits.");
        }
        if (newPermissionNames != null) {
            assertHoldsAll(actor, newPermissionNames, "Ce role donnerait des droits que vous n'avez pas");
        }
        return actor;
    }

    public boolean isSuperAdmin(Users user) {
        return user != null
                && roleUserRepository.findActiveRoleNamesByUserId(user.getId()).stream()
                .anyMatch(SUPER_ADMIN::equalsIgnoreCase);
    }

    private void assertHoldsAll(Actor actor, Collection<String> permissions, String message) {
        List<String> missing = permissions.stream()
                .filter(Objects::nonNull)
                .filter(permission -> !actor.authorities().contains(permission))
                .sorted()
                .toList();
        if (!missing.isEmpty()) {
            throw new ForbiddenException(message + " : " + String.join(", ", missing) + ".");
        }
    }
}
