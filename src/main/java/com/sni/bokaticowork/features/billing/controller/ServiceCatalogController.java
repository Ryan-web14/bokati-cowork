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
import com.sni.bokaticowork.features.billing.service.implementation.CatalogPriceService;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPath.V1 + "/billing/catalog")
@RequiredArgsConstructor
public class ServiceCatalogController {

    private final com.sni.bokaticowork.features.billing.service.implementation.CatalogPriceService catalogPrices;

    private final ServiceCatalogService serviceCatalogService;

    @PostMapping
    public ResponseEntity<ServiceCatalogItemResponse> create(@Valid @RequestBody CreateServiceCatalogItemRequest request) {
        return ResponseEntity.ok(serviceCatalogService.create(request));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<ServiceCatalogItemResponse>> list(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subcategory,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "displayOrder", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(new PaginatedResponse<>(serviceCatalogService.list(active, category, subcategory, q, pageable)));
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

    /** Lot B10 : nouveau prix date (segment de client) ; le precedent s'arrete la veille, l'historique reste. */
    @PostMapping("/{itemCode}/prices")
    public ResponseEntity<com.sni.bokaticowork.features.billing.dto.response.CatalogPriceResponse> setPrice(@PathVariable String itemCode,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.sni.bokaticowork.features.billing.dto.request.CatalogPriceRequest request) {
        return ResponseEntity.ok(catalogPrices.setPrice(itemCode, request));
    }

    @GetMapping("/{itemCode}/prices")
    public ResponseEntity<java.util.List<com.sni.bokaticowork.features.billing.dto.response.CatalogPriceResponse>> priceHistory(@PathVariable String itemCode) {
        return ResponseEntity.ok(catalogPrices.history(itemCode));
    }

    /** Lot B10 : prix applicable a un segment de client a une date. */
    @GetMapping("/{itemCode}/price")
    public ResponseEntity<com.sni.bokaticowork.features.billing.dto.response.CatalogPriceResponse> price(@PathVariable String itemCode,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String segment,
            @org.springframework.web.bind.annotation.RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        return ResponseEntity.ok(catalogPrices.price(itemCode, segment, date));
    }

    /** Lot B10 : lien vers l'article d'inventaire et compte de produit propre. */
    @PutMapping("/{itemCode}/links")
    public ResponseEntity<com.sni.bokaticowork.features.billing.dto.response.CatalogMarginResponse> links(@PathVariable String itemCode,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.sni.bokaticowork.features.billing.dto.request.CatalogLinksRequest request) {
        return ResponseEntity.ok(catalogPrices.links(itemCode, request));
    }

    /** Lot B10 : recherche avec apercu de la marge. */
    @GetMapping("/margins")
    public ResponseEntity<java.util.List<com.sni.bokaticowork.features.billing.dto.response.CatalogMarginResponse>> margins(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String search,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String category) {
        return ResponseEntity.ok(catalogPrices.margins(search, category));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv() {
        return ResponseEntity.ok().header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=catalogue.csv")
                .contentType(new org.springframework.http.MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(("\uFEFF" + catalogPrices.exportCsv()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** Lot B10 : import CSV (en-tete de l'export, separateur point-virgule). */
    @PostMapping(value = "/import", consumes = {"text/csv", "text/plain"})
    public ResponseEntity<CatalogPriceService.ImportResult> importCsv(@org.springframework.web.bind.annotation.RequestBody String content) {
        return ResponseEntity.ok(catalogPrices.importCsv(content));
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
