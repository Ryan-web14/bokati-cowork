package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.booking.dto.request.BookingIdentityLookup;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.repository.BusinessRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class BookingIdentityResolver {

    private final MemberRepository memberRepository;
    private final CustomerRepository customerRepository;
    private final BusinessRepository businessRepository;
    private final SequenceGeneratorFacade sequenceGenerator;

    public ResolvedBookingIdentity resolve(BookingIdentityLookup lookup) {
        return resolve(lookup, true);
    }

    public ResolvedBookingIdentity resolveExisting(BookingIdentityLookup lookup) {
        return resolve(lookup, false);
    }

    private ResolvedBookingIdentity resolve(BookingIdentityLookup lookup, boolean allowTransientOwner) {
        BookingIdentityLookup safeLookup = lookup == null
                ? new BookingIdentityLookup(null, null, null, null, null, null, null, null, null)
                : lookup;

        String memberId = trim(safeLookup.memberId());
        String customerId = trim(safeLookup.customerId());
        String businessCode = trim(safeLookup.businessCode());
        String email = trim(safeLookup.email());
        String phone = trim(safeLookup.phone());

        int explicitTargets = count(memberId, customerId, businessCode);
        if (explicitTargets > 1) {
            throw new BadRequestException("Only one owner selector is allowed: memberId, customerId or businessCode");
        }

        if (StringUtils.hasText(memberId)) {
            Member member = memberRepository.findByMemberIdAndDeletedFalse(memberId)
                    .orElseThrow(() -> new ResourceNotFoundException("Member " + memberId + " not found"));
            return fromMember(member, safeLookup, false);
        }
        if (StringUtils.hasText(customerId)) {
            Customer customer = customerRepository.findByCustomerId(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer " + customerId + " not found"));
            return fromCustomer(customer, safeLookup, false);
        }
        if (StringUtils.hasText(businessCode)) {
            BusinessEntity business = businessRepository.findByCode(businessCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Business entity " + businessCode + " not found"));
            return fromBusiness(business, safeLookup, false);
        }

        if (StringUtils.hasText(email)) {
            ResolvedBookingIdentity resolved = findByEmail(email, safeLookup);
            if (resolved != null) {
                return resolved;
            }
        }
        if (StringUtils.hasText(phone)) {
            ResolvedBookingIdentity resolved = findByPhone(phone, safeLookup);
            if (resolved != null) {
                return resolved;
            }
        }

        if (allowTransientOwner && shouldCreateGuest(safeLookup)) {
            return guest(safeLookup, email, phone);
        }

        throw new BadRequestException("A memberId, customerId, businessCode, email or phone is required");
    }

    private ResolvedBookingIdentity findByEmail(String email, BookingIdentityLookup lookup) {
        return memberRepository.findByEmailIgnoreCaseAndDeletedFalse(email)
                .map(member -> fromMember(member, lookup, false))
                .or(() -> customerRepository.findByEmailAndDeletedFalse(email).map(customer -> fromCustomer(customer, lookup, false)))
                .or(() -> businessRepository.findByEmailAndDeletedFalse(email).map(business -> fromBusiness(business, lookup, false)))
                .orElse(null);
    }

    private ResolvedBookingIdentity findByPhone(String phone, BookingIdentityLookup lookup) {
        return memberRepository.findByPhoneAndDeletedFalse(phone)
                .map(member -> fromMember(member, lookup, false))
                .or(() -> customerRepository.findByPhoneAndDeletedFalse(phone).map(customer -> fromCustomer(customer, lookup, false)))
                .or(() -> businessRepository.findByPhoneAndDeletedFalse(phone).map(business -> fromBusiness(business, lookup, false)))
                .orElse(null);
    }

    private ResolvedBookingIdentity fromMember(Member member, BookingIdentityLookup lookup, boolean transientOwner) {
        return new ResolvedBookingIdentity(
                SubscriberType.MEMBER,
                member.getMemberId(),
                firstText(lookup.contactName(), member.getDisplayName()),
                firstText(lookup.contactEmail(), lookup.email(), member.getEmail()),
                firstText(lookup.contactPhone(), lookup.phone(), member.getPhone()),
                transientOwner
        );
    }

    private ResolvedBookingIdentity fromCustomer(Customer customer, BookingIdentityLookup lookup, boolean transientOwner) {
        return new ResolvedBookingIdentity(
                SubscriberType.CUSTOMER,
                customer.getCustomerId(),
                firstText(lookup.contactName(), customer.getCompanyName(), fullName(customer.getFirstname(), customer.getLastname())),
                firstText(lookup.contactEmail(), lookup.email(), customer.getBillingEmail(), customer.getEmail()),
                firstText(lookup.contactPhone(), lookup.phone(), customer.getPhone()),
                transientOwner
        );
    }

    private ResolvedBookingIdentity fromBusiness(BusinessEntity business, BookingIdentityLookup lookup, boolean transientOwner) {
        return new ResolvedBookingIdentity(
                SubscriberType.BUSINESS_ENTITY,
                business.getCode(),
                firstText(lookup.contactName(), business.getName()),
                firstText(lookup.contactEmail(), lookup.email(), business.getEmail()),
                firstText(lookup.contactPhone(), lookup.phone(), business.getPhone()),
                transientOwner
        );
    }

    private ResolvedBookingIdentity guest(BookingIdentityLookup lookup, String email, String phone) {
        return new ResolvedBookingIdentity(
                SubscriberType.CUSTOMER,
                sequenceGenerator.next("booking_guest"),
                trim(lookup.contactName()),
                firstText(lookup.contactEmail(), email),
                firstText(lookup.contactPhone(), phone),
                true
        );
    }

    private boolean shouldCreateGuest(BookingIdentityLookup lookup) {
        return Boolean.TRUE.equals(lookup.walkIn())
                || StringUtils.hasText(lookup.contactName())
                || StringUtils.hasText(lookup.contactEmail())
                || StringUtils.hasText(lookup.contactPhone())
                || StringUtils.hasText(lookup.email())
                || StringUtils.hasText(lookup.phone());
    }

    private int count(String... values) {
        int count = 0;
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                count++;
            }
        }
        return count;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String fullName(String firstname, String lastname) {
        String value = ((firstname == null ? "" : firstname) + " " + (lastname == null ? "" : lastname)).trim();
        return StringUtils.hasText(value) ? value : null;
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public record ResolvedBookingIdentity(
            SubscriberType ownerType,
            String ownerCode,
            String contactName,
            String contactEmail,
            String contactPhone,
            boolean transientOwner
    ) {
    }
}
