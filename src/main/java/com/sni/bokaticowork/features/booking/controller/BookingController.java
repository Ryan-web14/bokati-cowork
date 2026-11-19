package com.sni.bokaticowork.features.booking.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.dto.request.BookingApprovalRequest;
import com.sni.bokaticowork.features.booking.dto.request.BookingAvailabilityRequest;
import com.sni.bokaticowork.features.booking.dto.request.BookingChangeResourceRequest;
import com.sni.bokaticowork.features.booking.dto.request.BookingCheckRequest;
import com.sni.bokaticowork.features.booking.dto.request.BookingParticipantRequest;
import com.sni.bokaticowork.features.booking.dto.request.BookingRescheduleRequest;
import com.sni.bokaticowork.features.booking.dto.request.BookingStatusChangeRequest;
import com.sni.bokaticowork.features.booking.dto.request.BookingTransferRequest;
import com.sni.bokaticowork.features.booking.dto.request.CreateBookingAudiencePolicyRequest;
import com.sni.bokaticowork.features.booking.dto.request.CreateBookingHoldRequest;
import com.sni.bokaticowork.features.booking.dto.request.CreateBookingQuotaOverrideRequest;
import com.sni.bokaticowork.features.booking.dto.request.CreateRecurringBookingRequest;
import com.sni.bokaticowork.features.booking.dto.request.CreateBookingRequest;
import com.sni.bokaticowork.features.booking.dto.response.*;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.service.interfaces.*;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(ApiPath.V1 + "/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingAvailabilityService availabilityService;
    private final BookingHoldService holdService;
    private final BookingPolicyService policyService;
    private final BookingNotificationService notificationService;
    private final BookingMetricsService metricsService;
    private final BookingSuggestionService suggestionService;
    private final BookingRepairService repairService;
    private final com.sni.bokaticowork.features.booking.service.interfaces.BookingBatchService batchService;

    @PostMapping("/check-availability")
    public ResponseEntity<BookingAvailabilityResponse> checkAvailability(@Valid @RequestBody BookingAvailabilityRequest request) {
        return ResponseEntity.ok(availabilityService.check(request));
    }

    @PostMapping("/suggestions")
    public ResponseEntity<List<BookingSuggestionResponse>> suggestions(@Valid @RequestBody BookingAvailabilityRequest request) {
        return ResponseEntity.ok(suggestionService.suggest(request));
    }

    @PostMapping("/holds")
    public ResponseEntity<BookingHoldResponse> createHold(@Valid @RequestBody CreateBookingHoldRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(holdService.create(request));
    }

    @PatchMapping("/holds/{holdNumber}/release")
    public ResponseEntity<BookingHoldResponse> releaseHold(@PathVariable String holdNumber) {
        return ResponseEntity.ok(holdService.release(holdNumber));
    }

    @PostMapping("/holds/expire-due")
    public ResponseEntity<Integer> expireHolds(@RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(holdService.expireDue(limit));
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.create(request));
    }

    @PostMapping("/recurring")
    public ResponseEntity<List<BookingResponse>> createRecurring(@Valid @RequestBody CreateRecurringBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createRecurring(request));
    }

    /**
     * Reservation multi-ressources : plusieurs ressources, un ou plusieurs membres, une facture
     * par membre. Tout ou rien · un refus liste chaque reservation impossible dans {@code errors}.
     */
    @PostMapping("/batches")
    public ResponseEntity<com.sni.bokaticowork.features.booking.dto.response.BookingBatchResponse> createBatch(
            @Valid @RequestBody com.sni.bokaticowork.features.booking.dto.request.CreateBookingBatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(batchService.create(request));
    }

    @GetMapping("/batches")
    public ResponseEntity<com.sni.bokaticowork.core.templateResponse.PaginatedResponse<com.sni.bokaticowork.features.booking.dto.response.BookingBatchResponse.Summary>> listBatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(batchService.list(org.springframework.data.domain.PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))));
    }

    @GetMapping("/batches/{batchNumber}")
    public ResponseEntity<com.sni.bokaticowork.features.booking.dto.response.BookingBatchResponse> getBatch(@PathVariable String batchNumber) {
        return ResponseEntity.ok(batchService.get(batchNumber));
    }

    @GetMapping("/{bookingNumber}")
    public ResponseEntity<BookingResponse> get(@PathVariable String bookingNumber) {
        return ResponseEntity.ok(bookingService.get(bookingNumber));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<BookingResponse>> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) SubscriberType ownerType,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String resourceCode,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) LocalDateTime startedFrom,
            @RequestParam(required = false) LocalDateTime startedTo,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(bookingService.list(query, ownerType, ownerCode, resourceCode, status, startedFrom, startedTo, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<List<BookingResponse>> basicSearch(@RequestParam String query) {
        return ResponseEntity.ok(bookingService.basicSearch(query));
    }

    @GetMapping("/cancellation-policy")
    public ResponseEntity<List<com.sni.bokaticowork.features.booking.dto.response.CancellationPolicyResponse>> cancellationPolicy() {
        return ResponseEntity.ok(bookingService.cancellationPolicies());
    }

    @PatchMapping("/{bookingNumber}/confirm")
    public ResponseEntity<BookingResponse> confirm(@PathVariable String bookingNumber,
                                                   @RequestBody(required = false) BookingStatusChangeRequest request) {
        return ResponseEntity.ok(bookingService.confirm(bookingNumber, request));
    }

    @PatchMapping("/{bookingNumber}/approve")
    public ResponseEntity<BookingResponse> approve(@PathVariable String bookingNumber,
                                                   @RequestBody(required = false) BookingApprovalRequest request) {
        return ResponseEntity.ok(bookingService.approve(bookingNumber, request));
    }

    @PatchMapping("/{bookingNumber}/reject")
    public ResponseEntity<BookingResponse> reject(@PathVariable String bookingNumber,
                                                  @RequestBody(required = false) BookingApprovalRequest request) {
        return ResponseEntity.ok(bookingService.reject(bookingNumber, request));
    }

    @PatchMapping("/{bookingNumber}/start")
    public ResponseEntity<BookingResponse> start(@PathVariable String bookingNumber,
                                                 @RequestBody(required = false) BookingStatusChangeRequest request) {
        return ResponseEntity.ok(bookingService.start(bookingNumber, request));
    }

    @PatchMapping("/{bookingNumber}/complete")
    public ResponseEntity<BookingResponse> complete(@PathVariable String bookingNumber,
                                                    @RequestBody(required = false) BookingStatusChangeRequest request) {
        return ResponseEntity.ok(bookingService.complete(bookingNumber, request));
    }

    @PatchMapping("/{bookingNumber}/cancel")
    public ResponseEntity<BookingResponse> cancel(@PathVariable String bookingNumber,
                                                  @RequestBody(required = false) BookingStatusChangeRequest request) {
        return ResponseEntity.ok(bookingService.cancel(bookingNumber, request));
    }

    @PatchMapping("/{bookingNumber}/no-show")
    public ResponseEntity<BookingResponse> noShow(@PathVariable String bookingNumber,
                                                  @RequestBody(required = false) BookingStatusChangeRequest request) {
        return ResponseEntity.ok(bookingService.noShow(bookingNumber, request));
    }

    @PatchMapping("/{bookingNumber}/early-check-in")
    public ResponseEntity<BookingResponse> earlyCheckIn(@PathVariable String bookingNumber,
                                                        @RequestBody(required = false) BookingCheckRequest request) {
        return ResponseEntity.ok(bookingService.earlyCheckIn(bookingNumber, request));
    }

    @PatchMapping("/{bookingNumber}/check-in")
    public ResponseEntity<BookingResponse> checkIn(@PathVariable String bookingNumber,
                                                   @RequestBody(required = false) BookingCheckRequest request) {
        return ResponseEntity.ok(bookingService.checkIn(bookingNumber, request));
    }

    @PatchMapping("/check-in/qr/{checkInToken}")
    public ResponseEntity<BookingResponse> checkInByToken(@PathVariable String checkInToken,
                                                          @RequestBody(required = false) BookingCheckRequest request) {
        return ResponseEntity.ok(bookingService.checkInByToken(checkInToken, request));
    }

    @PatchMapping("/{bookingNumber}/check-out")
    public ResponseEntity<BookingResponse> checkOut(@PathVariable String bookingNumber,
                                                    @RequestBody(required = false) BookingCheckRequest request) {
        return ResponseEntity.ok(bookingService.checkOut(bookingNumber, request));
    }

    @PatchMapping("/{bookingNumber}/transfer")
    public ResponseEntity<BookingResponse> transfer(@PathVariable String bookingNumber,
                                                    @Valid @RequestBody BookingTransferRequest request) {
        return ResponseEntity.ok(bookingService.transfer(bookingNumber, request));
    }

    /** Lot B2b : apercu d'un report (ancien et nouveau prix, ecart, modifications deja faites). */
    @org.springframework.web.bind.annotation.GetMapping("/{bookingNumber}/reschedule/preview")
    public ResponseEntity<com.sni.bokaticowork.features.booking.dto.response.BookingChangePreviewResponse> previewReschedule(
            @PathVariable String bookingNumber,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime newStartedAt,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime newEndedAt) {
        return ResponseEntity.ok(bookingService.previewReschedule(bookingNumber, newStartedAt, newEndedAt));
    }

    /** Lot B2b : apercu d'un changement de ressource. */
    @org.springframework.web.bind.annotation.GetMapping("/{bookingNumber}/change-resource/preview")
    public ResponseEntity<com.sni.bokaticowork.features.booking.dto.response.BookingChangePreviewResponse> previewChangeResource(
            @PathVariable String bookingNumber, @RequestParam String newResourceCode) {
        return ResponseEntity.ok(bookingService.previewChangeResource(bookingNumber, newResourceCode));
    }

    /**
     * Lot B4 : ajustement manuel du prix d'une reservation existante. Dans le plafond, applique ;
     * au-dela, demande d'approbation (la reponse porte {@code priceAdjustmentStatus: PENDING}).
     */
    @PostMapping("/{bookingNumber}/price-adjustment")
    public ResponseEntity<BookingResponse> adjustPrice(@PathVariable String bookingNumber,
            @Valid @RequestBody com.sni.bokaticowork.features.booking.dto.request.BookingPriceAdjustmentRequest request) {
        return ResponseEntity.ok(bookingService.adjustPrice(bookingNumber, request));
    }

    @PatchMapping("/{bookingNumber}/reschedule")
    public ResponseEntity<BookingResponse> reschedule(@PathVariable String bookingNumber,
                                                      @Valid @RequestBody BookingRescheduleRequest request) {
        return ResponseEntity.ok(bookingService.reschedule(bookingNumber, request));
    }

    @PatchMapping("/{bookingNumber}/change-resource")
    public ResponseEntity<BookingResponse> changeResource(@PathVariable String bookingNumber,
                                                          @Valid @RequestBody BookingChangeResourceRequest request) {
        return ResponseEntity.ok(bookingService.changeResource(bookingNumber, request));
    }

    @PostMapping("/{bookingNumber}/participants")
    public ResponseEntity<BookingParticipantResponse> addParticipant(@PathVariable String bookingNumber,
                                                                     @Valid @RequestBody BookingParticipantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.addParticipant(bookingNumber, request));
    }

    @DeleteMapping("/{bookingNumber}/participants/{participantId}")
    public ResponseEntity<BookingParticipantResponse> removeParticipant(@PathVariable String bookingNumber,
                                                                        @PathVariable Long participantId) {
        return ResponseEntity.ok(bookingService.removeParticipant(bookingNumber, participantId));
    }

    @GetMapping("/{bookingNumber}/history")
    public ResponseEntity<List<BookingStatusHistoryResponse>> history(@PathVariable String bookingNumber) {
        return ResponseEntity.ok(bookingService.history(bookingNumber));
    }

    @GetMapping("/{bookingNumber}/events")
    public ResponseEntity<List<BookingEventResponse>> events(@PathVariable String bookingNumber) {
        return ResponseEntity.ok(bookingService.events(bookingNumber));
    }

    @PostMapping("/policies")
    public ResponseEntity<BookingAudiencePolicyResponse> createPolicy(@Valid @RequestBody CreateBookingAudiencePolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.createPolicy(request));
    }

    @GetMapping("/policies")
    public ResponseEntity<List<BookingAudiencePolicyResponse>> listPolicies(@RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(policyService.listPolicies(active));
    }

    /** Lot B7 : remplace une politique (meme corps que la creation). */
    @org.springframework.web.bind.annotation.PutMapping("/policies/{policyNumber}")
    public ResponseEntity<BookingAudiencePolicyResponse> updatePolicy(@PathVariable String policyNumber,
                                                                      @Valid @RequestBody CreateBookingAudiencePolicyRequest request) {
        return ResponseEntity.ok(policyService.updatePolicy(policyNumber, request));
    }

    @PatchMapping("/policies/{policyNumber}/activate")
    public ResponseEntity<BookingAudiencePolicyResponse> activatePolicy(@PathVariable String policyNumber) {
        return ResponseEntity.ok(policyService.setPolicyActive(policyNumber, true));
    }

    @PatchMapping("/policies/{policyNumber}/deactivate")
    public ResponseEntity<BookingAudiencePolicyResponse> deactivatePolicy(@PathVariable String policyNumber) {
        return ResponseEntity.ok(policyService.setPolicyActive(policyNumber, false));
    }

    @PostMapping("/quota-overrides")
    public ResponseEntity<BookingQuotaOverrideResponse> createQuotaOverride(@Valid @RequestBody CreateBookingQuotaOverrideRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.createOverride(request));
    }

    @GetMapping("/quota-overrides")
    public ResponseEntity<List<BookingQuotaOverrideResponse>> listQuotaOverrides(@RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(policyService.listOverrides(active));
    }

    /** Lot B7 : modifie une derogation (le titulaire ne change pas). */
    @org.springframework.web.bind.annotation.PutMapping("/quota-overrides/{overrideNumber}")
    public ResponseEntity<BookingQuotaOverrideResponse> updateQuotaOverride(@PathVariable String overrideNumber,
            @Valid @RequestBody com.sni.bokaticowork.features.booking.dto.request.UpdateBookingQuotaOverrideRequest request) {
        return ResponseEntity.ok(policyService.updateOverride(overrideNumber, request));
    }

    @PatchMapping("/quota-overrides/{overrideNumber}/activate")
    public ResponseEntity<BookingQuotaOverrideResponse> activateQuotaOverride(@PathVariable String overrideNumber) {
        return ResponseEntity.ok(policyService.setOverrideActive(overrideNumber, true));
    }

    @PatchMapping("/quota-overrides/{overrideNumber}/deactivate")
    public ResponseEntity<BookingQuotaOverrideResponse> deactivateQuotaOverride(@PathVariable String overrideNumber) {
        return ResponseEntity.ok(policyService.setOverrideActive(overrideNumber, false));
    }

    @PostMapping("/{bookingNumber}/notifications/reminder")
    public ResponseEntity<Integer> queueReminder(@PathVariable String bookingNumber,
                                                 @RequestParam(defaultValue = "30") int minutesBefore) {
        return ResponseEntity.ok(notificationService.queueReminder(bookingNumber, minutesBefore));
    }

    @PostMapping("/notifications/dispatch-due")
    public ResponseEntity<Integer> dispatchDueNotifications(@RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(notificationService.dispatchDue(limit));
    }

    @GetMapping("/metrics/overview")
    public ResponseEntity<BookingMetricsOverviewResponse> metrics() {
        return ResponseEntity.ok(metricsService.overview());
    }

    @PostMapping("/repair")
    public ResponseEntity<BookingRepairResponse> repair(@RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(repairService.repair(limit));
    }
}
