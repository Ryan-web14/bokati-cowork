package com.sni.bokaticowork.security.authorization;

import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class ClientPortalAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private static final String ROLE_MEMBER = "ROLE_MEMBER";

    private final MemberRepository memberRepository;

    @Override
    public AuthorizationResult authorize(Supplier<? extends Authentication> authentication,
                                         RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        if (auth == null || !auth.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }

        if (!has(auth.getAuthorities(), ROLE_MEMBER)) {
            return new AuthorizationDecision(false);
        }

        if (!(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return new AuthorizationDecision(false);
        }

        Optional<Member> memberOpt = memberRepository.findByUser_IdAndDeletedFalse(principal.getUser().getId());
        if (memberOpt.isEmpty()) {
            return new AuthorizationDecision(false);
        }

        Member member = memberOpt.get();
        return new AuthorizationDecision(Boolean.TRUE.equals(member.getPortalAccess()));
    }

    private boolean has(Collection<? extends GrantedAuthority> authorities, String authority) {
        return authorities.stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }
}
