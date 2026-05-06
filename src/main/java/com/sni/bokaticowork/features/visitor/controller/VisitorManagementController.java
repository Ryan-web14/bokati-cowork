package com.sni.bokaticowork.features.visitor.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.visitor.dto.VisitorDtos.*;
import com.sni.bokaticowork.features.visitor.enums.VisitorPassStatus;
import com.sni.bokaticowork.features.visitor.service.interfaces.VisitorManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/visitors")
public class VisitorManagementController {
    private final VisitorManagementService service;

    @PostMapping("/passes")
    @PreAuthorize("hasAnyAuthority('VISITOR:WRITE','VISITOR_WRITE')")
    public ResponseEntity<VisitorPassResponse> create(@Valid @RequestBody CreateVisitorPassRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createPass(request));
    }

    @GetMapping("/passes")
    @PreAuthorize("hasAnyAuthority('VISITOR:READ','VISITOR_READ')")
    public ResponseEntity<List<VisitorPassResponse>> list(@RequestParam(required = false) VisitorPassStatus status) {
        return ResponseEntity.ok(service.list(status));
    }

    @GetMapping("/passes/today")
    @PreAuthorize("hasAnyAuthority('VISITOR:READ','VISITOR_READ')")
    public ResponseEntity<List<VisitorPassResponse>> today() {
        return ResponseEntity.ok(service.today());
    }

    @PostMapping("/{passNumber}/check-in")
    @PreAuthorize("hasAnyAuthority('VISITOR:CHECKIN','VISITOR_CHECKIN')")
    public ResponseEntity<VisitorPassResponse> checkIn(@PathVariable String passNumber,
                                                       @RequestBody CheckInRequest request) {
        return ResponseEntity.ok(service.checkIn(passNumber, request));
    }

    @PostMapping("/{passNumber}/check-out")
    @PreAuthorize("hasAnyAuthority('VISITOR:CHECKIN','VISITOR_CHECKIN')")
    public ResponseEntity<VisitorPassResponse> checkOut(@PathVariable String passNumber,
                                                        @RequestBody CheckInRequest request) {
        return ResponseEntity.ok(service.checkOut(passNumber, request));
    }

    @GetMapping("/log")
    @PreAuthorize("hasAnyAuthority('VISITOR:READ','VISITOR_READ')")
    public ResponseEntity<List<VisitorLogResponse>> log() {
        return ResponseEntity.ok(service.log());
    }

    @GetMapping("/passes/{passNumber}/badge")
    @PreAuthorize("hasAnyAuthority('VISITOR:READ','VISITOR_READ')")
    public ResponseEntity<byte[]> badge(@PathVariable String passNumber) {
        byte[] pdf = service.generateBadge(passNumber);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("badge-" + passNumber + ".pdf").build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
