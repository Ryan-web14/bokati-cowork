package com.sni.bokaticowork.features.subscription.subscription.service.support;

import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.company.repository.BusinessRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Identites d'un meme souscripteur : une entreprise a un profil client ({@code CUS-COM-...}, type
 * {@code CUSTOMER}) relie a son entite ({@code BUSINESS_ENTITY}). Un abonnement peut etre au nom de
 * l'un ou de l'autre ; chercher par l'un doit trouver ceux de l'autre.
 *
 * <p>Avant, chercher {@code BUSINESS_ENTITY} avec le code client de l'entreprise ne trouvait rien,
 * ses abonnements etant enregistres au nom du profil client.</p>
 */
@Component
@RequiredArgsConstructor
public class SubscriberIdentities {

    /** Une identite : type de souscripteur et code. */
    public record Identity(SubscriberType type, String code) {
    }

    private final CustomerRepository customerRepository;
    private final BusinessRepository businessRepository;

    /** Le souscripteur et ses autres identites ; un membre n'en a qu'une. */
    public List<Identity> of(SubscriberType type, String code) {
        Set<Identity> identities = new LinkedHashSet<>();
        if (!StringUtils.hasText(code)) {
            return List.of();
        }
        String trimmed = code.trim();
        if (type == SubscriberType.MEMBER) {
            return List.of(new Identity(type, trimmed));
        }
        if (type != null) {
            identities.add(new Identity(type, trimmed));
        }
        customerRepository.findByCustomerId(trimmed).ifPresent(customer -> {
            identities.add(new Identity(SubscriberType.CUSTOMER, customer.getCustomerId()));
            if (customer.getBusinessEntity() != null && StringUtils.hasText(customer.getBusinessEntity().getCode())) {
                identities.add(new Identity(SubscriberType.BUSINESS_ENTITY, customer.getBusinessEntity().getCode()));
            }
        });
        businessRepository.findByCode(trimmed).ifPresent(business -> {
            identities.add(new Identity(SubscriberType.BUSINESS_ENTITY, business.getCode()));
            for (Customer customer : customerRepository.findAllByBusinessEntityId(business.getId())) {
                identities.add(new Identity(SubscriberType.CUSTOMER, customer.getCustomerId()));
            }
        });
        return List.copyOf(identities);
    }
}
