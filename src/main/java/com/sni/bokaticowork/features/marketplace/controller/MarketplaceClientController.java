package com.sni.bokaticowork.features.marketplace.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.company.membership.service.CompanyMembershipService;
import com.sni.bokaticowork.features.marketplace.dto.MarketplaceDtos;
import com.sni.bokaticowork.features.marketplace.service.MarketplaceCatalogService;
import com.sni.bokaticowork.features.marketplace.service.MarketplaceOrderService;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * La marketplace dans le portail · la vitrine, le panier, les commandes du membre, et celles que
 * l'organisation paie pour son gerant.
 */
@RestController
@RequiredArgsConstructor
public class MarketplaceClientController {

    private static final String BASE = ApiPath.V1 + "/client/marketplace";

    private final MarketplaceCatalogService catalog;
    private final MarketplaceOrderService orders;
    private final ClientContextService clientContextService;
    private final CompanyMembershipService membershipService;

    @GetMapping(BASE + "/offers")
    public ResponseEntity<List<MarketplaceDtos.OfferView>> offers(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(catalog.storefront(category));
    }

    @GetMapping(BASE + "/cart")
    public ResponseEntity<MarketplaceDtos.OrderView> cart() {
        return ResponseEntity.ok(orders.cart(clientContextService.getAuthenticatedMember()));
    }

    @PostMapping(BASE + "/cart/lines")
    public ResponseEntity<MarketplaceDtos.OrderView> addLine(@Valid @RequestBody MarketplaceDtos.CartLineRequest request) {
        return ResponseEntity.ok(orders.addToCart(clientContextService.getAuthenticatedMember(), request));
    }

    @PatchMapping(BASE + "/cart/lines/{lineId}")
    public ResponseEntity<MarketplaceDtos.OrderView> updateLine(@PathVariable Long lineId,
                                                                @Valid @RequestBody MarketplaceDtos.CartQuantityRequest request) {
        return ResponseEntity.ok(orders.updateCartLine(clientContextService.getAuthenticatedMember(), lineId, request.quantity()));
    }

    @DeleteMapping(BASE + "/cart/lines/{lineId}")
    public ResponseEntity<MarketplaceDtos.OrderView> removeLine(@PathVariable Long lineId) {
        return ResponseEntity.ok(orders.removeCartLine(clientContextService.getAuthenticatedMember(), lineId));
    }

    @PostMapping(BASE + "/cart/checkout")
    public ResponseEntity<MarketplaceDtos.OrderView> checkout(@RequestBody(required = false) MarketplaceDtos.CheckoutRequest request) {
        return ResponseEntity.ok(orders.checkout(clientContextService.getAuthenticatedMember(), request));
    }

    @GetMapping(BASE + "/orders")
    public ResponseEntity<List<MarketplaceDtos.OrderView>> myOrders(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(orders.ordersOf(clientContextService.getAuthenticatedMember(), Math.min(Math.max(limit, 1), 200)));
    }

    @GetMapping(BASE + "/orders/{orderNumber}")
    public ResponseEntity<MarketplaceDtos.OrderView> myOrder(@PathVariable String orderNumber) {
        return ResponseEntity.ok(orders.orderOf(clientContextService.getAuthenticatedMember(), orderNumber));
    }

    @PostMapping(BASE + "/orders/{orderNumber}/cancel")
    public ResponseEntity<MarketplaceDtos.OrderView> cancel(@PathVariable String orderNumber,
                                                            @Valid @RequestBody(required = false) MarketplaceDtos.MemberCancelRequest request) {
        return ResponseEntity.ok(orders.cancelByMember(clientContextService.getAuthenticatedMember(), orderNumber,
                request == null ? null : request.reason()));
    }

    /** Les commandes que l'organisation paie · pour son gerant. */
    @GetMapping(ApiPath.V1 + "/client/companies/{customerId}/marketplace/orders")
    public ResponseEntity<List<MarketplaceDtos.OrderView>> companyOrders(@PathVariable String customerId,
                                                                         @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(orders.ordersOfCompany(
                membershipService.ownedCompany(clientContextService.getAuthenticatedMember(), customerId),
                Math.min(Math.max(limit, 1), 500)));
    }
}
