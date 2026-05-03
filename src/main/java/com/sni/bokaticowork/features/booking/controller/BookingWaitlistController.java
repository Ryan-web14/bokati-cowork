package com.sni.bokaticowork.features.booking.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.dto.request.JoinBookingWaitlistRequest;
import com.sni.bokaticowork.features.booking.dto.response.BookingWaitlistEntryResponse;
import com.sni.bokaticowork.features.booking.enums.BookingWaitlistStatus;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingWaitlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/bookings/waitlist")
public class BookingWaitlistController {

    private final BookingWaitlistService service;

    @PostMapping
    public ResponseEntity<BookingWaitlistEntryResponse> join(@Valid @RequestBody JoinBookingWaitlistRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.join(request));
    }

    @GetMapping
    public ResponseEntity<List<BookingWaitlistEntryResponse>> list(@RequestParam(required = false) BookingWaitlistStatus status) {
        return ResponseEntity.ok(service.list(status));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BookingWaitlistEntryResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancel(id));
    }

    @PostMapping("/promote-available")
    public ResponseEntity<Integer> promoteAvailable(@RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(service.promoteAvailable(limit));
    }

    @PostMapping("/expire-offers")
    public ResponseEntity<Integer> expireOffers(@RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(service.expireOffers(limit));
    }
}
