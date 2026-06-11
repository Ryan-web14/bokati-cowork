package com.sni.bokaticowork.features.support.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.support.dto.SupportDtos.TagResponse;
import com.sni.bokaticowork.features.support.service.interfaces.SupportTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/support/tags")
public class SupportTagController {

    private final SupportTicketService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPPORT:READ','SUPPORT_READ')")
    public ResponseEntity<List<TagResponse>> list() {
        return ResponseEntity.ok(service.listTags());
    }
}
