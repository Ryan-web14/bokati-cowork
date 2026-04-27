package com.sni.bokaticowork.features.booking.service.implementation;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.booking.dto.request.CreateBookingAudiencePolicyRequest;
import com.sni.bokaticowork.features.booking.dto.request.CreateBookingQuotaOverrideRequest;
import com.sni.bokaticowork.features.booking.dto.response.BookingAudiencePolicyResponse;
import com.sni.bokaticowork.features.booking.dto.response.BookingQuotaOverrideResponse;
import com.sni.bokaticowork.features.booking.model.BookingAudiencePolicy;
import com.sni.bokaticowork.features.booking.model.BookingQuotaOverride;
import com.sni.bokaticowork.features.booking.repository.BookingAudiencePolicyRepository;
import com.sni.bokaticowork.features.booking.repository.BookingQuotaOverrideRepository;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingPolicyService;
import com.sni.bokaticowork.features.booking.service.support.BookingIdentityResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class BookingPolicyServiceImpl implements BookingPolicyService {

    private final BookingAudiencePolicyRepository policyRepository;
    private final BookingQuotaOverrideRepository overrideRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final BookingIdentityResolver identityResolver;

    @Override
    public BookingAudiencePolicyResponse createPolicy(CreateBookingAudiencePolicyRequest request) {
        BookingAudiencePolicy policy = policyRepository.save(BookingAudiencePolicy.builder()
                .policyNumber(sequenceGenerator.next("booking_audience_policy"))
                .audienceType(request.audienceType())
                .resourceTypeCode(trim(request.resourceTypeCode()))
                .resourceGroupCode(trim(request.resourceGroupCode()))
                .approvalRequired(Boolean.TRUE.equals(request.approvalRequired()))
                .maxActiveBookings(request.maxActiveBookings())
                .maxBookingsPerDay(request.maxBookingsPerDay())
                .maxBookingsPerWeek(request.maxBookingsPerWeek())
                .maxBookingsPerMonth(request.maxBookingsPerMonth())
                .maxNoShowsPerMonth(request.maxNoShowsPerMonth())
                .minBookingNoticeMinutes(request.minBookingNoticeMinutes())
                .maxBookingDurationMinutes(request.maxBookingDurationMinutes())
                .active(request.active() == null || request.active())
                .build());
        return toResponse(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingAudiencePolicyResponse> listPolicies() {
        return policyRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public BookingQuotaOverrideResponse createOverride(CreateBookingQuotaOverrideRequest request) {
        BookingIdentityResolver.ResolvedBookingIdentity identity = identityResolver.resolveExisting(request.identityLookup());
        BookingQuotaOverride override = overrideRepository.save(BookingQuotaOverride.builder()
                .overrideNumber(sequenceGenerator.next("booking_quota_override"))
                .ownerType(identity.ownerType())
                .ownerCode(identity.ownerCode())
                .resourceCode(trim(request.resourceCode()))
                .extraActiveBookings(request.extraActiveBookings())
                .extraBookingsPerDay(request.extraBookingsPerDay())
                .extraBookingsPerWeek(request.extraBookingsPerWeek())
                .extraBookingsPerMonth(request.extraBookingsPerMonth())
                .reason(trim(request.reason()))
                .approvedBy(trim(request.approvedBy()))
                .validFrom(request.validFrom() == null ? Instant.now() : request.validFrom())
                .validUntil(request.validUntil())
                .active(Boolean.TRUE)
                .build());
        return toResponse(override);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingQuotaOverrideResponse> listOverrides() {
        return overrideRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private BookingAudiencePolicyResponse toResponse(BookingAudiencePolicy policy) {
        return new BookingAudiencePolicyResponse(policy.getPolicyNumber(), policy.getAudienceType(), policy.getResourceTypeCode(), policy.getResourceGroupCode(), policy.getApprovalRequired(), policy.getMaxActiveBookings(), policy.getMaxBookingsPerDay(), policy.getMaxBookingsPerWeek(), policy.getMaxBookingsPerMonth(), policy.getMaxNoShowsPerMonth(), policy.getMinBookingNoticeMinutes(), policy.getMaxBookingDurationMinutes(), policy.getActive());
    }

    private BookingQuotaOverrideResponse toResponse(BookingQuotaOverride override) {
        return new BookingQuotaOverrideResponse(override.getOverrideNumber(), override.getOwnerType(), override.getOwnerCode(), override.getResourceCode(), override.getExtraActiveBookings(), override.getExtraBookingsPerDay(), override.getExtraBookingsPerWeek(), override.getExtraBookingsPerMonth(), override.getReason(), override.getApprovedBy(), override.getValidFrom(), override.getValidUntil(), override.getActive());
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
