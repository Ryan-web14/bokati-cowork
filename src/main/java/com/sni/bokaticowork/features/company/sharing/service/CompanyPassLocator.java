package com.sni.bokaticowork.features.company.sharing.service;

import com.sni.bokaticowork.features.company.membership.repository.CustomerMemberLinkRepository;
import com.sni.bokaticowork.features.company.membership.service.MemberBillingCompanyResolver;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassBeneficiary;
import com.sni.bokaticowork.features.subscription.subscription.pass.repository.PassBeneficiaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Les passes d'organisation qu'un membre peut utiliser · ceux dont il est beneficiaire.
 *
 * <p>Un acces ne sert que si le membre est encore rattache a l'organisation titulaire, que le pass
 * est actif sur sa validite, et que l'acces lui-meme est ouvert (periode, nombre de passages). Le
 * choix entre organisations suit la meme regle que les places, voir {@link CompanySeatLocator}.</p>
 */
@Component
@RequiredArgsConstructor
public class CompanyPassLocator {

    private final PassBeneficiaryRepository beneficiaryRepository;
    private final CustomerMemberLinkRepository linkRepository;
    private final CompanySeatLocator seatLocator;

    @Transactional(readOnly = true)
    public List<PassBeneficiary> usableAccesses(String memberId, String requestedCompanyCode) {
        if (!StringUtils.hasText(memberId)) {
            return List.of();
        }
        String requested = StringUtils.hasText(requestedCompanyCode) ? requestedCompanyCode.trim() : null;
        if (MemberBillingCompanyResolver.NO_COMPANY.equalsIgnoreCase(requested)) {
            return List.of();
        }
        Set<String> linked = new HashSet<>();
        linkRepository.findActiveCompanyCodes(memberId.trim()).forEach(code -> linked.add(code.toUpperCase()));
        Instant now = Instant.now();
        Map<String, List<PassBeneficiary>> byCompany = new LinkedHashMap<>();
        for (PassBeneficiary access : beneficiaryRepository.findOpenAccesses("MEMBER", memberId.trim(), SubscriberType.CUSTOMER)) {
            Pass pass = access.getPass();
            String company = pass.getOwnerCode();
            if (company == null || !linked.contains(company.toUpperCase()) || !usable(access, now)) {
                continue;
            }
            byCompany.computeIfAbsent(company.toUpperCase(), k -> new ArrayList<>()).add(access);
        }
        if (byCompany.isEmpty()) {
            return List.of();
        }
        return CompanySeatLocator.choose(byCompany, requested, seatLocator.automaticCompany(memberId.trim()),
                "accès au pass");
    }

    static boolean usable(PassBeneficiary access, Instant now) {
        Pass pass = access.getPass();
        if (pass.getStatus() != PassStatus.ACTIVE) {
            return false;
        }
        if (pass.getValidFrom() != null && pass.getValidFrom().isAfter(now)) {
            return false;
        }
        if (pass.getValidUntil() != null && pass.getValidUntil().isBefore(now)) {
            return false;
        }
        Integer remaining = access.remainingForBeneficiary();
        return access.usableAt(now) && (remaining == null || remaining > 0);
    }
}
