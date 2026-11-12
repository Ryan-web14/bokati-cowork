package com.sni.bokaticowork.features.marketplace.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.marketplace.dto.MarketplaceDtos;
import com.sni.bokaticowork.features.marketplace.model.MarketplaceOrder;
import com.sni.bokaticowork.features.marketplace.service.MarketplaceCatalogService;
import com.sni.bokaticowork.features.marketplace.service.MarketplaceOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * La marketplace cote administration · le catalogue, les commandes, leur remise et leur annulation.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/marketplace")
public class MarketplaceAdminController {

    private static final String CATALOG_ROLES = "hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')";
    private static final String DESK_ROLES = CATALOG_ROLES + " or hasRole('OPERATIONS_AGENT')";

    private final MarketplaceCatalogService catalog;
    private final MarketplaceOrderService orders;

    @GetMapping("/offers")
    @PreAuthorize(DESK_ROLES)
    public ResponseEntity<List<MarketplaceDtos.OfferView>> offers(@RequestParam(required = false) Boolean published,
                                                                  @RequestParam(required = false) String category) {
        return ResponseEntity.ok(catalog.list(published, category));
    }

    @GetMapping("/offers/{id}")
    @PreAuthorize(DESK_ROLES)
    public ResponseEntity<MarketplaceDtos.OfferView> offer(@PathVariable Long id) {
        return ResponseEntity.ok(catalog.get(id));
    }

    @PostMapping("/offers")
    @PreAuthorize(CATALOG_ROLES)
    public ResponseEntity<MarketplaceDtos.OfferView> create(@Valid @RequestBody MarketplaceDtos.OfferRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalog.create(request, actor(authentication)));
    }

    @PatchMapping("/offers/{id}")
    @PreAuthorize(CATALOG_ROLES)
    public ResponseEntity<MarketplaceDtos.OfferView> update(@PathVariable Long id,
                                                            @Valid @RequestBody MarketplaceDtos.OfferUpdateRequest request) {
        return ResponseEntity.ok(catalog.update(id, request));
    }

    @PostMapping("/offers/{id}/publish")
    @PreAuthorize(CATALOG_ROLES)
    public ResponseEntity<MarketplaceDtos.OfferView> publish(@PathVariable Long id) {
        return ResponseEntity.ok(catalog.publish(id, true));
    }

    @PostMapping("/offers/{id}/unpublish")
    @PreAuthorize(CATALOG_ROLES)
    public ResponseEntity<MarketplaceDtos.OfferView> unpublish(@PathVariable Long id) {
        return ResponseEntity.ok(catalog.publish(id, false));
    }

    @GetMapping("/orders")
    @PreAuthorize(DESK_ROLES)
    public ResponseEntity<List<MarketplaceDtos.OrderView>> orders(@RequestParam(required = false) MarketplaceOrder.Status status,
                                                                  @RequestParam(defaultValue = "false") boolean stockPending,
                                                                  @RequestParam(defaultValue = "100") int limit) {
        int bounded = Math.min(Math.max(limit, 1), 500);
        return ResponseEntity.ok(stockPending ? orders.stockPending(bounded) : orders.search(status, bounded));
    }

    @GetMapping("/orders/{orderNumber}")
    @PreAuthorize(DESK_ROLES)
    public ResponseEntity<MarketplaceDtos.OrderView> order(@PathVariable String orderNumber) {
        return ResponseEntity.ok(orders.get(orderNumber));
    }

    /** Remise au comptoir d'une commande reglee. */
    @PostMapping("/orders/{orderNumber}/fulfil")
    @PreAuthorize(DESK_ROLES)
    public ResponseEntity<MarketplaceDtos.OrderView> fulfil(@PathVariable String orderNumber,
                                                            @Valid @RequestBody(required = false) MarketplaceDtos.FulfilRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.ok(orders.fulfil(orderNumber, request, actor(authentication)));
    }

    @PostMapping("/orders/{orderNumber}/cancel")
    @PreAuthorize(CATALOG_ROLES)
    public ResponseEntity<MarketplaceDtos.OrderView> cancel(@PathVariable String orderNumber,
                                                            @Valid @RequestBody MarketplaceDtos.CancelRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.ok(orders.cancelByStaff(orderNumber, request, actor(authentication)));
    }

    private static String actor(Authentication authentication) {
        return authentication == null ? "SYSTEM" : authentication.getName();
    }
}
