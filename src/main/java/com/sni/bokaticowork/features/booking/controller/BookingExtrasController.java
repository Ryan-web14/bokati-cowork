package com.sni.bokaticowork.features.booking.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.dto.request.CreateBookingBatchRequest;
import com.sni.bokaticowork.features.booking.dto.response.BookingBatchResponse;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.service.implementation.BookingBatchServiceImpl;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingBatchService;
import com.sni.bokaticowork.features.booking.service.support.BookingExtras;
import com.sni.bokaticowork.features.ressource.model.ResourceBundle;
import com.sni.bokaticowork.features.ressource.service.support.ResourceBundles;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/** Lot R5c : supplements d'une reservation, code promo, reservation d'un lot de ressources. */
@RestController
@RequiredArgsConstructor
public class BookingExtrasController {

    private static final String BOOKINGS = ApiPath.V1 + "/bookings";

    private final BookingExtras extras;
    private final ResourceBundles bundles;
    private final BookingBatchService batchService;

    public record ItemRequest(String code, Integer quantity) {
    }

    public record ParticipantsRequest(Integer count, Boolean waive) {
    }

    public record PromoRequest(String code) {
    }

    /** Reservation d'un lot : chaque ressource du lot sur le meme creneau, une facture par membre, remise du lot. */
    public record BundleBookingRequest(List<String> memberIds, LocalDateTime startedAt, LocalDateTime endedAt,
                                       BookingPaymentMode paymentMode, String billingCompanyCode, Boolean confirmImmediately,
                                       Boolean sendEmail, String notes, String idempotencyKey) {
    }

    @GetMapping(BOOKINGS + "/{number}/lines")
    public ResponseEntity<List<BookingExtras.LineView>> lines(@PathVariable String number) {
        return ResponseEntity.ok(extras.lines(number));
    }

    @PostMapping(BOOKINGS + "/{number}/services")
    @Audited(module = "BOOKING", action = "ADD_SERVICE", ressource = "booking")
    public ResponseEntity<BookingExtras.LineView> addService(@PathVariable String number, @RequestBody ItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(extras.addService(number, request.code(), request.quantity()));
    }

    @PostMapping(BOOKINGS + "/{number}/amenities")
    @Audited(module = "BOOKING", action = "ADD_AMENITY", ressource = "booking")
    public ResponseEntity<BookingExtras.LineView> addAmenity(@PathVariable String number, @RequestBody ItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(extras.addAmenity(number, request.code(), request.quantity()));
    }

    @PostMapping(BOOKINGS + "/{number}/extra-participants")
    @Audited(module = "BOOKING", action = "ADD_PARTICIPANTS", ressource = "booking")
    public ResponseEntity<BookingExtras.LineView> addParticipants(@PathVariable String number, @RequestBody ParticipantsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(extras.addParticipants(number, request.count(), Boolean.TRUE.equals(request.waive())));
    }

    @PostMapping(BOOKINGS + "/{number}/promo-code")
    @Audited(module = "BOOKING", action = "APPLY_PROMO_CODE", ressource = "booking")
    public ResponseEntity<BookingExtras.LineView> promo(@PathVariable String number, @RequestBody PromoRequest request,
                                                       Authentication authentication) {
        return ResponseEntity.ok(extras.applyPromoCode(number, request.code(), authentication == null ? null : authentication.getName()));
    }

    @PostMapping(ApiPath.V1 + "/resource-bundles/{code}/book")
    @Audited(module = "BOOKING", action = "BOOK_BUNDLE", ressource = "booking")
    @Transactional
    public ResponseEntity<BookingBatchResponse> book(@PathVariable String code, @RequestBody BundleBookingRequest request) {
        ResourceBundle bundle = bundles.entity(code);
        if (!Boolean.TRUE.equals(bundle.getActive())) {
            throw new com.sni.bokaticowork.core.exception.customs.ConflictException("Le lot " + bundle.getCode() + " est désactivé");
        }
        List<CreateBookingBatchRequest.Line> lines = bundle.getItems().stream()
                .map(item -> new CreateBookingBatchRequest.Line(item.getResource().getCode(), request.startedAt(), request.endedAt(),
                        item.getQuantity(), null, null, "Lot " + bundle.getName(), null))
                .toList();
        CreateBookingBatchRequest batch = new CreateBookingBatchRequest(request.memberIds(), lines,
                request.paymentMode() == null ? BookingPaymentMode.DIRECT : request.paymentMode(), request.billingCompanyCode(),
                request.confirmImmediately(), request.sendEmail(),
                request.notes() == null ? "Lot " + bundle.getName() : request.notes(), request.idempotencyKey());
        BookingBatchServiceImpl.withBundleDiscount(bundle.getName(), bundle.getDiscountPercent());
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(batchService.create(batch));
        } finally {
            BookingBatchServiceImpl.withBundleDiscount(null, null);
        }
    }
}
