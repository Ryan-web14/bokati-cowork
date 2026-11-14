package com.sni.bokaticowork.features.subscription.subscription.service.support.subscription;

import com.sni.bokaticowork.core.communication.whatsapp.WhatsAppNotifier;
import com.sni.bokaticowork.features.billing.service.support.MemberBillingContact;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionEventType;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * WhatsApp {@code abonnement_actif} a l'activation et au renouvellement d'un abonnement, en plus du
 * courriel. Les valeurs sont lues tout de suite (entites encore attachees) ; l'envoi part apres la
 * validation de la transaction.
 */
@Slf4j
@Component
public class SubscriptionWhatsAppNotifier {

    static final String PURPOSE = "SUBSCRIPTION_ACTIVATED";
    static final String PORTAL_PATH = "abonnements";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final WhatsAppNotifier whatsApp;
    private MemberBillingContact memberContact;

    public SubscriptionWhatsAppNotifier(WhatsAppNotifier whatsApp) {
        this.whatsApp = whatsApp;
    }

    @Autowired(required = false)
    void setMemberContact(MemberBillingContact memberContact) {
        this.memberContact = memberContact;
    }

    public void notify(Subscription subscription, SubscriptionEventType eventType) {
        if (subscription == null || (eventType != SubscriptionEventType.SUBSCRIPTION_ACTIVATED
                && eventType != SubscriptionEventType.SUBSCRIPTION_RENEWED) || !whatsApp.active()) {
            return;
        }
        Optional<Recipient> recipient = recipient(subscription);
        if (recipient.isEmpty()) {
            return;
        }
        whatsApp.notify(recipient.get().phone(), PURPOSE, values(subscription, recipient.get().name()),
                subscription.getSubscriptionNumber(), PORTAL_PATH);
    }

    /** Prenom (ou raison sociale), formule, debut et fin de la periode en cours. */
    static List<String> values(Subscription subscription, String name) {
        String plan = subscription.getPlanVersion() == null || !StringUtils.hasText(subscription.getPlanVersion().getName())
                ? subscription.getSubscriptionNumber() : subscription.getPlanVersion().getName();
        LocalDate start = subscription.getCurrentPeriodStart() != null ? subscription.getCurrentPeriodStart() : subscription.getStartDate();
        LocalDate end = subscription.getCurrentPeriodEnd();
        return List.of(name, plan, start == null ? "-" : DATE.format(start), end == null ? "-" : DATE.format(end));
    }

    record Recipient(String name, String phone) {
    }

    private Optional<Recipient> recipient(Subscription subscription) {
        Member member = subscription.getMember();
        if (member != null) {
            return MemberBillingContact.whatsAppPhone(member).map(phone -> new Recipient(firstName(member.getDisplayName()), phone));
        }
        Customer customer = subscription.getCustomer();
        if (customer != null) {
            boolean company = customer.getType() == CustomerType.COMPANY;
            String name = company ? customer.getCompanyName() : firstName(customer.getFirstname());
            Optional<String> phone = MemberBillingContact.firstPhone(customer.getWhatsappPhone(), customer.getPhone());
            if (phone.isEmpty() && memberContact != null) {
                phone = memberContact.memberOf(customer).flatMap(MemberBillingContact::whatsAppPhone);
            }
            return phone.map(found -> new Recipient(StringUtils.hasText(name) ? name.trim() : "client", found));
        }
        BusinessEntity business = subscription.getBusinessEntity();
        if (business != null) {
            return MemberBillingContact.firstPhone(business.getPhone())
                    .map(phone -> new Recipient(StringUtils.hasText(business.getName()) ? business.getName().trim() : "client", phone));
        }
        return Optional.empty();
    }

    private static String firstName(String name) {
        return StringUtils.hasText(name) ? name.trim().split("\\s+")[0] : "client";
    }
}
