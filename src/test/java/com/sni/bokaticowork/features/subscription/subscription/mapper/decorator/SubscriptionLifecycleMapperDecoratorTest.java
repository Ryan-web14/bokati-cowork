package com.sni.bokaticowork.features.subscription.subscription.mapper.decorator;

import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.SubscriptionResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.TargetAudience;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.model.SubscriptionPlan;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubscriptionLifecycleMapperDecoratorTest {

    private final SubscriptionLifecycleMapperDecorator mapper = new SubscriptionLifecycleMapperDecorator() {
    };

    @Test
    void shouldExposeMemberDisplayNameInSubscriptionResponse() {
        Subscription subscription = baseSubscription();
        subscription.setMember(Member.builder()
                .firstname("Jean")
                .lastname("Beberre")
                .build());

        SubscriptionResponse response = mapper.toResponse(subscription);

        assertEquals("Jean Beberre", response.subscriberName());
    }

    @Test
    void shouldFallbackToCustomerOrBusinessName() {
        Subscription customerSubscription = baseSubscription();
        customerSubscription.setCustomer(Customer.builder()
                .companyName("Bokati SARL")
                .build());

        SubscriptionResponse customerResponse = mapper.toResponse(customerSubscription);

        assertEquals("Bokati SARL", customerResponse.subscriberName());

        Subscription businessSubscription = baseSubscription();
        businessSubscription.setCustomer(null);
        businessSubscription.setBusinessEntity(BusinessEntity.builder()
                .name("Bokati Group")
                .build());

        SubscriptionResponse businessResponse = mapper.toResponse(businessSubscription);

        assertEquals("Bokati Group", businessResponse.subscriberName());
    }

    @Test
    void shouldFallbackToSubscriberCodeWhenNoNameExists() {
        Subscription subscription = baseSubscription();

        SubscriptionResponse response = mapper.toResponse(subscription);

        assertEquals("MBR-0001", response.subscriberName());
    }

    private Subscription baseSubscription() {
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .code("PLN-00004")
                .name("Coworking")
                .description("Plan")
                .planType(PlanType.COWORKING_ACCESS)
                .targetAudience(TargetAudience.INDIVIDUAL)
                .status(PlanStatus.ACTIVE)
                .visible(true)
                .build();
        PlanVersion version = PlanVersion.builder()
                .plan(plan)
                .versionNumber(1)
                .name("Coworking Monthly")
                .status(PlanStatus.ACTIVE)
                .build();

        return Subscription.builder()
                .subscriptionNumber("SUB-0001")
                .subscriberType(SubscriberType.MEMBER)
                .subscriberCode("MBR-0001")
                .planVersion(version)
                .status(SubscriptionStatus.PENDING_ACTIVATION)
                .startDate(LocalDate.of(2026, 4, 26))
                .currentPeriodStart(LocalDate.of(2026, 4, 26))
                .currentPeriodEnd(LocalDate.of(2026, 5, 25))
                .nextBillingDate(LocalDate.of(2026, 5, 26))
                .autoRenew(true)
                .billingCycle(BillingCycle.MONTHLY)
                .currency("XAF")
                .subtotalAmount(new BigDecimal("10000"))
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("10000"))
                .cancelAtPeriodEnd(false)
                .build();
    }
}
