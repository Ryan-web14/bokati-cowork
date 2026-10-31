package com.sni.bokaticowork.features.company.sharing.service;

import com.sni.bokaticowork.features.billing.service.support.InvoiceAnnexProvider;
import com.sni.bokaticowork.features.billing.service.support.InvoiceUsageAnnex;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.sharing.model.SeatUsage;
import com.sni.bokaticowork.features.company.sharing.repository.SeatUsageRepository;
import com.sni.bokaticowork.features.subscription.overage.model.SubscriptionOverageCharge;
import com.sni.bokaticowork.features.subscription.overage.repository.SubscriptionOverageChargeRepository;
import com.sni.bokaticowork.features.subscription.repository.EntitlementDefinitionRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementDefinition;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * L'annexe de la facture de renouvellement d'un abonnement d'organisation · ce que chaque
 * collaborateur a pris sur la periode qui vient de s'achever.
 *
 * <p>La facture de renouvellement porte la periode qui commence ; l'annexe, celle qui la precede.
 * Les depassements des collaborateurs y figurent pour memoire : ils ont deja ete factures a
 * l'organisation, chacun sur sa facture.</p>
 */
@Component
@RequiredArgsConstructor
public class SeatUsageAnnexProvider implements InvoiceAnnexProvider {

    static final Set<String> RENEWAL_SOURCES = Set.of("SUBSCRIPTION_RENEWAL");

    private final SubscriptionRepository subscriptionRepository;
    private final SeatUsageRepository usageRepository;
    private final SubscriptionOverageChargeRepository chargeRepository;
    private final MemberRepository memberRepository;
    private final EntitlementDefinitionRepository definitionRepository;

    @Override
    public InvoiceUsageAnnex annexFor(List<BillableItem> items) {
        if (items == null) {
            return null;
        }
        for (BillableItem item : items) {
            if (item.getSubscriberType() != SubscriberType.CUSTOMER || !RENEWAL_SOURCES.contains(item.getSourceType())
                    || item.getSourceId() == null || item.getBillingPeriodStart() == null) {
                continue;
            }
            Optional<Subscription> subscription = subscriptionRepository.findBySubscriptionNumber(item.getSourceId());
            if (subscription.isPresent()) {
                InvoiceUsageAnnex annex = annex(subscription.get(), item.getBillingPeriodStart());
                if (annex != null) {
                    return annex;
                }
            }
        }
        return null;
    }

    InvoiceUsageAnnex annex(Subscription subscription, LocalDate newPeriodStart) {
        LocalDate period = usageRepository.findLastPeriodBefore(subscription.getId(), newPeriodStart, newPeriodStart.minusMonths(13));
        if (period == null) {
            return null;
        }
        Map<String, Totals> byLine = new LinkedHashMap<>();
        for (SeatUsage usage : usageRepository.findConsumedInPeriod(subscription.getId(), period)) {
            Totals totals = byLine.computeIfAbsent(key(usage.getMemberCode(), usage.getEntitlementCode()),
                    k -> new Totals(usage.getMemberCode(), usage.getEntitlementCode()));
            totals.fromQuota = totals.fromQuota.add(nz(usage.getFromQuota()));
            totals.fromPool = totals.fromPool.add(nz(usage.getFromPool()));
            totals.consumed = totals.consumed.add(nz(usage.getQuantity()));
        }
        ZoneId zone = ZoneId.systemDefault();
        Instant from = period.atStartOfDay(zone).toInstant();
        Instant to = newPeriodStart.atStartOfDay(zone).toInstant();
        for (SubscriptionOverageCharge charge : chargeRepository.findBeneficiaryCharges(subscription.getId(), from, to)) {
            String member = charge.getUsageRecord().getBeneficiaryCode();
            Totals totals = byLine.computeIfAbsent(key(member, charge.getEntitlementCode()),
                    k -> new Totals(member, charge.getEntitlementCode()));
            totals.overage = totals.overage.add(nz(charge.getOverageQuantity()));
            totals.overageAmount = totals.overageAmount.add(nz(charge.getAmount()));
        }
        if (byLine.isEmpty()) {
            return null;
        }
        Map<String, String> names = new LinkedHashMap<>();
        Map<String, EntitlementDefinition> definitions = new LinkedHashMap<>();
        List<InvoiceUsageAnnex.Row> rows = new ArrayList<>();
        for (Totals totals : byLine.values()) {
            String name = names.computeIfAbsent(totals.member.toUpperCase(), k -> memberRepository
                    .findByMemberIdAndDeletedFalse(totals.member).map(Member::getDisplayName).orElse(totals.member));
            EntitlementDefinition definition = definitions.computeIfAbsent(totals.entitlement.toUpperCase(),
                    k -> definitionRepository.findByCodeIgnoreCase(totals.entitlement).orElse(null));
            rows.add(new InvoiceUsageAnnex.Row(totals.member, name, totals.entitlement,
                    definition == null ? null : definition.getName(),
                    definition == null || definition.getUnit() == null ? null : definition.getUnit().name(),
                    totals.fromQuota, totals.fromPool, totals.consumed, totals.overage, totals.overageAmount));
        }
        rows.sort(Comparator.comparing(InvoiceUsageAnnex.Row::memberName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(InvoiceUsageAnnex.Row::entitlementCode));
        return new InvoiceUsageAnnex("Consommation par collaborateur", subscription.getSubscriptionNumber(), period,
                newPeriodStart.minusDays(1), subscription.getCurrency(), rows);
    }

    private static String key(String member, String entitlement) {
        return (member == null ? "" : member.toUpperCase()) + "|" + (entitlement == null ? "" : entitlement.toUpperCase());
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static final class Totals {
        final String member;
        final String entitlement;
        BigDecimal fromQuota = BigDecimal.ZERO;
        BigDecimal fromPool = BigDecimal.ZERO;
        BigDecimal consumed = BigDecimal.ZERO;
        BigDecimal overage = BigDecimal.ZERO;
        BigDecimal overageAmount = BigDecimal.ZERO;

        Totals(String member, String entitlement) {
            this.member = member == null ? "" : member;
            this.entitlement = entitlement == null ? "" : entitlement;
        }
    }
}
