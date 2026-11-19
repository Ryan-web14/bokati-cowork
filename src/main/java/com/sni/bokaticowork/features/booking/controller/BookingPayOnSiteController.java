package com.sni.bokaticowork.features.booking.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.dto.response.BookingResponse;
import com.sni.bokaticowork.features.booking.mapper.interfaces.BookingMapper;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.support.BookingPayOnSite;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Lot B3 : rendez-vous regles sur place (option par client, file de l'accueil). */
@RestController
@RequestMapping(ApiPath.V1 + "/bookings/pay-on-site")
@RequiredArgsConstructor
public class BookingPayOnSiteController {

    private final BookingPayOnSite payOnSite;
    private final BookingRepository bookings;
    private final BookingMapper mapper;

    public record GrantRequest(@NotNull Boolean enabled, @Size(max = 500) String reason) {
    }

    /** Options posees par l'equipe (clients ouverts ou fermes), la plus recente d'abord. */
    @GetMapping("/clients")
    public ResponseEntity<List<BookingPayOnSite.Grant>> grants() {
        return ResponseEntity.ok(payOnSite.list());
    }

    /** Ce que le portail permet a ce client, et pourquoi (option du client ou reglage global). */
    @GetMapping("/clients/{ownerType}/{ownerCode}")
    public ResponseEntity<BookingPayOnSite.Access> access(@PathVariable SubscriberType ownerType, @PathVariable String ownerCode) {
        return ResponseEntity.ok(payOnSite.access(ownerType, ownerCode));
    }

    /** Ouvre ou ferme le rendez-vous regle sur place a un client ; l'emporte sur le reglage global. */
    @PutMapping("/clients/{ownerType}/{ownerCode}")
    public ResponseEntity<BookingPayOnSite.Grant> set(@PathVariable SubscriberType ownerType, @PathVariable String ownerCode,
                                                      @Valid @RequestBody GrantRequest request) {
        return ResponseEntity.ok(payOnSite.set(ownerType, ownerCode, request.enabled(), request.reason()));
    }

    /** File de l'accueil : rendez-vous regles sur place encore a encaisser, qui commencent ce jour-la (aujourd'hui par defaut). */
    @GetMapping("/due")
    @Transactional(readOnly = true)
    public ResponseEntity<List<BookingResponse>> due(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate day = date == null ? com.sni.bokaticowork.core.utils.time.AppTime.today() : date;
        return ResponseEntity.ok(mapper.toListResponses(bookings.findPayOnSiteToCollect(day.atStartOfDay(), day.plusDays(1).atStartOfDay())));
    }
}
