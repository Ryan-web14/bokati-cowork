package com.sni.bokaticowork.security.admin.user;

import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.security.admin.role.repository.RoleUserRepository;
import com.sni.bokaticowork.security.admin.role.service.interfaces.RoleUserService;
import com.sni.bokaticowork.security.admin.support.PrivilegeGuard;
import com.sni.bokaticowork.security.admin.user.dto.request.AdminResetUserPasswordRequest;
import com.sni.bokaticowork.security.admin.user.dto.request.AdminUpdateUserRequest;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.admin.user.repository.UserRepository;
import com.sni.bokaticowork.security.admin.user.service.implementation.UserAdminServiceImpl;
import com.sni.bokaticowork.security.admin.user.service.interfaces.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le service des comptes consulte le garde-fou avant d'agir, et s'arrete s'il refuse.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserAdminPrivilegeWiringTest {

    @Mock private UserService userService;
    @Mock private UserRepository userRepository;
    @Mock private RoleUserService roleUserService;
    @Mock private RoleUserRepository roleUserRepository;
    @Mock private PrivilegeGuard privilegeGuard;
    @InjectMocks private UserAdminServiceImpl service;

    private final Users superAdmin = new Users();
    private final PrivilegeGuard.Actor admin = new PrivilegeGuard.Actor(1L, "admin@x.com", Set.of("ROLE_ADMIN"));

    @BeforeEach
    void setUp() {
        superAdmin.setId(2L);
        superAdmin.setUserId("USR-2");
        superAdmin.setEmail("super@x.com");
        when(userRepository.findByUserId("USR-2")).thenReturn(Optional.of(superAdmin));
        when(privilegeGuard.currentActor()).thenReturn(admin);
        when(privilegeGuard.assertMayManageRoles()).thenReturn(admin);
        when(roleUserRepository.findByUserId(anyLong())).thenReturn(List.of());
    }

    @Test
    @DisplayName("Un administrateur ne reinitialise pas le mot de passe d'un super-administrateur")
    void passwordResetOnSuperAdminIsRefusedBeforeAnyChange() {
        doThrow(new ForbiddenException("non")).when(privilegeGuard).assertMayActOn(admin, superAdmin);

        assertThatThrownBy(() -> service.resetPassword("USR-2", new AdminResetUserPasswordRequest()))
                .isInstanceOf(ForbiddenException.class);
        verify(userService, never()).resetUserPassword(any(), anyString());
    }

    @Test
    @DisplayName("Un refus sur les roles arrete la mise a jour avant toute ecriture")
    void roleChangeRefusalStopsTheUpdate() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setRoleNames(List.of("SUPER_ADMIN"));
        doThrow(new ForbiddenException("non")).when(privilegeGuard).assertNotSelf(admin, superAdmin);

        assertThatThrownBy(() -> service.update("USR-2", request, "admin@x.com")).isInstanceOf(ForbiddenException.class);
        verify(userRepository, never()).save(any());
        verify(roleUserService, never()).addRoleToUser(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("Desactiver le dernier super-administrateur est refuse")
    void deactivatingTheLastSuperAdminIsRefused() {
        doThrow(new ForbiddenException("dernier")).when(privilegeGuard).assertKeepsASuperAdmin(superAdmin);

        assertThatThrownBy(() -> service.deactivate("USR-2")).isInstanceOf(ForbiddenException.class);
        verify(userService, never()).deactivateUser(anyString());
    }
}
