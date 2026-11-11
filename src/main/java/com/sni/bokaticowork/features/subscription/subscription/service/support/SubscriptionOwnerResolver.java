package com.sni.bokaticowork.features.subscription.subscription.service.support;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.repository.BusinessRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionOwnerResolver {

    private final MemberRepository memberRepository;
    private final CustomerRepository customerRepository;
    private final BusinessRepository businessRepository;

    public Owner resolve(SubscriberType type, String code) {
        return switch (type) {
            case MEMBER -> {
                Member member = memberRepository.findByMemberIdAndDeletedFalse(code)
                        .orElseThrow(() -> new ResourceNotFoundException("Member " + code + " not found"));
                yield new Owner(member.getMemberId(), member, null, null, SubscriberType.MEMBER);
            }
            case CUSTOMER -> {
                Customer customer = customerRepository.findByCustomerId(code)
                        .orElseThrow(() -> new ResourceNotFoundException("Customer " + code + " not found"));
                yield new Owner(customer.getCustomerId(), null, customer, null, SubscriberType.CUSTOMER);
            }
            case BUSINESS_ENTITY -> {
                BusinessEntity business = businessRepository.findByCode(code).orElse(null);
                if (business != null) {
                    yield new Owner(business.getCode(), null, null, business, SubscriberType.BUSINESS_ENTITY);
                }
                // BUSINESS_ENTITY designe une fiche /businesses (table business_entity, codes BIZ-...).
                // Une entreprise cliente creee par /companies (code CUS-COM-...) est un client de type
                // COMPANY : envoyee en BUSINESS_ENTITY, elle est reconnue et traitee comme CUSTOMER,
                // c'est le type enregistre ensuite.
                Customer company = customerRepository.findByCustomerId(code)
                        .filter(customer -> customer.getType() == CustomerType.COMPANY)
                        .orElseThrow(() -> new ResourceNotFoundException("Business entity " + code + " not found"
                                + " (pour une entreprise cliente, subscriberType CUSTOMER et son code CUS-COM-...)"));
                yield new Owner(company.getCustomerId(), null, company, null, SubscriberType.CUSTOMER);
            }
        };
    }

    /**
     * @param type le type reellement trouve · peut differer du type demande (entreprise cliente
     *             envoyee en BUSINESS_ENTITY, reconnue comme CUSTOMER)
     */
    public record Owner(String code, Member member, Customer customer, BusinessEntity businessEntity, SubscriberType type) {

        public Owner(String code, Member member, Customer customer, BusinessEntity businessEntity) {
            this(code, member, customer, businessEntity,
                    member != null ? SubscriberType.MEMBER
                            : customer != null ? SubscriberType.CUSTOMER
                            : businessEntity != null ? SubscriberType.BUSINESS_ENTITY
                            : null);
        }

        /** Le type a enregistrer : celui trouve, sinon celui demande. */
        public SubscriberType typeOr(SubscriberType requested) {
            return type != null ? type : requested;
        }
    }
}
