package com.sni.bokaticowork.features.ressource;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.ressource.dto.request.ResourcePhotoRequest;
import com.sni.bokaticowork.features.ressource.dto.response.PublicResourceCalendarResponse;
import com.sni.bokaticowork.features.ressource.enums.ResourceStatus;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourcePhoto;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAvailabilityRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourcePhotoRepository;
import com.sni.bokaticowork.features.ressource.service.implementation.PublicResourceCalendarServiceImpl;
import com.sni.bokaticowork.features.ressource.service.implementation.ResourcePhotoServiceImpl;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
import com.sni.bokaticowork.features.ressource.service.support.ResourceCalendarRange;
import com.sni.bokaticowork.security.authorization.AdminApiAuthorizationManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot R0 des ressources : calendrier public, photos, droits. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResourceR0Test {

    @Mock ResourceService resourceService;
    @Mock ResourceAvailabilityRepository availabilityRepository;
    @InjectMocks PublicResourceCalendarServiceImpl calendar;

    @Mock ResourcePhotoRepository photoRepository;
    @Mock DocumentRepository documentRepository;
    @InjectMocks ResourcePhotoServiceImpl photos;

    private Resource resource(boolean visible) {
        return Resource.builder().id(1L).code("RES-1").name("Salle").capacity(4).status(ResourceStatus.ACTIVE)
                .portalVisible(visible).bookingEnabled(true).active(true).build();
    }

    // ------------------------------------------------------------------ calendrier

    @Test
    void aHiddenResourceIsNotFoundOnThePublicCalendar() {
        when(resourceService.getResourceForService("RES-1")).thenReturn(resource(false));
        assertThatThrownBy(() -> calendar.calendar("RES-1", null, null)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void theRangeIsBoundedAndOrdered() {
        LocalDate day = LocalDate.of(2026, 12, 1);
        assertThatThrownBy(() -> ResourceCalendarRange.of(day, day.minusDays(1))).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> ResourceCalendarRange.of(day, day.plusDays(ResourceCalendarRange.MAX_DAYS)))
                .isInstanceOf(BadRequestException.class);
        assertThat(ResourceCalendarRange.of(day, day.plusDays(ResourceCalendarRange.MAX_DAYS - 1)).end())
                .isEqualTo(day.plusDays(ResourceCalendarRange.MAX_DAYS - 1));
    }

    @Test
    void aDayWithoutSlotsIsClosedNotFull() {
        when(resourceService.getResourceForService("RES-1")).thenReturn(resource(true));
        when(availabilityRepository.findAllSlotsInRange(any(), any(), any())).thenReturn(List.of());
        LocalDate day = LocalDate.of(2026, 12, 1);

        var response = calendar.calendar("RES-1", day, day);

        assertThat(response.days()).singleElement().extracting(PublicResourceCalendarResponse.Day::status).isEqualTo("CLOSED");
    }

    // ------------------------------------------------------------------ photos

    private ResourcePhotoRequest request(String documentCode, boolean cover) {
        ResourcePhotoRequest request = new ResourcePhotoRequest();
        request.setDocumentCode(documentCode);
        request.setCover(cover);
        return request;
    }

    @Test
    void aPhotoOfAnotherResourceIsNotFound() {
        ReflectionTestUtils.setField(photos, "documentRepository", documentRepository);
        ResourcePhoto photo = ResourcePhoto.builder().id(9L).resource(Resource.builder().code("RES-2").build()).build();
        when(photoRepository.findById(9L)).thenReturn(Optional.of(photo));
        when(documentRepository.findByCode("DOC-1")).thenReturn(Optional.of(Document.builder().code("DOC-1").mimeType("image/png").build()));

        assertThatThrownBy(() -> photos.update("RES-1", 9L, request("DOC-1", false))).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> photos.delete("RES-1", 9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void thePhotoMustBeAnImageOfTheGed() {
        ReflectionTestUtils.setField(photos, "documentRepository", documentRepository);
        when(documentRepository.findByCode("DOC-PDF")).thenReturn(Optional.of(Document.builder().code("DOC-PDF").mimeType("application/pdf").build()));
        when(documentRepository.findByCode("DOC-X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> photos.add("RES-1", request("DOC-PDF", false))).hasMessageContaining("n'est pas une image");
        assertThatThrownBy(() -> photos.add("RES-1", request("DOC-X", false))).hasMessageContaining("introuvable");
    }

    @Test
    void aNewCoverReplacesThePreviousOne() {
        ReflectionTestUtils.setField(photos, "documentRepository", documentRepository);
        Resource res = resource(true);
        ResourcePhoto old = ResourcePhoto.builder().id(1L).resource(res).cover(true).active(true).build();
        when(resourceService.getResourceForService("RES-1")).thenReturn(res);
        when(photoRepository.findAllByResourceAndActiveTrueOrderByDisplayOrderAscIdAsc(res)).thenReturn(List.of(old));
        when(photoRepository.save(any())).thenAnswer(invocation -> {
            ResourcePhoto saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(2L);
            }
            return saved;
        });
        when(documentRepository.findByCode("DOC-1")).thenReturn(Optional.of(Document.builder().code("DOC-1").mimeType("image/jpeg").build()));

        photos.add("RES-1", request("DOC-1", true));

        assertThat(old.getCover()).isFalse();
        verify(photoRepository).save(old);
    }

    // ------------------------------------------------------------------ droits

    private String required(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        TestingAuthenticationToken token = new TestingAuthenticationToken("a@b.c", null, "ROLE_ADMIN");
        token.setAuthenticated(true);
        new AdminApiAuthorizationManager().authorize(() -> token, new RequestAuthorizationContext(request));
        return (String) request.getAttribute(AdminApiAuthorizationManager.REQUIRED_PERMISSION_ATTRIBUTE);
    }

    @Test
    void searchingAndViewingTheGalleryAreReads() {
        String base = "/sni/api/v1";
        assertThat(required("POST", base + "/resources/search")).isEqualTo("RESOURCE:READ");
        assertThat(required("GET", base + "/resources/RES-1/photos")).isEqualTo("RESOURCE:READ");
        assertThat(required("PATCH", base + "/resources/RES-1/photos/3")).isEqualTo("RESOURCE:GALLERY");
        assertThat(required("PUT", base + "/resources/RES-1/opening-hours")).isEqualTo("RESOURCE:UPDATE");
        assertThat(required("POST", base + "/resources")).isEqualTo("RESOURCE:CREATE");
    }
}
