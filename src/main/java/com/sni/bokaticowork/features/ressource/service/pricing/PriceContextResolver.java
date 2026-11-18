package com.sni.bokaticowork.features.ressource.service.pricing;

import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Lot R1 : contexte de prix d'un titulaire, le meme pour le devis et pour la reservation.
 *
 * <ul>
 *   <li>Segments : MEMBER pour un membre ; COMPANY pour une entreprise ou un client entreprise ;
 *   PUBLIC pour un client particulier hors membre ; SUBSCRIBER en plus avec un abonnement en cours
 *   (et le code de son plan).</li>
 *   <li>Canal : PORTAL pour une requete de l'espace client ({@code /client/}, {@code /portal/}),
 *   TEAM sinon (back-office) ; nul hors requete (taches).</li>
 * </ul>
 */
@Slf4j
@Component
public class PriceContextResolver {

    static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");

    private CustomerRepository customerRepository;
    private SubscriptionRepository subscriptionRepository;

    @Autowired(required = false)
    void setCustomerRepository(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    private com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository memberRepository;
    private com.sni.bokaticowork.features.company.membership.repository.CustomerMemberLinkRepository memberLinks;

    @Autowired(required = false)
    void setMemberLinks(com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository memberRepository,
                        com.sni.bokaticowork.features.company.membership.repository.CustomerMemberLinkRepository memberLinks) {
        this.memberRepository = memberRepository;
        this.memberLinks = memberLinks;
    }

    /**
     * Lot B10 : identites du client · lui-meme, et pour un membre les organisations auxquelles il
     * est rattache (liens actifs), sous la forme « CUSTOMER:code ».
     */
    private Set<String> owners(String type, String ownerCode) {
        Set<String> owners = new LinkedHashSet<>();
        if (type == null || !StringUtils.hasText(ownerCode)) {
            return owners;
        }
        String normalized = "BUSINESS".equals(type) ? "BUSINESS_ENTITY" : type;
        owners.add(normalized + ":" + ownerCode.trim());
        if ("MEMBER".equals(normalized) && memberRepository != null && memberLinks != null) {
            try {
                memberRepository.findByMemberIdAndDeletedFalse(ownerCode.trim()).ifPresent(member ->
                        memberLinks.findAllByMemberAndStatusOrderByStartedAtDesc(member,
                                        com.sni.bokaticowork.features.company.enums.MemberLinkStatus.ACTIVE)
                                .forEach(link -> {
                                    if (link.getCustomer() != null && link.getCustomer().getCustomerId() != null) {
                                        owners.add("CUSTOMER:" + link.getCustomer().getCustomerId());
                                    }
                                }));
            } catch (RuntimeException ex) {
                log.warn("Organisations du membre {} illisibles : {}", ownerCode, ex.getMessage());
            }
        }
        return owners;
    }

    @Autowired(required = false)
    void setSubscriptionRepository(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    /**
     * @param ownerType MEMBER, CUSTOMER ou BUSINESS_ENTITY ; nul pour un visiteur
     * @param ownerCode code du titulaire
     */
    public PriceContext forOwner(String ownerType, String ownerCode) {
        Set<String> segments = new LinkedHashSet<>();
        String planCode = null;
        String type = ownerType == null ? null : ownerType.trim().toUpperCase(Locale.ROOT);
        if (type == null || !StringUtils.hasText(ownerCode)) {
            segments.add("PUBLIC");
        } else {
            switch (type) {
                case "MEMBER" -> segments.add("MEMBER");
                case "BUSINESS_ENTITY", "BUSINESS" -> segments.add("COMPANY");
                case "CUSTOMER" -> {
                    var customer = customerRepository == null ? null : customerRepository.findByCustomerId(ownerCode.trim()).orElse(null);
                    boolean company = customer != null && customer.getType() != null && "COMPANY".equals(customer.getType().name());
                    segments.add(company ? "COMPANY" : "PUBLIC");
                    if (customer != null && customer.isMember()) {
                        segments.add("MEMBER");
                    }
                }
                default -> segments.add("PUBLIC");
            }
            planCode = currentPlan("BUSINESS".equals(type) ? "BUSINESS_ENTITY" : type, ownerCode.trim());
            if (planCode != null) {
                segments.add("SUBSCRIBER");
            }
        }
        return new PriceContext(Set.copyOf(segments), "".equals(planCode) ? null : planCode, channel(), LocalDateTime.now(ZONE),
                owners(type, ownerCode));
    }

    /** Code du plan de l'abonnement en cours ; chaine vide si abonne sans plan lisible ; nul sans abonnement. */
    private String currentPlan(String subscriberType, String subscriberCode) {
        if (subscriptionRepository == null) {
            return null;
        }
        try {
            return subscriptionRepository.findCurrentActive(subscriberType, subscriberCode, LocalDate.now(ZONE))
                    .map(subscription -> {
                        var version = subscription.getPlanVersion();
                        var plan = version == null ? null : version.getPlan();
                        return plan == null || plan.getCode() == null ? "" : plan.getCode();
                    })
                    .orElse(null);
        } catch (RuntimeException ex) {
            log.warn("Abonnement de {} {} illisible pour le prix : {}", subscriberType, subscriberCode, ex.getMessage());
            return null;
        }
    }

    /** PORTAL pour l'espace client, TEAM pour le back-office, nul hors requete. */
    public static String channel() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        String uri = attributes.getRequest().getRequestURI();
        if (uri == null) {
            return null;
        }
        return uri.contains("/client/") || uri.contains("/portal/") ? "PORTAL" : "TEAM";
    }
}
