package com.sni.bokaticowork.features.booking.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.service.support.ResourceAnalytics;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Lot R5e : couts, marges, historique des prix, occupation et chiffre d'affaires des ressources. */
@RestController
@RequiredArgsConstructor
public class ResourceAnalyticsController {

    private final ResourceAnalytics analytics;

    @GetMapping(ApiPath.V1 + "/resources/{code}/costs")
    public ResponseEntity<List<ResourceAnalytics.CostResponse>> costs(@PathVariable String code) {
        return ResponseEntity.ok(analytics.costs(code));
    }

    @PostMapping(ApiPath.V1 + "/resources/{code}/costs")
    @Audited(module = "RESOURCE", action = "ADD_COST", ressource = "resource_cost")
    public ResponseEntity<ResourceAnalytics.CostResponse> addCost(@PathVariable String code,
                                                                  @RequestBody ResourceAnalytics.CostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(analytics.addCost(code, request));
    }

    @DeleteMapping(ApiPath.V1 + "/resource-costs/{id}")
    @Audited(module = "RESOURCE", action = "DELETE_COST", ressource = "resource_cost")
    public ResponseEntity<Void> deleteCost(@PathVariable Long id) {
        analytics.deleteCost(id);
        return ResponseEntity.noContent().build();
    }

    /** Historique des prix appliques aux reservations de la ressource (les versions des regles : /resource-pricing-rules/{id}/history). */
    @GetMapping(ApiPath.V1 + "/resources/{code}/price-history")
    public ResponseEntity<List<ResourceAnalytics.AppliedPrice>> priceHistory(
            @PathVariable String code,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analytics.priceHistory(code, from, to));
    }

    /** Occupation et chiffre d'affaires, par ressource, type, groupe ou site ; heures pleines et creuses. */
    @GetMapping(ApiPath.V1 + "/resource-analytics/occupancy")
    public ResponseEntity<ResourceAnalytics.OccupancyReport> occupancy(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) String siteCode) {
        return ResponseEntity.ok(analytics.occupancy(from, to, groupBy, siteCode));
    }

    /** Marges par ressource sur la periode (chiffre d'affaires moins couts). */
    @GetMapping(ApiPath.V1 + "/resource-analytics/margins")
    public ResponseEntity<ResourceAnalytics.MarginReport> margins(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String resourceCode) {
        return ResponseEntity.ok(analytics.margins(from, to, resourceCode));
    }

    /** Marge d'une reservation. */
    @GetMapping(ApiPath.V1 + "/resource-analytics/bookings/{number}/margin")
    public ResponseEntity<ResourceAnalytics.BookingMargin> bookingMargin(@PathVariable String number) {
        return ResponseEntity.ok(analytics.bookingMargin(number));
    }
}
