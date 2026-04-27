package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionOwnerResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class BookingOwnerContactResolver {

    private final SubscriptionOwnerResolver ownerResolver;

    public Contact resolve(SubscriberType ownerType, String ownerCode, String requestName, String requestEmail, String requestPhone) {
        SubscriptionOwnerResolver.Owner owner = ownerResolver.resolve(ownerType, ownerCode);
        String name = trim(requestName);
        String email = trim(requestEmail);
        String phone = trim(requestPhone);

        if (owner.member() != null) {
            Member member = owner.member();
            name = firstText(name, member.getDisplayName());
            email = firstText(email, member.getEmail());
            phone = firstText(phone, member.getPhone());
        } else if (owner.customer() != null) {
            Customer customer = owner.customer();
            name = firstText(name, customer.getCompanyName(), fullName(customer.getFirstname(), customer.getLastname()));
            email = firstText(email, customer.getBillingEmail(), customer.getEmail());
            phone = firstText(phone, customer.getPhone());
        } else if (owner.businessEntity() != null) {
            BusinessEntity business = owner.businessEntity();
            name = firstText(name, business.getName());
            email = firstText(email, business.getEmail());
            phone = firstText(phone, business.getPhone());
        }
        return new Contact(name, email, phone);
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

    public record Contact(String name, String email, String phone) {
    }
}
