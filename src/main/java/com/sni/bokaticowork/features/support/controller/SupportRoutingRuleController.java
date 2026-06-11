package com.sni.bokaticowork.features.support.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.support.dto.SupportDtos.RoutingRuleRequest;
import com.sni.bokaticowork.features.support.dto.SupportDtos.RoutingRuleResponse;
import com.sni.bokaticowork.features.support.service.implementation.SupportRoutingRuleServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/support/routing-rules")
public class SupportRoutingRuleController {

    private final SupportRoutingRuleServiceImpl service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPPORT:READ','SUPPORT_READ')")
    public ResponseEntity<List<RoutingRuleResponse>> list() {
        return ResponseEntity.ok(service.list());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')")
    public ResponseEntity<RoutingRuleResponse> create(@Valid @RequestBody RoutingRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')")
    public ResponseEntity<RoutingRuleResponse> update(@PathVariable Long id, @Valid @RequestBody RoutingRuleRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
