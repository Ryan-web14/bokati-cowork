package com.sni.bokaticowork.security.admin.user.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.security.admin.user.dto.request.UpdateStaffProfileRequest;
import com.sni.bokaticowork.security.admin.user.model.StaffProfile;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.admin.user.repository.StaffProfileRepository;
import com.sni.bokaticowork.security.admin.user.repository.UserRepository;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Lot B12 : l'auteur vient du jeton, avec son nom et sa fonction ; signature et hierarchie controlees. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StaffProfileServiceImplTest {

    @Mock private StaffProfileRepository profileRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private StaffProfileServiceImpl service;

    private final Users awa = user(1L, "USR-1", "awa@elleaose.com", "Awa", "Mbemba");
    private final Users paul = user(2L, "USR-2", "paul@elleaose.com", "Paul", "Ngoma");

    @BeforeEach
    void setUp() {
        for (Users u : List.of(awa, paul)) {
            when(userRepository.findByEmailIgnoreCase(u.getEmail())).thenReturn(Optional.of(u));
            when(userRepository.findByUserId(u.getUserId())).thenReturn(Optional.of(u));
            when(userRepository.findById(u.getId())).thenReturn(Optional.of(u));
        }
        when(profileRepository.save(any(StaffProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldTakeTheAuthorFromTheToken() {
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(StaffProfile.builder().userId(1L).jobTitle("Commerciale").build()));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("awa@elleaose.com", null, List.of()));

        StaffProfileService.Author author = service.currentAuthor();

        assertThat(author.login()).isEqualTo("awa@elleaose.com");
        assertThat(author.name()).isEqualTo("Awa Mbemba");
        assertThat(author.title()).isEqualTo("Commerciale");
    }

    @Test
    void shouldFallBackToSystemWithoutToken() {
        assertThat(service.currentAuthor()).isEqualTo(StaffProfileService.Author.SYSTEM);
    }

    @Test
    void shouldRefuseASignatureThatIsNotAnImage() {
        assertThatThrownBy(() -> service.update("USR-1", new UpdateStaffProfileRequest(null, null, "data:text/html;base64,PHNjcmlwdD4=", null)))
                .isInstanceOf(BadRequestException.class);
        assertThat(service.update("USR-1", new UpdateStaffProfileRequest("Commerciale", null, "data:image/png;base64,iVBORw0KGgo=", null))
                .hasSignature()).isTrue();
    }

    @Test
    void shouldRefuseAHierarchyLoop() {
        // Paul depend deja d'Awa : Awa ne peut pas dependre de Paul.
        when(profileRepository.findByUserId(2L)).thenReturn(Optional.of(StaffProfile.builder().userId(2L).managerUserId(1L).build()));

        assertThatThrownBy(() -> service.update("USR-1", new UpdateStaffProfileRequest(null, "USR-2", null, null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("boucle");
        assertThatThrownBy(() -> service.update("USR-1", new UpdateStaffProfileRequest(null, "USR-1", null, null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldSetTheManager() {
        assertThat(service.update("USR-2", new UpdateStaffProfileRequest(null, "USR-1", null, null)).managerName()).isEqualTo("Awa Mbemba");
    }

    private static Users user(long id, String code, String email, String first, String last) {
        Users user = new Users();
        user.setId(id);
        user.setUserId(code);
        user.setEmail(email);
        user.setFirstname(first);
        user.setLastname(last);
        return user;
    }
}
