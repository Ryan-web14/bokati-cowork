package com.sni.bokaticowork.features.billing.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.billing.dto.request.CreateServiceCatalogItemRequest;
import com.sni.bokaticowork.features.billing.dto.request.UpdateServiceCatalogItemRequest;
import com.sni.bokaticowork.features.billing.dto.response.CatalogLookupItemResponse;
import com.sni.bokaticowork.features.billing.dto.response.ServiceCatalogItemResponse;
import com.sni.bokaticowork.features.billing.service.interfaces.ServiceCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPath.V1 + "/billing/catalog")
@RequiredArgsConstructor
public class ServiceCatalogController {

    private final ServiceCatalogService serviceCatalogService;

    @PostMapping
    public ResponseEntity<ServiceCatalogItemResponse> create(@Valid @RequestBody CreateServiceCatalogItemRequest request) {
        return ResponseEntity.ok(serviceCatalogService.create(request));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<ServiceCatalogItemResponse>> list(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "displayOrder", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(new PaginatedResponse<>(serviceCatalogService.list(active, category, q, pageable)));
    }

    @PutMapping("/{itemCode}")
    public ResponseEntity<ServiceCatalogItemResponse> update(
            @PathVariable String itemCode,
            @Valid @RequestBody UpdateServiceCatalogItemRequest request
    ) {
        return ResponseEntity.ok(serviceCatalogService.update(itemCode, request));
    }

    @PatchMapping("/{itemCode}/activate")
    public ResponseEntity<ServiceCatalogItemResponse> activate(@PathVariable String itemCode) {
        return ResponseEntity.ok(serviceCatalogService.activate(itemCode));
    }

    @PatchMapping("/{itemCode}/deactivate")
    public ResponseEntity<ServiceCatalogItemResponse> deactivate(@PathVariable String itemCode) {
        return ResponseEntity.ok(serviceCatalogService.deactivate(itemCode));
    }

    @DeleteMapping("/{itemCode}")
    public ResponseEntity<Void> delete(@PathVariable String itemCode) {
        serviceCatalogService.delete(itemCode);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/lookup")
    public ResponseEntity<List<CatalogLookupItemResponse>> lookup(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<String> sources,
            @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(serviceCatalogService.lookup(q, sources, category));
    }
}
