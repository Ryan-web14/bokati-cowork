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
        boolean emailVerified = member.getUser() != null && member.getUser().emailVerified();

        Optional<KycCase> kycCaseOpt = kycCaseRepository
                .findFirstByOwnerTypeAndOwnerIdOrderByStartedAtDesc(DocumentOwnerType.MEMBER, member.getId());

        KycCaseStatus rawStatus = kycCaseOpt.map(KycCase::getStatus).orElse(null);
        String kycStatus = toFrontendKycStatus(rawStatus);
        boolean kycApproved = "APPROVED".equals(kycStatus);
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

    /**
     * Maps the 9 backend KycCaseStatus values to 5 simplified frontend statuses:
     *   NOT_STARTED  → NOT_STARTED  (no case or not started)
     *   IN_PROGRESS  → DRAFT        (uploading documents, not yet submitted)
     *   SUBMITTED, UNDER_REVIEW → SUBMITTED (documents being reviewed)
     *   APPROVED     → APPROVED     (fully verified)
     *   REJECTED, PENDING_CORRECTION, RENEWAL_REQUIRED, EXPIRED → REJECTED (needs action)
     */
    private String toFrontendKycStatus(KycCaseStatus status) {
        if (status == null) return "NOT_STARTED";
        return switch (status) {
            case NOT_STARTED -> "NOT_STARTED";
            case IN_PROGRESS -> "DRAFT";
            case SUBMITTED, UNDER_REVIEW -> "SUBMITTED";
            case APPROVED -> "APPROVED";
            case REJECTED, PENDING_CORRECTION, RENEWAL_REQUIRED, EXPIRED -> "REJECTED";
        };
    }

    private int computeKycPercent(String status) {
        return switch (status) {
            case "NOT_STARTED" -> 0;
            case "DRAFT" -> 30;
            case "SUBMITTED" -> 70;
            case "APPROVED" -> 100;
            case "REJECTED" -> 40;
            default -> 0;
        };
    }

    private String computeNextStep(boolean emailVerified, String kycStatus, boolean gracePeriodActive) {
        if (!emailVerified) return "VERIFY_EMAIL";
        if ("NOT_STARTED".equals(kycStatus)) return "SUBMIT_KYC";
        if ("DRAFT".equals(kycStatus)) return "COMPLETE_KYC";
        if ("REJECTED".equals(kycStatus)) return "CORRECT_KYC";
        if ("SUBMITTED".equals(kycStatus)) return "AWAIT_KYC_REVIEW";
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
