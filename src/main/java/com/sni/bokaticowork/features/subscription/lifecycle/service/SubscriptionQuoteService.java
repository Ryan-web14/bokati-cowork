package com.sni.bokaticowork.features.subscription.lifecycle.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.subscription.derivation.model.PlanDerivation;
import com.sni.bokaticowork.features.subscription.derivation.service.PlanDerivationService;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionCommitment;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionQuote;
import com.sni.bokaticowork.features.subscription.lifecycle.repository.SubscriptionQuoteRepository;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreateSubscriptionRequest;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionOwnerResolver;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionPlanResolver;
import com.sni.bokaticowork.features.subscription.subscription.service.support.subscription.SubscriptionCreationOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Le devis · un abonnement promis, converti a l'acceptation.
 *
 * <p>Il fige ce qui a ete propose : plan, rythme, prix, engagement, validite. A l'acceptation il
 * cree l'abonnement ; si le prix n'est pas celui du catalogue, il cree aussi la derivation qui
 * porte la negociation, avec ses regles (plancher, visa). Le devis ne contourne rien · il
 * anticipe.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionQuoteService {

    private final SubscriptionQuoteRepository quoteRepository;
    private final SubscriptionPlanResolver planResolver;
    private final SubscriptionOwnerResolver ownerResolver;
    private final SubscriptionPolicyService policyService;
    private final SubscriptionCommitmentService commitmentService;
    private final @Lazy SubscriptionCreationOperator creationOperator;
    private final @Lazy PlanDerivationService derivationService;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final OutboxService outboxService;

    public record Request(SubscriberType subscriberType, String subscriberCode, String planCode, String planVersionId,
                          BillingCycle billingCycle, BigDecimal quotedPrice, BigDecimal setupFee, Integer commitmentMonths,
                          Integer trialDays, LocalDate startDate, LocalDate validUntil, String notes, String proposalNumber) {
    }

    /** Ce que la conversion donnera · pour le lire avant d'envoyer. */
    public record Outlook(String quoteNumber, BigDecimal cataloguePrice, BigDecimal quotedPrice, BigDecimal discountPercent,
                          boolean negotiated, boolean requiresApproval, String blockingReason, Integer commitmentMonths, LocalDate validUntil) {
    }

    @Transactional
    public SubscriptionQuote create(Request request, String preparedBy) {
        if (request.subscriberType() == null || !StringUtils.hasText(request.subscriberCode())) {
            throw new BadRequestException("Le souscripteur est requis");
        }
        PlanVersion version = planResolver.resolvePlanVersion(request.planCode(), request.planVersionId());
        if (version.privateToSubscription()) {
            throw new BadRequestException("Un devis part d'une version de catalogue");
        }
        PlanPrice price = planResolver.resolvePrice(version, request.billingCycle());
        SubscriptionOwnerResolver.Owner owner = ownerResolver.resolve(request.subscriberType(), request.subscriberCode());
        BigDecimal quoted = request.quotedPrice() == null ? price.getAmount() : request.quotedPrice();
        if (quoted.signum() < 0) {
            throw new BadRequestException("Le prix proposé ne peut pas être négatif");
        }
        LocalDate validUntil = request.validUntil() != null ? request.validUntil()
                : LocalDate.now().plusDays(policyService.current().getQuoteValidityDays());
        if (!validUntil.isAfter(LocalDate.now())) {
            throw new BadRequestException("La validité du devis est dans le futur");
        }
        SubscriptionQuote quote = SubscriptionQuote.builder()
                .quoteNumber(sequenceGenerator.next("subscription_quote"))
                .subscriberType(request.subscriberType())
                .subscriberCode(owner.code())
                .subscriberName(ownerName(owner))
                .subscriberEmail(ownerEmail(owner))
                .planVersion(version)
                .billingCycle(price.getBillingCycle())
                .currency(price.getCurrency())
                .cataloguePrice(price.getAmount())
                .quotedPrice(quoted)
                .setupFee(request.setupFee() != null ? request.setupFee() : price.getSetupFee() == null ? BigDecimal.ZERO : price.getSetupFee())
                .commitmentMonths(request.commitmentMonths() != null ? request.commitmentMonths()
                        : price.getCommitmentMonths() == null ? 0 : price.getCommitmentMonths())
                .trialDays(request.trialDays() != null ? request.trialDays() : price.getTrialDays() == null ? 0 : price.getTrialDays())
                .startDate(request.startDate())
                .notes(trim(request.notes()))
                .validUntil(validUntil)
                .preparedBy(preparedBy)
                .proposalNumber(trim(request.proposalNumber()))
                .build();
        SubscriptionQuote saved = quoteRepository.save(quote);
        Outlook outlook = outlook(saved);
        if (outlook.blockingReason() != null) {
            throw new ConflictException("quote", outlook.blockingReason());
        }
        return saved;
    }

    /** La derivation qu'entrainerait ce devis · sans rien ecrire. */
    @Transactional(readOnly = true)
    public Outlook outlook(SubscriptionQuote quote) {
        if (!quote.negotiated()) {
            return new Outlook(quote.getQuoteNumber(), quote.getCataloguePrice(), quote.getQuotedPrice(), BigDecimal.ZERO, false, false, null,
                    quote.getCommitmentMonths(), quote.getValidUntil());
        }
        Subscription ghost = Subscription.builder().subscriptionNumber(quote.getQuoteNumber()).billingCycle(quote.getBillingCycle())
                .currency(quote.getCurrency()).build();
        PlanDerivationService.Preview preview = derivationService.simulateAgainst(ghost, quote.getPlanVersion(), derivationSpec(quote));
        return new Outlook(quote.getQuoteNumber(), quote.getCataloguePrice(), quote.getQuotedPrice(), preview.discountPercent(), true,
                preview.requiresApproval(), preview.blockingReason(), quote.getCommitmentMonths(), quote.getValidUntil());
    }

    @Transactional
    public SubscriptionQuote send(String quoteNumber) {
        SubscriptionQuote quote = get(quoteNumber);
        if (!quote.getStatus().open()) {
            throw new BadRequestException("Ce devis ne peut plus être envoyé · il est " + quote.getStatus());
        }
        if (quote.expiredOn(LocalDate.now())) {
            throw new ConflictException("quote", "ce devis a expiré le " + quote.getValidUntil());
        }
        quote.setStatus(SubscriptionQuote.Status.SENT);
        quote.setSentAt(Instant.now());
        if (StringUtils.hasText(quote.getSubscriberEmail())) {
            outboxService.publish("SUBSCRIPTION_QUOTE_SENT", "SUBSCRIPTION_QUOTE", quote.getQuoteNumber(), Map.of(
                    "recipientEmail", quote.getSubscriberEmail(),
                    "subject", "Votre devis " + quote.getQuoteNumber(),
                    "templateCode", "subscription_quote_sent",
                    "quoteNumber", quote.getQuoteNumber(),
                    "planName", quote.getPlanVersion().getName(),
                    "quotedPrice", quote.getQuotedPrice().toPlainString(),
                    "currency", quote.getCurrency(),
                    "billingCycle", quote.getBillingCycle().name(),
                    "commitmentMonths", String.valueOf(quote.getCommitmentMonths()),
                    "validUntil", quote.getValidUntil().toString()
            ));
        }
        return quoteRepository.save(quote);
    }

    /**
     * Accepte et convertit · l'abonnement est cree, la derivation posee si le prix est negocie,
     * l'engagement pose si le devis en porte un.
     */
    @Transactional
    public SubscriptionQuote accept(String quoteNumber, String acceptedBy) {
        SubscriptionQuote quote = get(quoteNumber);
        if (!quote.getStatus().open()) {
            throw new BadRequestException("Ce devis ne peut plus être accepté · il est " + quote.getStatus());
        }
        if (quote.expiredOn(LocalDate.now())) {
            quote.setStatus(SubscriptionQuote.Status.EXPIRED);
            quoteRepository.save(quote);
            throw new ConflictException("quote", "ce devis a expiré le " + quote.getValidUntil() + " · établissez-en un nouveau");
        }
        LocalDate start = quote.getStartDate() == null || quote.getStartDate().isBefore(LocalDate.now()) ? LocalDate.now() : quote.getStartDate();
        Subscription subscription = creationOperator.create(new CreateSubscriptionRequest(
                quote.getPlanVersion().getPlan().getCode(), String.valueOf(quote.getPlanVersion().getId()), quote.getBillingCycle(),
                quote.getSubscriberType(), quote.getSubscriberCode(), start, true,
                "{\"quoteNumber\":\"" + quote.getQuoteNumber() + "\"}", false));
        quote.setStatus(SubscriptionQuote.Status.CONVERTED);
        quote.setAcceptedAt(Instant.now());
        quote.setAcceptedBy(acceptedBy);
        quote.setConvertedSubscriptionNumber(subscription.getSubscriptionNumber());

        if (quote.negotiated()) {
            PlanDerivation derivation = derivationService.create(subscription, derivationSpec(quote), quote.getPreparedBy(), null);
            quote.setDerivationCode(derivation.getDerivationCode());
        }
        if (quote.getCommitmentMonths() != null && quote.getCommitmentMonths() > 0) {
            commitmentService.set(subscription, new SubscriptionCommitmentService.Spec(quote.getCommitmentMonths(), start, null, null, null, false),
                    SubscriptionCommitment.Source.QUOTE, acceptedBy);
        }
        log.info("Devis {} accepte par {} · abonnement {}{}", quoteNumber, acceptedBy, subscription.getSubscriptionNumber(),
                quote.getDerivationCode() == null ? "" : " · derivation " + quote.getDerivationCode());
        return quoteRepository.save(quote);
    }

    @Transactional
    public SubscriptionQuote reject(String quoteNumber, String reason) {
        SubscriptionQuote quote = get(quoteNumber);
        if (!quote.getStatus().open()) {
            throw new BadRequestException("Ce devis ne peut plus être refusé · il est " + quote.getStatus());
        }
        quote.setStatus(SubscriptionQuote.Status.REJECTED);
        quote.setRejectedAt(Instant.now());
        quote.setRejectionReason(trim(reason));
        return quoteRepository.save(quote);
    }

    /** Les devis ouverts dont la validite est passee · chaque jour. */
    @Transactional
    public int expireOpen() {
        List<SubscriptionQuote> expired = quoteRepository.findExpiredOpen(LocalDate.now());
        expired.forEach(q -> q.setStatus(SubscriptionQuote.Status.EXPIRED));
        quoteRepository.saveAll(expired);
        return expired.size();
    }

    @Transactional(readOnly = true)
    public SubscriptionQuote get(String quoteNumber) {
        return quoteRepository.findByQuoteNumber(quoteNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Devis introuvable"));
    }

    @Transactional(readOnly = true)
    public Page<SubscriptionQuote> list(SubscriptionQuote.Status status, Pageable pageable) {
        return status == null ? quoteRepository.findAllByOrderByCreatedAtDesc(pageable)
                : quoteRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionQuote> ofSubscriber(SubscriberType type, String code) {
        return quoteRepository.findBySubscriberTypeAndSubscriberCodeOrderByCreatedAtDesc(type, code);
    }

    private static PlanDerivationService.Spec derivationSpec(SubscriptionQuote quote) {
        return new PlanDerivationService.Spec(quote.getQuotedPrice(), quote.getSetupFee(), quote.getTrialDays(), quote.getCommitmentMonths(),
                null, null, null, PlanDerivation.Reason.NEGOTIATION, "Devis " + quote.getQuoteNumber(),
                null, null, PlanDerivation.RenewalBehaviour.KEEP, null, false);
    }

    private static String ownerName(SubscriptionOwnerResolver.Owner owner) {
        if (owner.member() != null) return owner.member().getDisplayName();
        if (owner.customer() != null) return (nz(owner.customer().getFirstname()) + " " + nz(owner.customer().getLastname())).trim();
        if (owner.businessEntity() != null) return owner.businessEntity().getName();
        return owner.code();
    }

    private static String ownerEmail(SubscriptionOwnerResolver.Owner owner) {
        if (owner.member() != null) return owner.member().getEmail();
        if (owner.customer() != null) {
            return StringUtils.hasText(owner.customer().getBillingEmail()) ? owner.customer().getBillingEmail() : owner.customer().getEmail();
        }
        if (owner.businessEntity() != null) return owner.businessEntity().getEmail();
        return null;
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
