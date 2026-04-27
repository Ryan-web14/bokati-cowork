package com.sni.bokaticowork.features.subscription.subscription.service.support;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
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
                yield new Owner(member.getMemberId(), member, null, null);
            }
            case CUSTOMER -> {
                Customer customer = customerRepository.findByCustomerId(code)
                        .orElseThrow(() -> new ResourceNotFoundException("Customer " + code + " not found"));
                yield new Owner(customer.getCustomerId(), null, customer, null);
            }
            case BUSINESS_ENTITY -> {
                BusinessEntity business = businessRepository.findByCode(code)
                        .orElseThrow(() -> new ResourceNotFoundException("Business entity " + code + " not found"));
                yield new Owner(business.getCode(), null, null, business);
            }
        };
    }

    public record Owner(String code, Member member, Customer customer, BusinessEntity businessEntity) {
    }
}
