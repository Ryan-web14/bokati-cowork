package com.sni.bokaticowork.features.booking.controller;

import com.sni.bokaticowork.core.reference.dto.ReferenceDtos.EnumGroup;
import com.sni.bokaticowork.core.reference.dto.ReferenceDtos.ListSummary;
import com.sni.bokaticowork.core.reference.dto.ReferenceDtos.Value;
import com.sni.bokaticowork.core.reference.lookup.LookupItem;
import com.sni.bokaticowork.core.reference.service.ReferenceEnumRegistry;
import com.sni.bokaticowork.core.reference.service.ReferenceLists;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.dto.response.CancellationPolicyResponse;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import com.sni.bokaticowork.features.booking.service.support.BookingOwnerLookup;
import com.sni.bokaticowork.features.booking.service.support.BookingReferenceEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lot B7 : tout ce qu'un ecran de reservation charge pour ses listes deroulantes, en un appel, et
 * la recherche unique du titulaire.
 */
@RestController
@RequestMapping(ApiPath.V1 + "/bookings")
@RequiredArgsConstructor
public class BookingReferenceController {

    private final ReferenceEnumRegistry enums;
    private final ReferenceLists lists;
    private final BookingService bookingService;
    private final BookingOwnerLookup ownerLookup;

    /** Valeurs fixes du module, listes de l'administrateur (motifs) et grille d'annulation generale. */
    public record BookingReference(List<EnumGroup> enums, Map<String, List<Value>> lists, List<CancellationPolicyResponse> cancellationPolicy) {
    }

    @GetMapping("/reference")
    public ResponseEntity<BookingReference> reference() {
        Map<String, List<Value>> values = new LinkedHashMap<>();
        for (ListSummary list : lists.lists(BookingReferenceEnums.MODULE)) {
            values.put(list.code(), lists.values(list.code(), false));
        }
        return ResponseEntity.ok(new BookingReference(enums.groups(BookingReferenceEnums.MODULE, null), values,
                bookingService.cancellationPolicies()));
    }

    /**
     * Recherche unique du titulaire : membres, clients et entreprises par nom, code, courriel ou
     * telephone. {@code types} restreint (MEMBER, CUSTOMER, BUSINESS_ENTITY, separes par des virgules).
     */
    @GetMapping("/lookup/owners")
    public ResponseEntity<PaginatedResponse<LookupItem>> owners(@RequestParam(required = false) String q,
                                                                @RequestParam(required = false) String types,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ownerLookup.search(q, types, page, size));
    }
}
