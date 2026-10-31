package com.sni.bokaticowork.features.company.sharing.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.company.membership.repository.CustomerMemberLinkRepository;
import com.sni.bokaticowork.features.company.membership.service.MemberBillingCompanyResolver;
import com.sni.bokaticowork.features.company.sharing.repository.CompanySeatRepository;
import com.sni.bokaticowork.features.subscription.seat.model.SubscriptionSeat;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Les places d'un membre sur les abonnements d'une organisation · celles qu'une reservation peut utiliser.
 *
 * <p>Une place ne sert que si le membre est encore rattache a l'organisation et que l'abonnement est
 * actif sur sa periode. Quand plusieurs organisations sont possibles, le choix se fait ainsi :</p>
 * <ol>
 *   <li>l'organisation demandee a la reservation ({@code billingCompanyCode}) ; {@code NONE} n'en prend aucune ;</li>
 *   <li>sinon celle dont le lien est en mode automatique, si le membre y a une place ;</li>
 *   <li>sinon la seule organisation ou il a une place ;</li>
 *   <li>sinon le membre doit choisir.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class CompanySeatLocator {

    private final CompanySeatRepository seatRepository;
    private final CustomerMemberLinkRepository linkRepository;
    private final MemberBillingCompanyResolver billingCompanyResolver;

    /** Les places utilisables, toutes sur la meme organisation · vide s'il n'y en a pas. */
    @Transactional(readOnly = true)
    public List<SubscriptionSeat> usableSeats(String memberId, String requestedCompanyCode) {
        if (!StringUtils.hasText(memberId)) {
            return List.of();
        }
        String requested = StringUtils.hasText(requestedCompanyCode) ? requestedCompanyCode.trim() : null;
        if (MemberBillingCompanyResolver.NO_COMPANY.equalsIgnoreCase(requested)) {
            return List.of();
        }
        Set<String> linked = new HashSet<>();
        linkRepository.findActiveCompanyCodes(memberId.trim()).forEach(code -> linked.add(code.toUpperCase()));
        LocalDate today = LocalDate.now();
        Map<String, List<SubscriptionSeat>> byCompany = new LinkedHashMap<>();
        for (SubscriptionSeat seat : seatRepository.findActiveCompanySeatsOfMember(memberId.trim())) {
            Subscription subscription = seat.getSubscription();
            String company = subscription.getSubscriberCode();
            if (company == null || !linked.contains(company.toUpperCase()) || !current(subscription, today)) {
                continue;
            }
            byCompany.computeIfAbsent(company.toUpperCase(), k -> new java.util.ArrayList<>()).add(seat);
        }
        if (byCompany.isEmpty()) {
            return List.of();
        }
        if (requested != null) {
            return byCompany.getOrDefault(requested.toUpperCase(), List.of());
        }
        String automatic = billingCompanyResolver.resolve("MEMBER", memberId.trim(), null)
                .map(c -> c.getCustomerId().toUpperCase()).orElse(null);
        if (automatic != null && byCompany.containsKey(automatic)) {
            return byCompany.get(automatic);
        }
        if (byCompany.size() == 1) {
            return byCompany.values().iterator().next();
        }
        throw new BadRequestException("Vous avez une place sur l'abonnement de plusieurs organisations ("
                + String.join(", ", byCompany.keySet())
                + ") : choisissez celle pour laquelle vous réservez (billingCompanyCode).");
    }

    static boolean current(Subscription subscription, LocalDate today) {
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            return false;
        }
        boolean started = subscription.getCurrentPeriodStart() != null && !subscription.getCurrentPeriodStart().isAfter(today);
        boolean notEnded = subscription.getCurrentPeriodEnd() == null || !subscription.getCurrentPeriodEnd().isBefore(today);
        return started && notEnded;
    }
}
