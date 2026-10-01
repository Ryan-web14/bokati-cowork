package com.sni.bokaticowork.features.portal.subscription.service;

import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.subscription.change.model.SubscriptionChangeRequest;
import com.sni.bokaticowork.features.subscription.change.repository.SubscriptionChangeRequestRepository;
import com.sni.bokaticowork.features.subscription.derivation.model.PlanDerivation;
import com.sni.bokaticowork.features.subscription.derivation.repository.PlanDerivationRepository;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionFreeze;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionTermination;
import com.sni.bokaticowork.features.subscription.lifecycle.repository.SubscriptionFreezeRepository;
import com.sni.bokaticowork.features.subscription.lifecycle.repository.SubscriptionTerminationRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.timeline.enums.SubscriptionTimelineEventType;
import com.sni.bokaticowork.features.subscription.timeline.model.SubscriptionTimelineEvent;
import com.sni.bokaticowork.features.subscription.timeline.repository.SubscriptionTimelineEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * L'historique lisible par le client · ce qui lui est arrive, dans ses mots.
 *
 * <p>La frise interne existe ; le client voit la sienne : souscriptions, changements, gels,
 * preavis, avantages accordes. Ce qui est interne (signal de fraude, notification en file) n'y
 * figure pas. Chaque ligne a une date, une famille, un titre et un detail en francais.</p>
 */
@Service
@RequiredArgsConstructor
public class ClientHistoryService {

    private static final Set<SubscriptionTimelineEventType> HIDDEN = EnumSet.of(
            SubscriptionTimelineEventType.FRAUD_SIGNAL_RAISED, SubscriptionTimelineEventType.NOTIFICATION_QUEUED,
            SubscriptionTimelineEventType.NOTIFICATION_DISPATCHED, SubscriptionTimelineEventType.BILLING_SCHEDULED,
            SubscriptionTimelineEventType.ENTITLEMENTS_GRANTED, SubscriptionTimelineEventType.STATUS_CHANGED);

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionTimelineEventRepository timelineRepository;
    private final SubscriptionFreezeRepository freezeRepository;
    private final SubscriptionTerminationRepository terminationRepository;
    private final PlanDerivationRepository derivationRepository;
    private final SubscriptionChangeRequestRepository changeRepository;
    private final ClientSubscriptionSelfService selfService;

    public enum Kind {
        SUBSCRIPTION, PAYMENT, CHANGE, FREEZE, TERMINATION, BENEFIT, PASS, USAGE
    }

    public record Entry(Instant occurredAt, Kind kind, String subscriptionNumber, String title, String detail, BigDecimal amount, String currency) {
    }

    @Transactional(readOnly = true)
    public List<Entry> ofSubscription(Member member, String subscriptionNumber) {
        Subscription subscription = selfService.own(member, subscriptionNumber);
        return entries(subscription);
    }

    /** Toute l'histoire du membre · tous ses abonnements, du plus recent au plus ancien. */
    @Transactional(readOnly = true)
    public List<Entry> ofMember(Member member) {
        List<Entry> all = new ArrayList<>();
        subscriptionRepository.findAllBySubscriber(SubscriberType.MEMBER.name(), member.getMemberId()).forEach(s -> all.addAll(entries(s)));
        return sorted(all);
    }

    private List<Entry> entries(Subscription subscription) {
        String number = subscription.getSubscriptionNumber();
        String currency = subscription.getCurrency();
        List<Entry> entries = new ArrayList<>();

        timelineRepository.search(number, null, null, null, null, null, PageRequest.of(0, 500)).forEach(event -> {
            if (event.getEventType() == null || HIDDEN.contains(event.getEventType())) {
                return;
            }
            entries.add(new Entry(event.getOccurredAt(), kindOf(event.getEventType()), number, title(event), event.getDescription(), null, currency));
        });
        for (SubscriptionFreeze f : freezeRepository.findBySubscription_IdOrderByStartedOnDesc(subscription.getId())) {
            entries.add(new Entry(at(f.getStartedOn()), Kind.FREEZE, number, "Gel de l'abonnement",
                    "Du " + f.getStartedOn() + " au " + (f.getResumedOn() != null ? f.getResumedOn() : f.getPlannedUntil())
                            + (f.getFeeAmount() != null && f.getFeeAmount().signum() > 0 ? " · frais " + plain(f.getFeeAmount()) + " " + currency : ""),
                    f.getFeeAmount(), currency));
        }
        for (SubscriptionTermination t : terminationRepository.findBySubscription_IdOrderByRequestedAtDesc(subscription.getId())) {
            String status = switch (t.getStatus()) {
                case REQUESTED -> "demande enregistrée";
                case ACCEPTED -> "acceptée";
                case RETRACTED -> "retirée";
                case COMPLETED -> "achevée";
            };
            entries.add(new Entry(t.getRequestedAt(), Kind.TERMINATION, number, "Résiliation " + status,
                    "Effet le " + t.getEffectiveDate() + (t.feeDue().signum() > 0 ? " · frais de rupture " + plain(t.feeDue()) + " " + currency : ""),
                    t.feeDue(), currency));
        }
        for (PlanDerivation d : derivationRepository.findBySubscription_IdOrderByCreatedAtDesc(subscription.getId())) {
            if (d.getStatus() == PlanDerivation.Status.REJECTED || d.getStatus() == PlanDerivation.Status.DRAFT
                    || d.getStatus() == PlanDerivation.Status.PENDING_APPROVAL) {
                continue;
            }
            entries.add(new Entry(d.getAppliedAt() != null ? d.getAppliedAt() : d.getCreatedAt(), Kind.BENEFIT, number, "Conditions particulières accordées",
                    reasonLabel(d.getReason()) + (d.getDiscountPercent() != null && d.getDiscountPercent().signum() > 0
                            ? " · " + plain(d.getDiscountPercent()) + " % de remise" : "")
                            + (d.getEndedAt() != null ? " · terminées" : d.getEffectiveTo() != null ? " · jusqu'au " + d.getEffectiveTo() : ""),
                    d.getTotalImpactAmount(), currency));
        }
        for (SubscriptionChangeRequest c : changeRepository.findBySubscription_IdOrderByCreatedAtDesc(subscription.getId())) {
            String what = c.getTargetPlanVersion() == null ? "Changement" : "Passage au plan " + c.getTargetPlanVersion().getName();
            String when = c.getAppliedAt() != null ? "appliqué" : "prévu le " + c.getEffectiveDate();
            entries.add(new Entry(c.getAppliedAt() != null ? c.getAppliedAt() : c.getCreatedAt(), Kind.CHANGE, number, what,
                    when + (c.getProrationAmount() != null && c.getProrationAmount().signum() > 0
                            ? " · " + plain(c.getProrationAmount()) + " " + currency + (Boolean.TRUE.equals(c.getProrationCredit()) ? " crédités" : " facturés") : ""),
                    c.getProrationAmount(), currency));
        }
        return sorted(entries);
    }

    private static List<Entry> sorted(List<Entry> entries) {
        entries.sort(Comparator.comparing(Entry::occurredAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return entries;
    }

    private static Kind kindOf(SubscriptionTimelineEventType type) {
        return switch (type) {
            case PLAN_CHANGED, ADDON_ADDED, ADDON_CANCELLED, ADDON_EXPIRED -> Kind.CHANGE;
            case PROMOTION_REDEEMED, WALLET_CREDIT_APPLIED -> Kind.BENEFIT;
            case PASS_ISSUED, PASS_CANCELLED, PASS_EXPIRED -> Kind.PASS;
            case USAGE_RECORDED, OVERAGE_BILLED, ROLLOVER_APPLIED -> Kind.USAGE;
            case PAUSE_STARTED, PAUSE_ENDED -> Kind.FREEZE;
            default -> Kind.SUBSCRIPTION;
        };
    }

    private static String title(SubscriptionTimelineEvent event) {
        if (event.getTitle() != null && !event.getTitle().isBlank()) {
            return event.getTitle();
        }
        return switch (event.getEventType()) {
            case SUBSCRIPTION_CREATED -> "Souscription";
            case SUBSCRIPTION_ACTIVATED -> "Abonnement activé";
            case SUBSCRIPTION_RENEWED -> "Abonnement renouvelé";
            case SUBSCRIPTION_SUSPENDED -> "Abonnement suspendu";
            case SUBSCRIPTION_CANCELLED -> "Abonnement clos";
            case SUBSCRIPTION_EXPIRED -> "Abonnement expiré";
            case PLAN_CHANGED -> "Changement de plan";
            case PROMOTION_REDEEMED -> "Promotion appliquée";
            case PASS_ISSUED -> "Pass émis";
            case PAUSE_STARTED -> "Gel";
            case PAUSE_ENDED -> "Reprise";
            case CONTRACT_BOUND -> "Contrat signé";
            default -> event.getEventType().name().toLowerCase().replace('_', ' ');
        };
    }

    private static String reasonLabel(PlanDerivation.Reason reason) {
        return switch (reason) {
            case NEGOTIATION -> "Tarif négocié";
            case GOODWILL -> "Geste commercial";
            case PARTNERSHIP -> "Tarif partenaire";
            case PILOT -> "Offre pilote";
            case GRANDFATHERING -> "Ancien tarif conservé";
            case LOYALTY -> "Avantage fidélité";
            case CORRECTION -> "Correction";
        };
    }

    private static Instant at(LocalDate day) {
        return day == null ? null : day.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
