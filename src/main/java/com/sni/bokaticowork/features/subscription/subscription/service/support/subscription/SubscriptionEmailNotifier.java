package com.sni.bokaticowork.features.subscription.subscription.service.support.subscription;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionEventType;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionEmailNotifier {

    private final DefaultEmailSender emailSender;
    private final TemplateEngine templateEngine;

    public void notify(Subscription subscription, SubscriptionEventType eventType) {
        Recipient recipient = resolveRecipient(subscription);
        if (!StringUtils.hasText(recipient.email())) {
            return;
        }
        // Render template eagerly while entity is still in-session (avoids LazyInitializationException after commit)
        String html;
        String subject = subject(subscription, eventType);
        try {
            html = templateEngine.process(template(eventType), context(subscription, recipient, eventType));
        } catch (Exception ex) {
            log.warn("Failed to render subscription email {} for {}", eventType, subscription.getSubscriptionNumber(), ex);
            return;
        }
        String to = recipient.email();
        String subscriptionNumber = subscription.getSubscriptionNumber();
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            // Defer sending until after commit — avoids emailing on rollback
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch(to, subject, html, subscriptionNumber, eventType);
                }
            });
        } else {
            dispatch(to, subject, html, subscriptionNumber, eventType);
        }
    }

    private void dispatch(String to, String subject, String html, String subscriptionNumber, SubscriptionEventType eventType) {
        try {
            emailSender.sendHtmlEmail(to, subject, html);
        } catch (Exception ex) {
            log.warn("Failed to queue subscription email {} for {}", eventType, subscriptionNumber, ex);
        }
    }

    private Context context(Subscription subscription, Recipient recipient, SubscriptionEventType eventType) {
        Context context = new Context(Locale.FRENCH);
        context.setVariable("recipientName", valueOrDefault(recipient.name(), "client"));
        context.setVariable("subscriptionNumber", subscription.getSubscriptionNumber());
        context.setVariable("planName", subscription.getPlanVersion() == null ? "-" : subscription.getPlanVersion().getName());
        context.setVariable("status", subscription.getStatus() == null ? "-" : subscription.getStatus().name());
        context.setVariable("subscriberType", subscription.getSubscriberType() == null ? "-" : subscription.getSubscriberType().name());
        context.setVariable("subscriberCode", subscription.getSubscriberCode());
        context.setVariable("startDate", subscription.getStartDate());
        context.setVariable("currentPeriodStart", subscription.getCurrentPeriodStart());
        context.setVariable("currentPeriodEnd", subscription.getCurrentPeriodEnd());
        context.setVariable("nextBillingDate", subscription.getNextBillingDate());
        context.setVariable("billingCycle", subscription.getBillingCycle() == null ? "-" : subscription.getBillingCycle().name());
        context.setVariable("amount", money(subscription.getTotalAmount(), subscription.getCurrency()));
        context.setVariable("reason", subscription.getCancellationReason());
        context.setVariable("eventType", eventType.name());
        return context;
    }

    private Recipient resolveRecipient(Subscription subscription) {
        Member member = subscription.getMember();
        if (member != null) {
            return new Recipient(valueOrDefault(member.getDisplayName(), member.getMemberId()), member.getEmail());
        }
        Customer customer = subscription.getCustomer();
        if (customer != null) {
            String name = customer.getType() == CustomerType.COMPANY
                    ? customer.getCompanyName()
                    : ((customer.getFirstname() == null ? "" : customer.getFirstname()) + " " + (customer.getLastname() == null ? "" : customer.getLastname())).trim();
            String email = StringUtils.hasText(customer.getBillingEmail()) ? customer.getBillingEmail() : customer.getEmail();
            return new Recipient(valueOrDefault(name, customer.getCustomerId()), email);
        }
        BusinessEntity business = subscription.getBusinessEntity();
        if (business != null) {
            return new Recipient(valueOrDefault(business.getName(), business.getCode()), business.getEmail());
        }
        return new Recipient(subscription.getSubscriberCode(), null);
    }

    private String template(SubscriptionEventType eventType) {
        return switch (eventType) {
            case SUBSCRIPTION_ACTIVATED -> "subscription-activated";
            case SUBSCRIPTION_CANCELLED -> "subscription-cancelled";
            case SUBSCRIPTION_RENEWED   -> "subscription-activated";
            default                     -> "subscription-created";
        };
    }

    private String subject(Subscription subscription, SubscriptionEventType eventType) {
        String number = subscription.getSubscriptionNumber();
        return switch (eventType) {
            case SUBSCRIPTION_ACTIVATED -> "Abonnement active " + number;
            case SUBSCRIPTION_CANCELLED -> "Abonnement annule " + number;
            case SUBSCRIPTION_RENEWED   -> "Abonnement renouvele " + number;
            default                     -> "Abonnement cree " + number;
        };
    }

    private String money(BigDecimal amount, String currency) {
        String value = amount == null ? "0" : amount.stripTrailingZeros().toPlainString();
        return value + " " + valueOrDefault(currency, "");
    }

    private String valueOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private record Recipient(String name, String email) {
    }
}
