package com.sni.bokaticowork.features.booking;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.booking.repository.BookingEndReportRepository;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.support.BookingEventWriter;
import com.sni.bokaticowork.features.booking.service.support.BookingOperations;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceTaskTemplateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Lot B0 du module booking : traçabilite et controles. */
class BookingB0Test {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void theActorComesFromTheSessionNotFromTheRequest() throws Exception {
        Object service = mock(com.sni.bokaticowork.features.booking.service.implementation.BookingServiceImpl.class,
                org.mockito.Mockito.CALLS_REAL_METHODS);
        Method actor = com.sni.bokaticowork.features.booking.service.implementation.BookingServiceImpl.class.getDeclaredMethod("actor", String.class);
        actor.setAccessible(true);

        // Sans session (tache du systeme, paiement recu) : la valeur fournie.
        assertThat(actor.invoke(service, "SYSTEM")).isEqualTo("SYSTEM");

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "agent@elleaose.com", null, List.of(new SimpleGrantedAuthority("ROLE_OPERATIONS_AGENT"))));
        assertThat(actor.invoke(service, "quelqu'un d'autre")).isEqualTo("agent@elleaose.com");
    }

    @Test
    void aMemberCanOnlyAttachHisOwnDocuments() {
        DocumentRepository documents = mock(DocumentRepository.class);
        Document mine = new Document();
        mine.setCode("DOC-1");
        mine.setOwnerType(DocumentOwnerType.MEMBER);
        mine.setOwnerId(7L);
        Document other = new Document();
        other.setCode("DOC-2");
        other.setOwnerType(DocumentOwnerType.MEMBER);
        other.setOwnerId(8L);
        when(documents.findByCode("DOC-1")).thenReturn(Optional.of(mine));
        when(documents.findByCode("DOC-2")).thenReturn(Optional.of(other));
        when(documents.findByCode("DOC-X")).thenReturn(Optional.empty());
        BookingOperations operations = new BookingOperations(mock(BookingRepository.class), mock(BookingEndReportRepository.class),
                mock(ResourceTaskTemplateRepository.class), mock(BookingEventWriter.class));
        ReflectionTestUtils.setField(operations, "documents", documents);

        assertThatCode(() -> operations.checkPhotos(List.of("DOC-1"), 7L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> operations.checkPhotos(List.of("DOC-2"), 7L)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> operations.checkPhotos(List.of("DOC-X"), null)).isInstanceOf(BadRequestException.class);
        // L'equipe joint tout document existant.
        assertThatCode(() -> operations.checkPhotos(List.of("DOC-2"), null)).doesNotThrowAnyException();
    }
}
