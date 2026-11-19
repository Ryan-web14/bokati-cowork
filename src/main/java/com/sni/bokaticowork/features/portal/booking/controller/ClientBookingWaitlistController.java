package com.sni.bokaticowork.features.portal.booking.controller;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.dto.request.BookingIdentityLookup;
import com.sni.bokaticowork.features.booking.dto.request.JoinBookingWaitlistRequest;
import com.sni.bokaticowork.features.booking.dto.response.BookingWaitlistEntryResponse;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.enums.BookingWaitlistStatus;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingWaitlistService;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import com.sni.bokaticowork.features.portal.resource.service.ClientResourceService;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Lot B6 : liste d'attente au portail. Le membre s'inscrit sur un creneau pris, suit ses
 * inscriptions, accepte ou decline la place proposee (la reservation est creee a l'acceptation).
 */
@RestController
@RequestMapping(ApiPath.V1 + "/client/bookings/waitlist")
@RequiredArgsConstructor
public class ClientBookingWaitlistController {

    private final ClientContextService clientContextService;
    private final BookingWaitlistService waitlist;
    private final ClientResourceService clientResourceService;

    public record JoinRequest(@NotBlank String resourceCode, @Future @NotNull LocalDateTime startedAt, @Future @NotNull LocalDateTime endedAt,
                              @Positive Integer quantity, @NotNull BookingPaymentMode paymentMode) {
    }

    @PostMapping
    public ResponseEntity<BookingWaitlistEntryResponse> join(@Valid @RequestBody JoinRequest request) {
        Member member = clientContextService.getAuthenticatedMember();
        clientResourceService.assertPortalVisible(request.resourceCode());
        BookingIdentityLookup identity = new BookingIdentityLookup(member.getMemberId(), null, null, null, null, Boolean.FALSE,
                null, null, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(waitlist.join(new JoinBookingWaitlistRequest(request.resourceCode(), identity,
                request.startedAt(), request.endedAt(), request.quantity(), request.paymentMode())));
    }

    /** Mes inscriptions, filtrables par etat. */
    @GetMapping
    public ResponseEntity<PaginatedResponse<BookingWaitlistEntryResponse>> mine(@RequestParam(required = false) BookingWaitlistStatus status,
                                                                                @RequestParam(defaultValue = "0") int page,
                                                                                @RequestParam(defaultValue = "20") int size) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(waitlist.search(status, null, SubscriberType.MEMBER, member.getMemberId(), PageRequest.of(page, size)));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<BookingWaitlistEntryResponse> accept(@PathVariable Long id) {
        owned(id);
        return ResponseEntity.ok(waitlist.accept(id));
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<BookingWaitlistEntryResponse> decline(@PathVariable Long id) {
        owned(id);
        return ResponseEntity.ok(waitlist.decline(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BookingWaitlistEntryResponse> cancel(@PathVariable Long id) {
        owned(id);
        return ResponseEntity.ok(waitlist.cancel(id));
    }

    /** Une inscription d'un autre se presente comme introuvable. */
    private void owned(Long id) {
        Member member = clientContextService.getAuthenticatedMember();
        BookingWaitlistEntryResponse entry = waitlist.get(id);
        if (entry.ownerType() != SubscriberType.MEMBER || !member.getMemberId().equals(entry.ownerCode())) {
            throw new ResourceNotFoundException("Inscription en liste d'attente introuvable");
        }
    }
}
