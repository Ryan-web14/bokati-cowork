package com.sni.bokaticowork.features.portal.resource.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceTypeResponse;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPath.V1 + "/client/resource-types")
@RequiredArgsConstructor
public class ClientResourceTypeController {

    private final ResourceTypeService resourceTypeService;

    @GetMapping
    public ResponseEntity<List<ResourceTypeResponse>> list() {
        return ResponseEntity.ok(resourceTypeService.listActive());
    }

    @GetMapping("/search")
    public ResponseEntity<List<ResourceTypeResponse>> search(@RequestParam String query) {
        return ResponseEntity.ok(resourceTypeService.searchActive(query));
    }


}
