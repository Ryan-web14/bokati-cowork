package com.sni.bokaticowork.features.portal.profile.service;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.portal.profile.dto.response.OnboardingStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientOnboardingService {

    private final KycCaseRepository kycCaseRepository;

    @Transactional(readOnly = true)
    public OnboardingStatusResponse getOnboardingStatus(Member member) {
        boolean emailVerified = !Boolean.TRUE.equals(member.getUser().getIsAccountLocked());

        Optional<KycCase> kycCaseOpt = kycCaseRepository
                .findFirstByOwnerTypeAndOwnerIdOrderByStartedAtDesc(DocumentOwnerType.MEMBER, member.getId());

        String kycStatus = kycCaseOpt.map(k -> k.getStatus().name()).orElse(KycCaseStatus.NOT_STARTED.name());
        boolean kycApproved = kycCaseOpt.map(k -> k.getStatus() == KycCaseStatus.APPROVED).orElse(false);
        int kycPercent = computeKycPercent(kycStatus);

        Instant now = Instant.now();
        Instant gracePeriodEnd = member.getKycGracePeriodEndAt();
        boolean gracePeriodActive = gracePeriodEnd != null && now.isBefore(gracePeriodEnd);
        long graceDaysRemaining = gracePeriodEnd != null
                ? Math.max(0, ChronoUnit.DAYS.between(now, gracePeriodEnd))
                : 0;

        boolean portalFullyActive = emailVerified && kycApproved;
        String nextStep = computeNextStep(emailVerified, kycStatus, gracePeriodActive);
        List<String> restrictedActions = computeRestrictedActions(emailVerified, kycApproved, gracePeriodActive);

        return OnboardingStatusResponse.builder()
                .emailVerified(emailVerified)
                .profileComplete(true)
                .kycStatus(kycStatus)
                .kycCompletionPercent(kycPercent)
                .gracePeriodEndsAt(gracePeriodEnd)
                .gracePeriodActive(gracePeriodActive)
                .gracePeriodDaysRemaining(graceDaysRemaining)
                .portalFullyActive(portalFullyActive)
                .nextStep(nextStep)
                .restrictedActions(restrictedActions)
                .build();
    }

    private int computeKycPercent(String status) {
        return switch (status) {
            case "NOT_STARTED" -> 0;
            case "IN_PROGRESS" -> 30;
            case "SUBMITTED" -> 60;
            case "UNDER_REVIEW" -> 80;
            case "APPROVED" -> 100;
            case "PENDING_CORRECTION" -> 50;
            default -> 0;
        };
    }

    private String computeNextStep(boolean emailVerified, String kycStatus, boolean gracePeriodActive) {
        if (!emailVerified) return "VERIFY_EMAIL";
        if ("NOT_STARTED".equals(kycStatus)) return "SUBMIT_KYC";
        if ("IN_PROGRESS".equals(kycStatus) || "PENDING_CORRECTION".equals(kycStatus)) return "COMPLETE_KYC";
        if ("SUBMITTED".equals(kycStatus) || "UNDER_REVIEW".equals(kycStatus)) return "AWAIT_KYC_REVIEW";
        if ("APPROVED".equals(kycStatus)) return "COMPLETED";
        if (gracePeriodActive) return "SUBMIT_KYC_BEFORE_GRACE_EXPIRES";
        return "CONTACT_SUPPORT";
    }

    private List<String> computeRestrictedActions(boolean emailVerified, boolean kycApproved, boolean gracePeriodActive) {
        if (!emailVerified) {
            return List.of("BOOKING", "PAYMENT", "CONTRACT", "SUBSCRIPTION");
        }
        if (!kycApproved && !gracePeriodActive) {
            return List.of("BOOKING", "PAYMENT", "CONTRACT", "SUBSCRIPTION");
        }
        return List.of();
    }
}
