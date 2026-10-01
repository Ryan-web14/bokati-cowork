package com.sni.bokaticowork.features.domiciliation.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.domiciliation.model.ServiceDefinition;
import com.sni.bokaticowork.features.domiciliation.model.SubscriptionService;
import com.sni.bokaticowork.features.domiciliation.repository.ServiceDefinitionRepository;
import com.sni.bokaticowork.features.domiciliation.repository.SubscriptionServiceRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriberKycLevelGuard;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionOwnerResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Les services souscrits · ce qu'un abonnement contient au-dela de son plan.
 *
 * <p>Souscrire un service n'est pas le rendre. Un service qui exige un contrat reste
 * {@code PENDING} jusqu'a ce que le contrat soit la ; c'est le contrat qui l'active. Un service
 * sans obligation particuliere est actif des la souscription. La difference est une donnee du
 * service, pas une branche du code.</p>
 */
@Service
@RequiredArgsConstructor
public class SubscribedServiceService {

    private final ServiceDefinitionRepository definitionRepository;
    private final SubscriptionServiceRepository serviceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionOwnerResolver ownerResolver;
    private final SubscriberKycLevelGuard kycGuard;
    private final SequenceGeneratorFacade sequenceGenerator;

    // ---- Catalogue ----------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ServiceDefinition> catalogue(boolean includeInactive) {
        return includeInactive ? definitionRepository.findAllByOrderByNameAsc() : definitionRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public ServiceDefinition definition(String code) {
        return definitionRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Service introuvable · " + code));
    }

    @Transactional
    public ServiceDefinition saveDefinition(ServiceDefinition definition) {
        if (!StringUtils.hasText(definition.getCode()) || !StringUtils.hasText(definition.getName())
                || definition.getServiceCategory() == null) {
            throw new BadRequestException("Un service a un code, un nom et une catégorie");
        }
        ServiceDefinition existing = definitionRepository.findByCode(definition.getCode()).orElse(null);
        if (existing == null) {
            return definitionRepository.save(definition);
        }
        existing.setName(definition.getName());
        existing.setDescription(definition.getDescription());
        existing.setServiceCategory(definition.getServiceCategory());
        existing.setDeliveryMode(definition.getDeliveryMode());
        existing.setRequiresContract(definition.getRequiresContract());
        existing.setRequiresKycLevel(definition.getRequiresKycLevel());
        existing.setRequiresPhysicalResource(definition.getRequiresPhysicalResource());
        existing.setHasRegulatoryObligations(definition.getHasRegulatoryObligations());
        existing.setDefaultBillingCycle(definition.getDefaultBillingCycle());
        existing.setDefaultNoticePeriodDays(definition.getDefaultNoticePeriodDays());
        existing.setDefaultCommitmentMonths(definition.getDefaultCommitmentMonths());
        existing.setUnitPrice(definition.getUnitPrice());
        existing.setCurrency(definition.getCurrency());
        existing.setUsageEntitlementCode(definition.getUsageEntitlementCode());
        existing.setActive(definition.getActive());
        return definitionRepository.save(existing);
    }

    // ---- Souscription -------------------------------------------------------------------------

    /**
     * Ajoute un service a un abonnement.
     *
     * <p>Le niveau de verification exige par le service est controle ici, avec le message qui dit de
     * quel service il s'agit. Un service deja souscrit et non termine ne se souscrit pas deux fois ·
     * on augmente sa quantite, ou on le termine d'abord.</p>
     */
    @Transactional
    public SubscriptionService subscribe(String subscriptionNumber, String serviceCode, Integer quantity,
                                         BigDecimal unitPrice, String metadataJson, String actor) {
        Subscription subscription = subscriptionRepository.findBySubscriptionNumber(subscriptionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement introuvable"));
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED || subscription.getStatus() == SubscriptionStatus.EXPIRED) {
            throw new ConflictException("subscription", "cet abonnement est terminé · un service ne s'y ajoute plus");
        }
        ServiceDefinition definition = definition(serviceCode);
        if (!Boolean.TRUE.equals(definition.getActive())) {
            throw new BadRequestException("Ce service n'est plus proposé");
        }
        serviceRepository.findFirstBySubscription_IdAndServiceDefinition_CodeAndStatusNot(
                        subscription.getId(), definition.getCode(), SubscriptionService.Status.TERMINATED)
                .ifPresent(existing -> {
                    throw new ConflictException("service", "ce service est déjà souscrit sur cet abonnement · " + existing.getServiceNumber());
                });

        SubscriptionOwnerResolver.Owner owner = ownerResolver.resolve(subscription.getSubscriberType(), subscription.getSubscriberCode());
        kycGuard.require(definition.getRequiresKycLevel(), owner, "service « " + definition.getName() + " »");

        boolean activeNow = !Boolean.TRUE.equals(definition.getRequiresContract())
                && !Boolean.TRUE.equals(definition.getRequiresPhysicalResource());
        SubscriptionService service = SubscriptionService.builder()
                .serviceNumber(sequenceGenerator.next("subscription_service"))
                .subscription(subscription)
                .serviceDefinition(definition)
                .status(activeNow ? SubscriptionService.Status.ACTIVE : SubscriptionService.Status.PENDING)
                .quantity(quantity == null || quantity < 1 ? 1 : quantity)
                .unitPrice(unitPrice != null ? unitPrice : definition.getUnitPrice())
                .currency(StringUtils.hasText(subscription.getCurrency()) ? subscription.getCurrency() : definition.getCurrency())
                .activatedAt(activeNow ? Instant.now() : null)
                .serviceMetadataJson(metadataJson)
                .createdBy(actor)
                .build();
        return serviceRepository.save(service);
    }

    /** Ce que le contrat ou la ressource appelle quand le service peut enfin etre rendu. */
    @Transactional
    public SubscriptionService activate(SubscriptionService service) {
        if (service.getStatus() == SubscriptionService.Status.TERMINATED) {
            throw new ConflictException("service", "un service terminé ne se réactive pas · on le souscrit à nouveau");
        }
        service.setStatus(SubscriptionService.Status.ACTIVE);
        service.setActivatedAt(service.getActivatedAt() == null ? Instant.now() : service.getActivatedAt());
        service.setSuspendedAt(null);
        return serviceRepository.save(service);
    }

    @Transactional
    public SubscriptionService suspend(String serviceNumber, String reason) {
        SubscriptionService service = get(serviceNumber);
        if (service.getStatus() != SubscriptionService.Status.ACTIVE) {
            throw new ConflictException("service", "seul un service actif se suspend");
        }
        service.setStatus(SubscriptionService.Status.SUSPENDED);
        service.setSuspendedAt(Instant.now());
        return serviceRepository.save(service);
    }

    @Transactional
    public SubscriptionService terminate(SubscriptionService service) {
        service.setStatus(SubscriptionService.Status.TERMINATED);
        service.setTerminatedAt(Instant.now());
        return serviceRepository.save(service);
    }

    @Transactional
    public SubscriptionService terminate(String serviceNumber) {
        return terminate(get(serviceNumber));
    }

    @Transactional(readOnly = true)
    public SubscriptionService get(String serviceNumber) {
        return serviceRepository.findByServiceNumber(serviceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Service souscrit introuvable"));
    }

    @Transactional(readOnly = true)
    public List<SubscriptionService> ofSubscription(String subscriptionNumber) {
        Subscription subscription = subscriptionRepository.findBySubscriptionNumber(subscriptionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement introuvable"));
        return serviceRepository.findBySubscription_IdOrderByCreatedAtAsc(subscription.getId());
    }
}
