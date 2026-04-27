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
}
