package com.sni.bokaticowork.features.ressource.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.ressource.dto.response.PublicResourceCalendarResponse;
import com.sni.bokaticowork.features.ressource.service.interfaces.PublicResourceCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/public/resources")
public class PublicResourceCalendarController {

    private final PublicResourceCalendarService service;

    @GetMapping("/{resourceCode}/calendar")
    public ResponseEntity<PublicResourceCalendarResponse> calendar(
            @PathVariable String resourceCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(service.calendar(resourceCode, fromDate, toDate));
    }
}
