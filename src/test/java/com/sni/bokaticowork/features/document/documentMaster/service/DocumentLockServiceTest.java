package com.sni.bokaticowork.features.document.documentMaster.service;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentLockService;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.admin.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Le verrou d'un document : il protege son poseur, expire seul, et ne se leve par un autre qu'en
 * force (administrateur ou gestionnaire du dossier).
 */
class DocumentLockServiceTest {

    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final DocumentLockService service = new DocumentLockService(documents, users);

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    private static void as(long userId) {
        Users user = new Users();
        user.setId(userId);
        UserPrincipal principal = new UserPrincipal();
        ReflectionTestUtils.setField(principal, "user", user);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private Document document() {
        when(documents.save(any(Document.class))).thenAnswer(call -> call.getArgument(0));
        Users alice = new Users();
        alice.setEmail("alice@test.cg");
        when(users.findById(1L)).thenReturn(Optional.of(alice));
        return Document.builder().code("DOC-1").title("statuts.pdf").build();
    }

    @Test
    @DisplayName("Le verrou d'Alice bloque Bob, pas Alice ; il se prolonge et se plafonne a 24 heures")
    void lockProtectsItsOwner() {
        Document document = document();
        as(1L);
        service.lock(document, Duration.ofHours(48));
        assertThat(document.getLockExpiresAt()).isBefore(Instant.now().plus(Duration.ofHours(24)).plusSeconds(5));
        assertThatCode(() -> service.assertEditable(document)).doesNotThrowAnyException();

        as(2L);
        assertThatThrownBy(() -> service.assertEditable(document))
                .isInstanceOf(ConflictException.class).hasMessageContaining("verrouillé par alice@test.cg");
        assertThatThrownBy(() -> service.lock(document, Duration.ofHours(1))).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.unlock(document, false)).isInstanceOf(ConflictException.class);

        service.unlock(document, true);
        assertThat(document.getLockedBy()).isNull();
    }

    @Test
    @DisplayName("Un verrou expire seul : passe l'heure, un autre modifie et verrouille a son tour")
    void expiredLockNoLongerBlocks() {
        Document document = document();
        document.setLockedBy(1L);
        document.setLockedAt(Instant.now().minus(Duration.ofHours(3)));
        document.setLockExpiresAt(Instant.now().minus(Duration.ofHours(2)));

        as(2L);
        assertThat(DocumentLockService.active(document)).isFalse();
        assertThatCode(() -> service.assertEditable(document)).doesNotThrowAnyException();
        service.lock(document, null);
        assertThat(document.getLockedBy()).isEqualTo(2L);
    }
}
