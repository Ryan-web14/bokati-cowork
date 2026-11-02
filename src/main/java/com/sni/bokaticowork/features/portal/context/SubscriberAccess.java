package com.sni.bokaticowork.features.portal.context;

import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceAvailabilityService;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Le droit d'un membre aux creneaux du soir, reserves aux abonnes.
 *
 * <p>Abonne veut dire : un abonnement actif sur la periode en cours, au nom du membre ou du client
 * auquel il est rattache (l'abonnement d'une entreprise couvre ses membres). C'est la meme
 * definition que celle qu'applique deja le paiement par abonnement d'une reservation.</p>
 *
 * <p>Ne concerne que le portail · l'accueil reserve les soirees pour n'importe qui, et ne passe
 * pas par ici.</p>
 */
@Component
@RequiredArgsConstructor
public class SubscriberAccess {

    static final String RESERVED_MESSAGE =
            "Ce créneau est réservé aux abonnés. Souscrivez un abonnement ou adressez-vous à l'accueil.";

    private final SubscriptionRepository subscriptionRepository;
    private final ResourceAvailabilityService availabilityService;
    private final MemberRepository memberRepository;
    private final com.sni.bokaticowork.features.company.membership.repository.CustomerMemberLinkRepository linkRepository;

    @Transactional(readOnly = true)
    public boolean isSubscriber(Member member) {
        if (member == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        if (StringUtils.hasText(member.getMemberId())
                && !subscriptionRepository.findAllCurrentActive("MEMBER", member.getMemberId(), today).isEmpty()) {
            return true;
        }
        // Le membre arrive detache · son client n'est qu'une reference paresseuse, et la lire ici
        // levait LazyInitializationException : calendrier et creneaux du portail en 500. Le code
        // du client se lit par requete, dans la transaction de cette methode.
        if (!StringUtils.hasText(member.getMemberId())) {
            return false;
        }
        boolean viaPayer = memberRepository.findCustomerIdByMemberId(member.getMemberId())
                .filter(StringUtils::hasText)
                .map(customerId -> !subscriptionRepository.findAllCurrentActive("CUSTOMER", customerId, today).isEmpty())
                .orElse(false);
        if (viaPayer) {
            return true;
        }
        // L'abonnement de chaque organisation ou le membre est rattache et actif le couvre aussi.
        return linkRepository.findActiveCompanyCodes(member.getMemberId()).stream()
                .anyMatch(code -> !subscriptionRepository.findAllCurrentActive("CUSTOMER", code, today).isEmpty());
    }

    /** La plage touche-t-elle une soiree que ce membre n'a pas le droit de reserver. */
    @Transactional(readOnly = true)
    public boolean isDenied(Member member, String resourceCode, LocalDateTime startedAt, LocalDateTime endedAt) {
        return availabilityService.touchesSubscribersOnly(resourceCode, startedAt, endedAt) && !isSubscriber(member);
    }

    public void assertMayBook(Member member, String resourceCode, LocalDateTime startedAt, LocalDateTime endedAt) {
        if (isDenied(member, resourceCode, startedAt, endedAt)) {
            throw new ForbiddenException(RESERVED_MESSAGE);
        }
    }
}
