package com.sni.bokaticowork.features.subscription.subscription.service.support.pass;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.features.billing.service.support.MemberBillingContact;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionOwnerResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PassEmailNotifier {

    private final DefaultEmailSender emailSender;
    private final SubscriptionOwnerResolver ownerResolver;

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private MemberBillingContact memberContact;

    /** Absent : adresse du compte et fuseau de l'exploitant. */
    @Autowired(required = false)
    void setMemberContact(MemberBillingContact memberContact) {
        this.memberContact = memberContact;
    }

    @Async
    public void notifyCreated(Pass pass) { send(pass, "Votre pass a été créé · "); }

    @Async
    public void notifyActivated(Pass pass) { send(pass, "Votre pass est actif · "); }

    @Async
    public void notifyRenewed(Pass pass) { send(pass, "Votre pass a été renouvelé · "); }

    @Async
    public void notifyCancelled(Pass pass, String reason) { send(pass, "Votre pass a été annulé · "); }

    @Async
    public void notifyPastDue(Pass pass) { send(pass, "Échec renouvellement pass · "); }

    private void send(Pass pass, String subjectPrefix) {
        Member member = resolveMember(pass);
        String email = resolveEmail(pass, member);
        if (!StringUtils.hasText(email)) return;
        try {
            emailSender.sendEmail(email, subjectPrefix + pass.getName(),
                    buildTextBody(pass, zone(member)));
        } catch (Exception ex) {
            log.warn("Failed to send email for pass {}", pass.getPassNumber(), ex);
        }
    }

    /** Le membre destinataire : le titulaire membre, ou le membre d'un client personne. */
    private Member resolveMember(Pass pass) {
        try {
            var owner = ownerResolver.resolve(pass.getOwnerType(), pass.getOwnerCode());
            if (owner.member() != null) return owner.member();
            if (owner.customer() != null && memberContact != null) {
                return memberContact.memberOf(owner.customer()).orElse(null);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String resolveEmail(Pass pass, Member member) {
        try {
            var owner = ownerResolver.resolve(pass.getOwnerType(), pass.getOwnerCode());
            // Adresse de facturation du membre d'abord, a defaut celle du compte.
            if (owner.member() != null) return billingRecipient(member, owner.member().getEmail());
            if (owner.customer() != null) return billingRecipient(member, owner.customer().getEmail());
            if (owner.businessEntity() != null) return owner.businessEntity().getEmail();
        } catch (Exception ignored) {}
        return null;
    }

    private String billingRecipient(Member member, String email) {
        return member == null || memberContact == null ? email : memberContact.recipient(member, email);
    }

    private ZoneId zone(Member member) {
        return memberContact == null ? MemberBillingContact.defaultZone() : memberContact.zone(member);
    }

    private String buildTextBody(Pass pass, ZoneId zone) {
        return "Pass : " + pass.getPassNumber()
                + "\nNom : " + pass.getName()
                + "\nStatut : " + pass.getStatus().name()
                + "\nValide du : " + format(pass.getValidFrom(), zone)
                + "\nValide jusqu'au : " + format(pass.getValidUntil(), zone);
    }

    /** Date et heure dans le fuseau du destinataire · un Instant brut se lisait en UTC. */
    private String format(Instant value, ZoneId zone) {
        return value == null ? "-" : DATE_TIME.format(value.atZone(zone));
    }
}
