package com.sni.bokaticowork.features.booking.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.dto.request.BookingAvailabilityRequest;
import com.sni.bokaticowork.features.booking.dto.request.BookingApprovalRequest;
import com.sni.bokaticowork.features.booking.dto.request.BookingCheckRequest;
import com.sni.bokaticowork.features.booking.dto.request.BookingParticipantRequest;
import com.sni.bokaticowork.features.booking.dto.request.BookingStatusChangeRequest;
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
            @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(bookingService.list(query, ownerType, ownerCode, resourceCode, status, startedFrom, startedTo, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<List<BookingResponse>> basicSearch(@RequestParam String query) {
        return ResponseEntity.ok(bookingService.basicSearch(query));
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

    @PatchMapping("/{bookingNumber}/check-in")
    public ResponseEntity<BookingResponse> checkIn(@PathVariable String bookingNumber,
                                                   @RequestBody(required = false) BookingCheckRequest request) {
        return ResponseEntity.ok(bookingService.checkIn(bookingNumber, request));
    }

    @PatchMapping("/{bookingNumber}/check-out")
    public ResponseEntity<BookingResponse> checkOut(@PathVariable String bookingNumber,
                                                    @RequestBody(required = false) BookingCheckRequest request) {
        return ResponseEntity.ok(bookingService.checkOut(bookingNumber, request));
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
    public ResponseEntity<List<BookingAudiencePolicyResponse>> listPolicies() {
        return ResponseEntity.ok(policyService.listPolicies());
    }

    @PostMapping("/quota-overrides")
    public ResponseEntity<BookingQuotaOverrideResponse> createQuotaOverride(@Valid @RequestBody CreateBookingQuotaOverrideRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.createOverride(request));
    }

    @GetMapping("/quota-overrides")
    public ResponseEntity<List<BookingQuotaOverrideResponse>> listQuotaOverrides() {
        return ResponseEntity.ok(policyService.listOverrides());
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
