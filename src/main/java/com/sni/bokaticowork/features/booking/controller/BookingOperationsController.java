package com.sni.bokaticowork.features.booking.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.service.support.BookingOperations;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Lot R5d : reglement d'usage et etat des lieux d'une reservation (equipe). */
@RestController
@RequestMapping(ApiPath.V1 + "/bookings/{number}")
@RequiredArgsConstructor
public class BookingOperationsController {

    private final BookingOperations operations;

    @GetMapping("/usage-rules")
    public ResponseEntity<BookingOperations.UsageRulesView> usageRules(@PathVariable String number) {
        return ResponseEntity.ok(operations.usageRules(operations.booking(number)));
    }

    /** Acceptation recueillie par l'equipe (a l'accueil, au nom du client). */
    @PostMapping("/usage-rules/accept")
    @Audited(module = "BOOKING", action = "ACCEPT_USAGE_RULES", ressource = "booking")
    public ResponseEntity<BookingOperations.UsageRulesView> accept(@PathVariable String number, Authentication authentication) {
        return ResponseEntity.ok(operations.acceptUsageRules(operations.booking(number),
                authentication == null ? null : authentication.getName()));
    }

    @GetMapping("/end-report")
    public ResponseEntity<List<BookingOperations.EndReportResponse>> endReports(@PathVariable String number) {
        return ResponseEntity.ok(operations.endReports(operations.booking(number)));
    }

    @PostMapping("/end-report")
    @Audited(module = "BOOKING", action = "END_REPORT", ressource = "booking")
    public ResponseEntity<BookingOperations.EndReportResponse> endReport(@PathVariable String number,
                                                                         @RequestBody BookingOperations.EndReportRequest request,
                                                                         Authentication authentication) {
        String actor = authentication == null ? null : authentication.getName();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(operations.endReport(operations.booking(number), request, "TEAM", actor, actor, null, null));
    }
}
