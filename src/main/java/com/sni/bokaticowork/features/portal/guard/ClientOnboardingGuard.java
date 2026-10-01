package com.sni.bokaticowork.features.portal.guard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.ApiError;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
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
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ClientOnboardingGuard implements HandlerInterceptor {

    private static final String ONBOARDING_STATUS_PATH = "/me/onboarding-status";

    private final MemberRepository memberRepository;
    private final KycCaseRepository kycCaseRepository;
    private final com.sni.bokaticowork.features.document.kyc.repository.KycDocumentRepository kycDocumentRepository;
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

        Optional<KycCase> kycCase = kycCaseRepository
                .findFirstByOwnerTypeAndOwnerIdOrderByStartedAtDesc(DocumentOwnerType.MEMBER, member.getId());

        if (kycCase.map(k -> k.getStatus() == KycCaseStatus.APPROVED).orElse(false)) {
            return true;
        }

        Instant gracePeriodEnd = member.getKycGracePeriodEndAt();
        boolean inGracePeriod = gracePeriodEnd != null && Instant.now().isBefore(gracePeriodEnd);

        if (!inGracePeriod) {
            // Un refus dit ou en est le dossier · « votre verification est requise » laisse le
            // titulaire sans rien a faire, et le guichet sans rien a repondre. On nomme l'etape,
            // et les pieces refusees quand il y en a.
            KycCase current = kycCase.orElse(null);
            writeError(response, HttpStatus.FORBIDDEN, "KYC_REQUIRED",
                    explain(current), rejectedPieces(current));
            return false;
        }

        return true;
    }

    /** Ou en est le dossier, dit au titulaire · et ce qu'il peut faire ensuite. */
    private String explain(KycCase kycCase) {
        String next = " Consultez le détail sur /client/me/onboarding-status.";
        if (kycCase == null) {
            return "Votre vérification d'identité n'a pas encore commencé. Déposez vos pièces pour accéder à votre espace." + next;
        }
        return switch (kycCase.getStatus()) {
            case SUBMITTED, UNDER_REVIEW -> "Vos pièces sont en cours de vérification par notre équipe (dossier "
                    + kycCase.getCode() + "). Vous y aurez accès dès qu'elle sera terminée." + next;
            case REJECTED, PENDING_CORRECTION -> "Une ou plusieurs pièces de votre dossier " + kycCase.getCode()
                    + " ont été refusées. Déposez-en de nouvelles pour poursuivre." + next;
            case EXPIRED, RENEWAL_REQUIRED -> "Votre vérification d'identité (dossier " + kycCase.getCode()
                    + ") est arrivée à échéance. Déposez des pièces à jour." + next;
            default -> "Votre dossier " + kycCase.getCode()
                    + " est incomplet. Déposez les pièces demandées pour accéder à votre espace." + next;
        };
    }

    /** Les pieces refusees, nommees · c'est ce que le titulaire doit redeposer. */
    private List<String> rejectedPieces(KycCase kycCase) {
        if (kycCase == null) {
            return List.of();
        }
        return kycDocumentRepository.findAllByKycCaseOrderByIdAsc(kycCase).stream()
                .filter(item -> item.getStatus() == KycDocumentVerificationStatus.REJECTED)
                .map(item -> item.getDocument() != null && item.getDocument().getDocumentType() != null
                        ? item.getDocument().getDocumentType().getName()
                        : item.getDocumentType())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String code, String message) throws Exception {
        writeError(response, status, code, message, List.of());
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String code, String message,
                            List<String> details) throws Exception {
        response.setStatus(status.value());
        // Sans encodage declare, un « é » part en ISO-8859-1 et s'affiche casse cote client.
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = ApiError.builder()
                .status(status.value())
                .errorCode(code)
                .message(message)
                .errors(details == null || details.isEmpty() ? null : details)
                .build();
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
