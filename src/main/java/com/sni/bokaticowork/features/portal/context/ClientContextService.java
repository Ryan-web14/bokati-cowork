package com.sni.bokaticowork.features.portal.context;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.service.interfaces.MemberService;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClientContextService {

    private final MemberService memberService;

    public Member getAuthenticatedMember() {
        UserPrincipal principal = resolvePrincipal();
        return memberService.getByUserForService(principal.getUser().getId());
    }

    public Customer getAuthenticatedCustomer() {
        Member member = getAuthenticatedMember();
        if (member.getCustomer() == null) {
            throw new ResourceNotFoundException("No customer linked to this member account");
        }
        return member.getCustomer();
    }

    public String getMemberCode() {
        return getAuthenticatedMember().getMemberId();
    }

    public String getCustomerCode() {
        return getAuthenticatedCustomer().getCustomerId();
    }

    private UserPrincipal resolvePrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResourceNotFoundException("No authenticated member session");
        }
        return principal;
    }
}
