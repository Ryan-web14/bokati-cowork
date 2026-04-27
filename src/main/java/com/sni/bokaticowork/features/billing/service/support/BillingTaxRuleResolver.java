package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.features.billing.model.TaxRule;
import com.sni.bokaticowork.features.billing.repository.TaxRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BillingTaxRuleResolver {

    private static final String DEFAULT_COUNTRY_CODE = "CG";
    private static final String VAT = "VAT";
    private static final String ADDITIONAL_CENT = "ADDITIONAL_CENT";

    private final TaxRuleRepository taxRuleRepository;

    public TaxProfile defaultTaxProfile() {
        List<TaxRule> rules = taxRuleRepository.findActiveRules(DEFAULT_COUNTRY_CODE, LocalDate.now());
        BigDecimal vatRate = rules.stream()
                .filter(rule -> VAT.equalsIgnoreCase(rule.getTaxType()))
                .findFirst()
                .map(TaxRule::getRate)
                .orElse(BigDecimal.ZERO);
        BigDecimal additionalCentRate = rules.stream()
                .filter(rule -> ADDITIONAL_CENT.equalsIgnoreCase(rule.getTaxType()))
                .findFirst()
                .map(TaxRule::getRate)
                .orElse(BigDecimal.ZERO);
        return new TaxProfile(vatRate, additionalCentRate);
    }

    public record TaxProfile(BigDecimal vatRate, BigDecimal additionalCentRate) {
    }
}
