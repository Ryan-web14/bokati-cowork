package com.sni.bokaticowork.features.subscription.derivation.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.subscription.derivation.model.PlanDerivation;
import com.sni.bokaticowork.features.subscription.repository.PlanPriceRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanVersionRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Deriver en lot · ce qui rend la protection tarifaire et les partenariats praticables.
 *
 * <p>Toujours en deux temps : simuler d'abord, avec le cout total, executer ensuite. Une derivation
 * qui echoue n'arrete pas le lot · elle est rapportee, ligne par ligne, avec sa raison. Un lot qui
 * s'arreterait a la premiere erreur laisserait la moitie des abonnes proteges et l'autre pas, sans
 * que personne ne sache lesquels.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanDerivationBatchService {

    private final PlanDerivationService derivationService;
    private final com.sni.bokaticowork.features.subscription.derivation.repository.PlanDerivationRepository derivationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanVersionRepository planVersionRepository;
    private final PlanPriceRepository planPriceRepository;
    private final SequenceGeneratorFacade sequenceGenerator;

    public record Line(String subscriptionNumber, boolean ok, String derivationCode, BigDecimal impact,
                       boolean requiresApproval, String message) {
    }

    public record Outcome(String batchCode, int total, int succeeded, int failed, int pendingApproval,
                          BigDecimal totalImpact, List<Line> lines) {
    }

    /** Une meme derivation sur une liste d'abonnements · simulation ou execution. */
    @Transactional
    public Outcome apply(List<String> subscriptionNumbers, PlanDerivationService.Spec spec, boolean simulateOnly, String requestedBy) {
        if (subscriptionNumbers == null || subscriptionNumbers.isEmpty()) {
            throw new BadRequestException("Indiquez les abonnements");
        }
        String batchCode = simulateOnly ? null : sequenceGenerator.next("plan_derivation_batch");
        List<Line> lines = new ArrayList<>();
        BigDecimal totalImpact = BigDecimal.ZERO;
        int succeeded = 0;
        int failed = 0;
        int pending = 0;

        for (String number : subscriptionNumbers) {
            try {
                PlanDerivationService.Preview preview = derivationService.simulate(number, spec);
                if (!preview.allowed()) {
                    lines.add(new Line(number, false, null, preview.totalImpact(), false, preview.blockingReason()));
                    failed++;
                    continue;
                }
                if (simulateOnly) {
                    lines.add(new Line(number, true, null, preview.totalImpact(), preview.requiresApproval(), "simulation"));
                } else {
                    Subscription subscription = subscriptionRepository.findBySubscriptionNumber(number)
                            .orElseThrow(() -> new ResourceNotFoundException("Abonnement introuvable"));
                    PlanDerivation created = derivationService.create(subscription, spec, requestedBy, batchCode);
                    boolean awaiting = created.getStatus() == PlanDerivation.Status.PENDING_APPROVAL;
                    lines.add(new Line(number, true, created.getDerivationCode(), created.getTotalImpactAmount(), awaiting,
                            awaiting ? "en attente de visa" : "appliquée"));
                }
                totalImpact = totalImpact.add(preview.totalImpact());
                if (preview.requiresApproval()) {
                    pending++;
                }
                succeeded++;
            } catch (RuntimeException ex) {
                lines.add(new Line(number, false, null, BigDecimal.ZERO, false, ex.getMessage()));
                failed++;
            }
        }
        return new Outcome(batchCode, subscriptionNumbers.size(), succeeded, failed, pending, totalImpact, lines);
    }

    /**
     * Protection tarifaire lors d'une hausse de catalogue.
     *
     * <p>Une nouvelle version du plan a ete publiee ; les abonnes de l'ancienne conservent leur
     * tarif jusqu'a la date donnee. Une derivation GRANDFATHERING est generee pour chacun, au prix
     * qu'il payait · et l'on sait exactement combien la protection coute.</p>
     *
     * @param protectedUntil fin de la protection · nulle pour une protection sans terme
     */
    @Transactional
    public Outcome protectSubscribers(Long previousVersionId, LocalDate protectedUntil, boolean simulateOnly, String requestedBy) {
        PlanVersion previous = planVersionRepository.findById(previousVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version de plan introuvable"));
        if (previous.privateToSubscription()) {
            throw new BadRequestException("La protection part d'une version de catalogue, pas d'une version privée");
        }
        List<Subscription> subscribers = subscriptionRepository.findAllOpenOnPlanVersion(previous.getId());
        List<Line> lines = new ArrayList<>();
        BigDecimal totalImpact = BigDecimal.ZERO;
        int succeeded = 0;
        int failed = 0;
        String batchCode = simulateOnly ? null : sequenceGenerator.next("plan_derivation_batch");

        for (Subscription subscription : subscribers) {
            PlanPrice oldPrice = planPriceRepository.findAllByPlanVersion(previous.getId()).stream()
                    .filter(p -> p.getBillingCycle() == subscription.getBillingCycle()).findFirst().orElse(null);
            if (oldPrice == null) {
                lines.add(new Line(subscription.getSubscriptionNumber(), false, null, BigDecimal.ZERO, false,
                        "aucun prix sur l'ancienne version pour le cycle " + subscription.getBillingCycle()));
                failed++;
                continue;
            }
            // L'abonne est bascule sur la version de catalogue active, puis protege au prix ancien ·
            // la derivation part toujours du catalogue courant, c'est ce qui rend l'ecart lisible.
            PlanDerivationService.Spec spec = new PlanDerivationService.Spec(oldPrice.getAmount(), null, null, null,
                    null, null, null, PlanDerivation.Reason.GRANDFATHERING,
                    "Protection tarifaire · version " + previous.getVersionNumber() + " conservée",
                    LocalDate.now(), protectedUntil, PlanDerivation.RenewalBehaviour.KEEP, null, false);
            try {
                PlanVersion catalogue = planVersionRepository.findFirstByPlanAndStatusOrderByVersionNumberDesc(
                                previous.getPlan().getId(), com.sni.bokaticowork.features.subscription.subscription.enums.PlanStatus.ACTIVE.name())
                        .orElse(previous);
                if (!simulateOnly) {
                    subscription.setPlanVersion(catalogue);
                    subscriptionRepository.save(subscription);
                }
                PlanDerivationService.Preview preview = simulateOnly
                        ? derivationService.simulateAgainst(subscription, catalogue, spec)
                        : null;
                if (preview != null && !preview.allowed()) {
                    if (preview.blockingReason().startsWith("Rien ne diffère")) {
                        lines.add(new Line(subscription.getSubscriptionNumber(), true, null, BigDecimal.ZERO, false, "prix inchangé · rien à protéger"));
                        succeeded++;
                    } else {
                        lines.add(new Line(subscription.getSubscriptionNumber(), false, null, BigDecimal.ZERO, false, preview.blockingReason()));
                        failed++;
                    }
                    continue;
                }
                if (simulateOnly) {
                    lines.add(new Line(subscription.getSubscriptionNumber(), true, null, preview.totalImpact(), preview.requiresApproval(), "simulation"));
                    totalImpact = totalImpact.add(preview.totalImpact());
                    succeeded++;
                    continue;
                }
                PlanDerivation created = derivationService.create(subscription, spec, requestedBy, batchCode);
                lines.add(new Line(subscription.getSubscriptionNumber(), true, created.getDerivationCode(), created.getTotalImpactAmount(),
                        created.getStatus() == PlanDerivation.Status.PENDING_APPROVAL, created.getStatus().name()));
                totalImpact = totalImpact.add(created.getTotalImpactAmount());
                succeeded++;
            } catch (RuntimeException ex) {
                String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                boolean unchanged = message.contains("Rien ne diffère");
                lines.add(new Line(subscription.getSubscriptionNumber(), unchanged, null, BigDecimal.ZERO, false,
                        unchanged ? "prix inchangé · rien à protéger" : message));
                if (unchanged) succeeded++; else failed++;
            }
        }
        log.info("Protection tarifaire · version {} · {} abonne(s), {} protege(s), impact {}", previous.getVersionNumber(),
                subscribers.size(), succeeded, totalImpact);
        return new Outcome(batchCode, subscribers.size(), succeeded, failed, 0, totalImpact, lines);
    }

    @Transactional(readOnly = true)
    public List<PlanDerivation> ofBatch(String batchCode) {
        return derivationRepository.findByBatchCodeOrderByCreatedAtAsc(batchCode);
    }
}
