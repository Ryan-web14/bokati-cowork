package com.sni.bokaticowork.features.domiciliation.service;

import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.company.enums.MemberLinkRole;
import com.sni.bokaticowork.features.company.enums.MemberLinkStatus;
import com.sni.bokaticowork.features.company.membership.model.CustomerMemberLink;
import com.sni.bokaticowork.features.company.membership.repository.CustomerMemberLinkRepository;
import com.sni.bokaticowork.features.domiciliation.model.DomiciliationContract;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Qui, cote domicilie, voit la domiciliation et retire le courrier.
 *
 * <p>Un membre domicilie a son nom : lui. Une organisation domiciliee : son gerant, et les membres
 * rattaches dont le lien porte le drapeau {@code can_collect_mail}. La fin du lien leur retire
 * l'acces.</p>
 */
@Component
@RequiredArgsConstructor
public class DomiciliationCompanyAccess {

    private final CustomerRepository customerRepository;
    private final CustomerMemberLinkRepository linkRepository;

    /** L'organisation domiciliee par cet abonnement · vide pour un membre ou une entite. */
    public Optional<Customer> companyOf(Subscription subscription) {
        if (subscription == null || subscription.getSubscriberType() != SubscriberType.CUSTOMER) {
            return Optional.empty();
        }
        return customerRepository.findByCustomerId(subscription.getSubscriberCode())
                .filter(customer -> customer.getType() == CustomerType.COMPANY && !customer.isDeleted());
    }

    /** Le membre voit-il ce contrat, et retire-t-il son courrier. */
    public boolean mayAccess(Member member, DomiciliationContract contract) {
        Subscription subscription = contract.getSubscription();
        if (subscription.getSubscriberType() == SubscriberType.MEMBER) {
            return member.getMemberId().equals(subscription.getSubscriberCode());
        }
        return companyOf(subscription)
                .flatMap(company -> linkRepository.findByCustomerAndMemberAndStatus(company, member, MemberLinkStatus.ACTIVE))
                .filter(DomiciliationCompanyAccess::authorized)
                .isPresent();
    }

    /** Les organisations dont le membre voit la domiciliation. */
    public List<String> companiesFor(Member member) {
        List<String> codes = new ArrayList<>();
        for (CustomerMemberLink link : linkRepository.findAllByMemberAndStatusOrderByStartedAtDesc(member, MemberLinkStatus.ACTIVE)) {
            if (authorized(link) && link.getCustomer() != null) {
                codes.add(link.getCustomer().getCustomerId());
            }
        }
        return codes;
    }

    static boolean authorized(CustomerMemberLink link) {
        return link.getRole() == MemberLinkRole.OWNER || link.isCanCollectMail();
    }
}
