package com.sni.bokaticowork.features.support.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.support.dto.SupportDtos.QuickReplyRequest;
import com.sni.bokaticowork.features.support.dto.SupportDtos.QuickReplyResponse;
import com.sni.bokaticowork.features.support.enums.TicketCategory;
import com.sni.bokaticowork.features.support.service.implementation.SupportQuickReplyServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/support/quick-replies")
public class SupportQuickReplyController {

    private final SupportQuickReplyServiceImpl service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPPORT:READ','SUPPORT_READ')")
    public ResponseEntity<List<QuickReplyResponse>> list(
            @RequestParam(required = false) TicketCategory category) {
        return ResponseEntity.ok(service.list(category));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')")
    public ResponseEntity<QuickReplyResponse> create(@Valid @RequestBody QuickReplyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
