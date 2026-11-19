package com.sni.bokaticowork.features.booking.service.interfaces;

import com.sni.bokaticowork.features.booking.dto.request.CreateBookingAudiencePolicyRequest;
import com.sni.bokaticowork.features.booking.dto.request.CreateBookingQuotaOverrideRequest;
import com.sni.bokaticowork.features.booking.dto.response.BookingAudiencePolicyResponse;
import com.sni.bokaticowork.features.booking.dto.response.BookingQuotaOverrideResponse;

import java.util.List;

public interface BookingPolicyService {
    BookingAudiencePolicyResponse createPolicy(CreateBookingAudiencePolicyRequest request);
    List<BookingAudiencePolicyResponse> listPolicies();
    BookingQuotaOverrideResponse createOverride(CreateBookingQuotaOverrideRequest request);
    List<BookingQuotaOverrideResponse> listOverrides();

    /** Lot B7 : politiques et derogations modifiables, filtre actif. */
    default List<BookingAudiencePolicyResponse> listPolicies(Boolean active) {
        return listPolicies().stream().filter(policy -> active == null || active.equals(policy.active())).toList();
    }

    default List<BookingQuotaOverrideResponse> listOverrides(Boolean active) {
        return listOverrides().stream().filter(override -> active == null || active.equals(override.active())).toList();
    }

    BookingAudiencePolicyResponse updatePolicy(String policyNumber, CreateBookingAudiencePolicyRequest request);

    BookingAudiencePolicyResponse setPolicyActive(String policyNumber, boolean active);

    BookingQuotaOverrideResponse updateOverride(String overrideNumber,
                                                com.sni.bokaticowork.features.booking.dto.request.UpdateBookingQuotaOverrideRequest request);

    BookingQuotaOverrideResponse setOverrideActive(String overrideNumber, boolean active);
}
