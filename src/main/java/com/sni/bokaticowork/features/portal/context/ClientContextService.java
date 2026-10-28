package com.sni.bokaticowork.features.portal.context;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.service.interfaces.MemberService;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClientContextService {

    private final MemberService memberService;

    /**
     * Le membre connecte, avec son client deja charge.
     *
     * <p>Le membre sort de cette methode detache · son client n'etait qu'une reference paresseuse,
     * et le premier appelant qui la lisait hors transaction tombait en LazyInitializationException
     * (calendrier et creneaux du portail en 500). Il est charge ici, tant que la session existe.</p>
     */
    @Transactional(readOnly = true)
    public Member getAuthenticatedMember() {
        UserPrincipal principal = resolvePrincipal();
        Member member = memberService.getByUserForService(principal.getUser().getId());
        if (member != null && member.getCustomer() != null) {
            Hibernate.initialize(member.getCustomer());
        }
        return member;
    }

    @Transactional(readOnly = true)
    public Customer getAuthenticatedCustomer() {
        Member member = getAuthenticatedMember();
        if (member.getCustomer() == null) {
            throw new ResourceNotFoundException("No customer linked to this member account");
        }
        return Hibernate.unproxy(member.getCustomer(), Customer.class);
    }

    @Transactional(readOnly = true)
    public String getMemberCode() {
        return getAuthenticatedMember().getMemberId();
    }

    @Transactional(readOnly = true)
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
