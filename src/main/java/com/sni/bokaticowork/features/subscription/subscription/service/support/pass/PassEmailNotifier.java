package com.sni.bokaticowork.features.subscription.subscription.service.support.pass;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionOwnerResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PassEmailNotifier {

    private final DefaultEmailSender emailSender;
    private final SubscriptionOwnerResolver ownerResolver;

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
        String email = resolveEmail(pass);
        if (!StringUtils.hasText(email)) return;
        try {
            emailSender.sendEmail(email, subjectPrefix + pass.getName(),
                    buildTextBody(pass));
        } catch (Exception ex) {
            log.warn("Failed to send email for pass {}", pass.getPassNumber(), ex);
        }
    }

    private String resolveEmail(Pass pass) {
        try {
            var owner = ownerResolver.resolve(pass.getOwnerType(), pass.getOwnerCode());
            if (owner.member() != null) return owner.member().getEmail();
            if (owner.customer() != null) return owner.customer().getEmail();
            if (owner.businessEntity() != null) return owner.businessEntity().getEmail();
        } catch (Exception ignored) {}
        return null;
    }

    private String buildTextBody(Pass pass) {
        return "Pass : " + pass.getPassNumber()
                + "\nNom : " + pass.getName()
                + "\nStatut : " + pass.getStatus().name()
                + "\nValide du : " + pass.getValidFrom()
                + "\nValide jusqu'au : " + pass.getValidUntil();
    }
}
