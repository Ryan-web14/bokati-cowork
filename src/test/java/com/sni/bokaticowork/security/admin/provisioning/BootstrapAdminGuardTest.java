package com.sni.bokaticowork.security.admin.provisioning;

import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.security.admin.provisioning.service.implementation.UserProvisioningServiceImpl;
import com.sni.bokaticowork.security.admin.role.service.interfaces.RoleUserService;
import com.sni.bokaticowork.security.admin.user.dto.request.UserRequest;
import com.sni.bokaticowork.security.admin.user.model.Users;
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
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un administrateur ne se cree plus avec une simple requete anonyme.
 *
 * <p>Le garde-fou qui refermait l'amorcage apres le premier administrateur etait commente, et la
 * route est publique : une seule requete POST non authentifiee rendait un compte ADMIN active, a
 * n'importe qui, autant de fois qu'il le voulait. C'etait la faille la plus directement
 * exploitable du systeme, et elle ne dependait d'aucun reglage.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BootstrapAdminGuardTest {

    private static final String SECRET = "le-secret-d-amorcage";

    @Mock private UserService userService;
    @Mock private RoleUserService roleUserService;
    @InjectMocks private UserProvisioningServiceImpl service;

    private UserRequest request;

    @BeforeEach
    void setUp() {
        Users created = new Users();
        created.setId(1L);
        created.setEmail("premier@elleaose.com");
        when(userService.createUser(any())).thenReturn(created);

        request = new UserRequest();
        request.setEmail("premier@elleaose.com");
        request.setPassword("MotDePasse1!");
    }

    private void secretConfigured(String value) {
        ReflectionTestUtils.setField(service, "bootstrapSecret", value);
    }

    @Test
    @DisplayName("Sans secret configuré, l'amorçage est fermé · c'est le bon état par défaut")
    void closedWhenNoSecretIsConfigured() {
        secretConfigured("");
        request.setBootstrapSecret("peu importe");

        assertThatThrownBy(() -> service.initializeGlobalAdmin(request))
                .isInstanceOf(ForbiddenException.class);

        verify(userService, never()).createUser(any());
        verify(roleUserService, never()).addRoleToUser(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("Sans secret dans la demande, refus")
    void refusedWithoutTheSecretInTheRequest() {
        secretConfigured(SECRET);
        request.setBootstrapSecret(null);

        assertThatThrownBy(() -> service.initializeGlobalAdmin(request))
                .isInstanceOf(ForbiddenException.class);
        verify(userService, never()).createUser(any());
    }

    @Test
    @DisplayName("Avec un mauvais secret, refus")
    void refusedWithTheWrongSecret() {
        secretConfigured(SECRET);
        request.setBootstrapSecret("pas-le-bon");

        assertThatThrownBy(() -> service.initializeGlobalAdmin(request))
                .isInstanceOf(ForbiddenException.class);
        verify(userService, never()).createUser(any());
    }

    @Test
    @DisplayName("Dès qu'un administrateur existe, la porte est refermée définitivement")
    void refusedOnceAnAdministratorExists() {
        secretConfigured(SECRET);
        request.setBootstrapSecret(SECRET);
        when(roleUserService.hasAnyUserAssignedToRole("ADMIN")).thenReturn(true);

        assertThatThrownBy(() -> service.initializeGlobalAdmin(request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("déjà");

        verify(userService, never()).createUser(any());
    }

    @Test
    @DisplayName("Le bon secret, et aucun administrateur · l'amorçage passe, une fois")
    void allowedWithTheRightSecretAndNoAdministrator() {
        secretConfigured(SECRET);
        request.setBootstrapSecret(SECRET);
        when(roleUserService.hasAnyUserAssignedToRole("ADMIN")).thenReturn(false);

        Users admin = service.initializeGlobalAdmin(request);

        assertThat(admin.getEmail()).isEqualTo("premier@elleaose.com");
        verify(roleUserService).addRoleToUser(1L, "ADMIN", "SYSTEM");
        verify(userService).activateUser("premier@elleaose.com");
    }

    @Test
    @DisplayName("Une demande absente ne contourne rien")
    void aMissingRequestBypassesNothing() {
        secretConfigured(SECRET);

        assertThatThrownBy(() -> service.initializeGlobalAdmin(null))
                .isInstanceOf(ForbiddenException.class);
        verify(userService, never()).createUser(any());
    }

    @Test
    @DisplayName("Le secret se compare en temps constant · les espaces de bordure sont ignorés")
    void theSecretIsComparedLeniencyOnWhitespaceOnly() {
        secretConfigured("  " + SECRET + "  ");
        request.setBootstrapSecret(SECRET + " ");
        when(roleUserService.hasAnyUserAssignedToRole("ADMIN")).thenReturn(false);

        assertThat(service.initializeGlobalAdmin(request)).isNotNull();
    }
}
