package com.sni.bokaticowork.features.ressource.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.ressource.dto.request.ResourceCalendarDayRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceCalendarDayResponse;
import com.sni.bokaticowork.features.ressource.service.support.ResourceCalendarDays;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Lot R5a : jours feries, horaires exceptionnels et jours fermes. */
@RestController
@RequestMapping(ApiPath.V1 + "/resource-calendar-days")
@RequiredArgsConstructor
public class ResourceCalendarDayController {

    private final ResourceCalendarDays calendarDays;

    /** Jours particuliers d'une plage (feries nationaux compris), generaux et, si demande, d'une ressource. */
    @GetMapping
    public ResponseEntity<List<ResourceCalendarDayResponse>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String resourceCode) {
        return ResponseEntity.ok(calendarDays.list(from, to, resourceCode));
    }

    @PostMapping
    @Audited(module = "RESOURCE", action = "CREATE_CALENDAR_DAY", ressource = "resource_calendar_day")
    public ResponseEntity<ResourceCalendarDayResponse> create(@RequestBody ResourceCalendarDayRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(calendarDays.create(request, authentication == null ? null : authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @Audited(module = "RESOURCE", action = "DELETE_CALENDAR_DAY", ressource = "resource_calendar_day")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        calendarDays.delete(id);
        return ResponseEntity.noContent().build();
    }
}
