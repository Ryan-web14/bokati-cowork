package com.sni.bokaticowork.features.ressource.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.ressource.service.support.ResourceIcal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lot R5b : flux d'abonnement iCalendar d'une ressource, par jeton. Coupe par defaut
 * ({@code app.resources.ical.public-feed-enabled}) ; ne montre que « Occupé » et « Fermé ».
 */
@RestController
@RequestMapping(ApiPath.V1 + "/public/resource-calendars")
@RequiredArgsConstructor
public class PublicResourceIcalController {

    private final ResourceIcal ical;

    @GetMapping("/{token}.ics")
    public ResponseEntity<String> feed(@PathVariable String token) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"))
                .body(ical.publicFeed(token));
    }
}
