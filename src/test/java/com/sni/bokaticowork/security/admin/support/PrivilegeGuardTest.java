package com.sni.bokaticowork.security.admin.support;

import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.security.admin.role.model.Role;
import com.sni.bokaticowork.security.admin.role.repository.RoleUserRepository;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import com.sni.bokaticowork.security.admin.user.model.Users;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Personne ne s'eleve soi-meme, et un super-administrateur n'est gere que par ses pairs.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PrivilegeGuardTest {

    @Mock private RoleUserRepository roleUserRepository;
    @InjectMocks private PrivilegeGuard guard;

    @BeforeEach
    void setUp() {
        when(roleUserRepository.findActiveRoleNamesByUserId(anyLong())).thenReturn(List.of());
        when(roleUserRepository.findActivePermissionNamesByRoleId(anyLong())).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static Users user(long id) {
        Users user = new Users();
        user.setId(id);
        user.setEmail("u" + id + "@example.com");
        return user;
    }

    private void connectAs(long id, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UserPrincipal(user(id)), null,
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }

    private static Role role(long id, String name) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        return role;
    }

    private void superAdminTarget(long id) {
        when(roleUserRepository.findActiveRoleNamesByUserId(id)).thenReturn(List.of("SUPER_ADMIN"));
    }

    @Test
    @DisplayName("Un compte STAFF, meme avec SYSTEM:ROLES, ne gere pas les roles")
    void staffCannotManageRoles() {
        connectAs(1, "ROLE_STAFF", "SYSTEM:ROLES");

        assertThatThrownBy(guard::assertMayManageRoles).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Sans compte connecte, rien ne passe")
    void anonymousIsRefused() {
        assertThatThrownBy(guard::currentActor).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Un administrateur n'attribue pas le role super-administrateur, un super-administrateur si")
    void onlySuperAdminGrantsSuperAdmin() {
        connectAs(1, "ROLE_ADMIN");
        PrivilegeGuard.Actor admin = guard.currentActor();
        assertThatThrownBy(() -> guard.assertMayGrant(admin, role(9, "SUPER_ADMIN"))).isInstanceOf(ForbiddenException.class);

        connectAs(2, "ROLE_SUPER_ADMIN");
        PrivilegeGuard.Actor superAdmin = guard.currentActor();
        assertThatCode(() -> guard.assertMayGrant(superAdmin, role(9, "SUPER_ADMIN"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Un role dont une permission manque a l'acteur est plus puissant que lui")
    void aRoleWithMissingPermissionsCannotBeGranted() {
        connectAs(1, "ROLE_ADMIN", "BOOKING:READ");
        when(roleUserRepository.findActivePermissionNamesByRoleId(5L)).thenReturn(List.of("BOOKING:READ", "BILLING:REFUND"));
        when(roleUserRepository.findActivePermissionNamesByRoleId(6L)).thenReturn(List.of("BOOKING:READ"));
        PrivilegeGuard.Actor admin = guard.currentActor();

        assertThatThrownBy(() -> guard.assertMayGrant(admin, role(5, "COMPTABLE")))
                .isInstanceOf(ForbiddenException.class).hasMessageContaining("BILLING:REFUND");
        assertThatCode(() -> guard.assertMayGrant(admin, role(6, "ACCUEIL"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Personne ne modifie ses propres roles")
    void nobodyChangesTheirOwnRoles() {
        connectAs(7, "ROLE_SUPER_ADMIN");
        PrivilegeGuard.Actor actor = guard.currentActor();

        assertThatThrownBy(() -> guard.assertNotSelf(actor, user(7))).isInstanceOf(ForbiddenException.class);
        assertThatCode(() -> guard.assertNotSelf(actor, user(8))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Un administrateur n'agit pas sur le compte d'un super-administrateur")
    void adminCannotActOnSuperAdmin() {
        superAdminTarget(3);
        connectAs(1, "ROLE_ADMIN");
        PrivilegeGuard.Actor admin = guard.currentActor();

        assertThatThrownBy(() -> guard.assertMayActOn(admin, user(3))).isInstanceOf(ForbiddenException.class);
        assertThatCode(() -> guard.assertMayActOn(admin, user(4))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Le dernier super-administrateur actif ne peut pas etre retire")
    void theLastSuperAdminIsKept() {
        superAdminTarget(3);
        when(roleUserRepository.countOtherEnabledHolders(eq("SUPER_ADMIN"), eq(3L))).thenReturn(0L);
        assertThatThrownBy(() -> guard.assertKeepsASuperAdmin(user(3))).isInstanceOf(ForbiddenException.class);

        when(roleUserRepository.countOtherEnabledHolders(eq("SUPER_ADMIN"), eq(3L))).thenReturn(1L);
        assertThatCode(() -> guard.assertKeepsASuperAdmin(user(3))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Un administrateur ne modifie ni son propre role, ni celui de super-administrateur")
    void adminCannotEditOwnRoleOrSuperAdminRole() {
        connectAs(1, "ROLE_ADMIN", "BOOKING:READ");

        assertThatThrownBy(() -> guard.assertMayEditRole(role(2, "ADMIN"), List.of("BOOKING:READ")))
                .isInstanceOf(ForbiddenException.class).hasMessageContaining("vous portez");
        assertThatThrownBy(() -> guard.assertMayEditRole(role(1, "SUPER_ADMIN"), null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Un administrateur ne donne pas a un role une permission qu'il n'a pas")
    void adminCannotGiveAPermissionHeLacks() {
        connectAs(1, "ROLE_ADMIN", "BOOKING:READ");

        assertThatThrownBy(() -> guard.assertMayEditRole(role(6, "ACCUEIL"), List.of("BOOKING:READ", "SYSTEM:ROLES")))
                .isInstanceOf(ForbiddenException.class).hasMessageContaining("SYSTEM:ROLES");
        assertThatCode(() -> guard.assertMayEditRole(role(6, "ACCUEIL"), List.of("BOOKING:READ")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Un super-administrateur modifie n'importe quel role, le sien compris")
    void superAdminEditsAnyRole() {
        connectAs(1, "ROLE_SUPER_ADMIN");

        assertThatCode(() -> guard.assertMayEditRole(role(1, "SUPER_ADMIN"), List.of("X:Y"))).doesNotThrowAnyException();
    }
}
