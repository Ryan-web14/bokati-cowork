package com.sni.bokaticowork.features.company.sharing.service;

import com.sni.bokaticowork.features.billing.service.support.InvoiceAnnexProvider;
import com.sni.bokaticowork.features.billing.service.support.InvoiceUsageAnnex;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.repository.EntitlementDefinitionRepository;
import com.sni.bokaticowork.features.subscription.repository.EntitlementLedgerRepository;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementDefinition;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * L'annexe de la facture de renouvellement d'un pass d'organisation · ce que chaque beneficiaire a
 * pris sur la periode qui s'acheve.
 *
 * <p>Le renouvellement decale la validite du pass avant de le facturer : la periode ecoulee se lit
 * donc sur sa facturation precedente (emission ou renouvellement), jusqu'au renouvellement.</p>
 */
@Component
public class PassUsageAnnexProvider implements InvoiceAnnexProvider {

    static final String PASS_RENEWAL = "PASS_RENEWAL";

    private final PassRepository passRepository;
    private final BillableItemRepository billableItemRepository;
    private final EntitlementLedgerRepository ledgerRepository;
    private final MemberRepository memberRepository;
    private final EntitlementDefinitionRepository definitionRepository;
    private final Clock clock;

    @Autowired
    public PassUsageAnnexProvider(PassRepository passRepository, BillableItemRepository billableItemRepository,
                                  EntitlementLedgerRepository ledgerRepository, MemberRepository memberRepository,
                                  EntitlementDefinitionRepository definitionRepository) {
        this(passRepository, billableItemRepository, ledgerRepository, memberRepository, definitionRepository,
                Clock.systemDefaultZone());
    }

    PassUsageAnnexProvider(PassRepository passRepository, BillableItemRepository billableItemRepository,
                           EntitlementLedgerRepository ledgerRepository, MemberRepository memberRepository,
                           EntitlementDefinitionRepository definitionRepository, Clock clock) {
        this.passRepository = passRepository;
        this.billableItemRepository = billableItemRepository;
        this.ledgerRepository = ledgerRepository;
        this.memberRepository = memberRepository;
        this.definitionRepository = definitionRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public InvoiceUsageAnnex annexFor(List<BillableItem> items) {
        if (items == null) {
            return null;
        }
        for (BillableItem item : items) {
            if (item.getSubscriberType() != SubscriberType.CUSTOMER || !PASS_RENEWAL.equals(item.getSourceType())
                    || item.getSourceId() == null || item.getBillingPeriodStart() == null) {
                continue;
            }
            Optional<Pass> pass = passRepository.findByPassNumber(item.getSourceId());
            if (pass.isPresent()) {
                InvoiceUsageAnnex annex = annex(pass.get(), item.getBillingPeriodStart());
                if (annex != null) {
                    return annex;
                }
            }
        }
        return null;
    }

    InvoiceUsageAnnex annex(Pass pass, LocalDate newPeriodStart) {
        LocalDate previous = billableItemRepository.findPreviousPassPeriodStart(pass.getPassNumber(), newPeriodStart);
        if (previous == null) {
            return null;
        }
        Instant from = previous.atStartOfDay(clock.getZone()).toInstant();
        Instant to = clock.instant();
        Map<String, String> names = new HashMap<>();
        Map<String, EntitlementDefinition> definitions = new HashMap<>();
        List<InvoiceUsageAnnex.Row> rows = new ArrayList<>();
        for (Object[] line : ledgerRepository.sumConsumedByBeneficiaryOnPassBetween(pass.getId(), from, to)) {
            String member = String.valueOf(line[1]);
            String code = String.valueOf(line[2]);
            BigDecimal consumed = line[3] instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(line[3]));
            String name = names.computeIfAbsent(member.toUpperCase(), k -> memberRepository.findByMemberIdAndDeletedFalse(member)
                    .map(Member::getDisplayName).orElse(member));
            EntitlementDefinition definition = definitions.computeIfAbsent(code.toUpperCase(),
                    k -> definitionRepository.findByCodeIgnoreCase(code).orElse(null));
            rows.add(new InvoiceUsageAnnex.Row(member, name, code, definition == null ? null : definition.getName(),
                    definition == null || definition.getUnit() == null ? null : definition.getUnit().name(),
                    BigDecimal.ZERO, BigDecimal.ZERO, consumed, BigDecimal.ZERO, BigDecimal.ZERO));
        }
        if (rows.isEmpty()) {
            return null;
        }
        rows.sort(Comparator.comparing(InvoiceUsageAnnex.Row::memberName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(InvoiceUsageAnnex.Row::entitlementCode));
        return new InvoiceUsageAnnex("Consommation par collaborateur", pass.getPassNumber(), previous,
                newPeriodStart.minusDays(1), pass.getCurrency(), rows, InvoiceUsageAnnex.KIND_PASS);
    }
}
