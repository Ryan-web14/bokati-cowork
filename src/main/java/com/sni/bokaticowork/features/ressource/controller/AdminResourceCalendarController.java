package com.sni.bokaticowork.features.ressource.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.ressource.dto.response.AdminResourceCalendarResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceOccupiedSlotResponse;
import com.sni.bokaticowork.features.ressource.service.interfaces.AdminResourceCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/resources")
public class AdminResourceCalendarController {

    private final AdminResourceCalendarService calendarService;

    @GetMapping("/{resourceCode}/calendar/admin")
    public ResponseEntity<AdminResourceCalendarResponse> calendar(
            @PathVariable String resourceCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(calendarService.calendar(resourceCode, fromDate, toDate));
    }

    @GetMapping("/occupied-slots")
    public ResponseEntity<List<ResourceOccupiedSlotResponse>> occupiedSlots(
            @RequestParam String resourceCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        LocalDate fromDate = from != null ? from.toLocalDate() : null;
        LocalDate toDate = to != null ? to.toLocalDate() : null;
        return ResponseEntity.ok(calendarService.occupiedSlots(resourceCode, fromDate, toDate));
    }
}
