package com.sni.bokaticowork.features.document.kyc.service.support;

import com.sni.bokaticowork.core.communication.whatsapp.WhatsAppNotifier;
import com.sni.bokaticowork.core.communication.whatsapp.WhatsAppPortalPaths;
import com.sni.bokaticowork.core.enums.EventType;
import com.sni.bokaticowork.core.settings.userSettings.service.MemberPreferences;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Lot KG7 : WhatsApp « pieces demandees », en plus du courriel, quand une piece manque ou est
 * renvoyee en correction. Modele {@code kyc_pieces_demandees} (objet {@code KYC_DOCUMENTS_REQUESTED}) :
 * prenom, pieces, reference du dossier ; bouton vers la page KYC de l'espace client. Rien ne part
 * tant que le modele n'est pas approuve et enregistre, ni si le membre a coupe ce message.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KycWhatsAppNotifier {

    public static final String PURPOSE = "KYC_DOCUMENTS_REQUESTED";
    static final Set<String> EVENTS = Set.of("KYC_CASE_REQUIREMENT_ADDED", "KYC_CASE_LEVEL_CHANGED",
            "KYC_CASE_CORRECTION_REQUESTED", "KYC_DOCUMENT_CORRECTION_REQUESTED");

    private final WhatsAppNotifier whatsApp;
    private final MemberRepository memberRepository;
    private final CustomerRepository customerRepository;
    private final MemberPreferences memberPreferences;

    public void notify(String eventType, DocumentOwnerType ownerType, Long ownerId, String caseCode, List<String> pieces) {
        if (!EVENTS.contains(eventType) || ownerType == null || ownerId == null || !whatsApp.active()) {
            return;
        }
        try {
            String phone;
            String name;
            if (ownerType == DocumentOwnerType.MEMBER) {
                Optional<Member> member = memberRepository.findById(ownerId);
                if (member.isEmpty()) {
                    return;
                }
                if (!memberPreferences.allows(member.get(), EventType.KYC_DOCUMENTS_REQUESTED,
                        com.sni.bokaticowork.core.enums.NotificationChannel.WHATSAPP)) {
                    log.info("WhatsApp KYC du dossier {} non envoye : preference du membre", caseCode);
                    return;
                }
                phone = first(member.get().getWhatsappPhone(), member.get().getPhone());
                name = member.get().getDisplayName();
            } else if (ownerType == DocumentOwnerType.CUSTOMER) {
                var customer = customerRepository.findById(ownerId).orElse(null);
                if (customer == null) {
                    return;
                }
                phone = first(customer.getWhatsappPhone(), customer.getPhone());
                name = null;
            } else {
                return;
            }
            if (!StringUtils.hasText(phone)) {
                return;
            }
            whatsApp.notify(phone, PURPOSE, values(name, pieces, caseCode), caseCode, WhatsAppPortalPaths.KYC);
        } catch (RuntimeException ex) {
            log.warn("WhatsApp KYC du dossier {} non envoye : {}", caseCode, ex.getMessage());
        }
    }

    /** Prenom, pieces demandees, reference du dossier (ordre des variables du modele). */
    static List<String> values(String name, List<String> pieces, String caseCode) {
        String first = StringUtils.hasText(name) ? name.trim().split("\\s+")[0] : "client";
        String list = pieces == null || pieces.isEmpty() ? "voir votre espace client" : String.join(", ", pieces);
        return List.of(first, list, caseCode == null ? "-" : caseCode);
    }

    private static String first(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }
}
