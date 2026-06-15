package com.sni.bokaticowork.features.portal.guard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.ApiError;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ClientOnboardingGuard implements HandlerInterceptor {

    private static final String ONBOARDING_STATUS_PATH = "/me/onboarding-status";

    private final MemberRepository memberRepository;
    private final KycCaseRepository kycCaseRepository;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        if (uri.endsWith(ONBOARDING_STATUS_PATH)) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return true;
        }

        Optional<Member> memberOpt = memberRepository.findByUser_IdAndDeletedFalse(principal.getUser().getId());
        if (memberOpt.isEmpty()) {
            return true;
        }

        Member member = memberOpt.get();

        if (Boolean.TRUE.equals(member.getUser().getIsAccountLocked())) {
            writeError(response, HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED",
                    "Please verify your email before accessing this resource.");
            return false;
        }

        boolean kycApproved = kycCaseRepository
                .findFirstByOwnerTypeAndOwnerIdOrderByStartedAtDesc(DocumentOwnerType.MEMBER, member.getId())
                .map(k -> k.getStatus() == KycCaseStatus.APPROVED)
                .orElse(false);

        if (kycApproved) {
            return true;
        }

        Instant gracePeriodEnd = member.getKycGracePeriodEndAt();
        boolean inGracePeriod = gracePeriodEnd != null && Instant.now().isBefore(gracePeriodEnd);

        if (!inGracePeriod) {
            writeError(response, HttpStatus.FORBIDDEN, "KYC_REQUIRED",
                    "Your KYC verification is required to access this resource. Please complete your identity verification.");
            return false;
        }

        return true;
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String code, String message) throws Exception {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = ApiError.builder()
                .status(status.value())
                .errorCode(code)
                .message(message)
                .build();
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
