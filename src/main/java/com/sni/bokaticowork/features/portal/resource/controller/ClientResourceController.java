package com.sni.bokaticowork.features.portal.resource.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.portal.resource.dto.response.ClientResourceDetailResponse;
import com.sni.bokaticowork.features.portal.resource.dto.response.ClientResourceSummaryResponse;
import com.sni.bokaticowork.features.portal.resource.service.ClientResourceService;
import com.sni.bokaticowork.features.ressource.dto.response.PublicResourceCalendarResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceAvailabilityWindowResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourcePriceQuoteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(ApiPath.V1 + "/client/resources")
@RequiredArgsConstructor
public class ClientResourceController {

    private final ClientResourceService clientResourceService;

    @GetMapping
    public ResponseEntity<PaginatedResponse<ClientResourceSummaryResponse>> listResources(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer capacity,
            @PageableDefault(size = 20, sort = "displayOrder", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(clientResourceService.listPortalResources(type, group, q, capacity, pageable));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ClientResourceDetailResponse> getResource(@PathVariable String code) {
        return ResponseEntity.ok(clientResourceService.getPortalResource(code));
    }

    @GetMapping("/{code}/calendar")
    public ResponseEntity<PublicResourceCalendarResponse> getCalendar(
            @PathVariable String code,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(clientResourceService.getCalendar(code, from, to));
    }

    @GetMapping("/{code}/slots")
    public ResponseEntity<List<ResourceAvailabilityWindowResponse>> getAvailableSlots(
            @PathVariable String code,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startedAt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endedAt,
            @RequestParam(required = false, defaultValue = "30") Integer durationMinutes,
            @RequestParam(required = false, defaultValue = "1") Integer quantity) {
        return ResponseEntity.ok(clientResourceService.getAvailableSlots(code, startedAt, endedAt, durationMinutes, quantity));
    }

    @GetMapping("/{code}/quote")
    public ResponseEntity<ResourcePriceQuoteResponse> getQuote(
            @PathVariable String code,
            @RequestParam String bookingUnit,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startedAt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endedAt) {
        return ResponseEntity.ok(clientResourceService.getQuote(code, bookingUnit, startedAt, endedAt));
    }
}
