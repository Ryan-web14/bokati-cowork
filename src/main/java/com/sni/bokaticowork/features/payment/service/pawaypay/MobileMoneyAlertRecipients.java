package com.sni.bokaticowork.features.payment.service.pawaypay;

import com.sni.bokaticowork.core.utils.mail.Recipients;
import com.sni.bokaticowork.features.payment.provider.pawaypay.PawapayProperties;
import com.sni.bokaticowork.security.admin.role.repository.RoleUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Qui recoit les alertes mobile money · l'adresse configuree, et a defaut les super-administrateurs.
 *
 * <p>{@code PAWAYPAY_ALERT_EMAIL} est vide en production : un paiement reste sans reponse de
 * l'operateur, l'alerte « a rapprocher » partait vers personne, et la facture restait bloquee sans
 * que quiconque le sache. Sans adresse configuree, les super-administrateurs actifs la recoivent.</p>
 */
@Component
@RequiredArgsConstructor
public class MobileMoneyAlertRecipients {

    private final PawapayProperties properties;
    private final RoleUserRepository roleUserRepository;

    public List<String> recipients() {
        Set<String> recipients = new LinkedHashSet<>(Recipients.split(properties.getAlertEmail()));
        if (recipients.isEmpty()) {
            recipients.addAll(roleUserRepository.findEnabledHolderEmails("SUPER_ADMIN"));
        }
        return List.copyOf(recipients);
    }

    /** Les super-administrateurs et l'adresse configuree · pour une decision qu'ils doivent tous voir. */
    public List<String> recipientsWithSuperAdmins() {
        Set<String> recipients = new LinkedHashSet<>(Recipients.split(properties.getAlertEmail()));
        recipients.addAll(roleUserRepository.findEnabledHolderEmails("SUPER_ADMIN"));
        return List.copyOf(recipients);
    }
}
