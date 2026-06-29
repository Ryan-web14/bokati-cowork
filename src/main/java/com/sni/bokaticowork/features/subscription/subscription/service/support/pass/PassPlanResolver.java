package com.sni.bokaticowork.features.subscription.subscription.service.support.pass;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlan;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanVersion;
import com.sni.bokaticowork.features.subscription.repository.PassPlanPriceRepository;
import com.sni.bokaticowork.features.subscription.repository.PassPlanRepository;
import com.sni.bokaticowork.features.subscription.repository.PassPlanVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PassPlanResolver {

    private final PassPlanRepository passPlansRepository;
    private final PassPlanVersionRepository passVersionRepository;
    private final PassPlanPriceRepository passPriceRepository;

    public PassPlanVersion resolveVersion(String planCode, Long versionId) {
        PassPlan plan = passPlansRepository.findByCode(planCode.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Pass plan not found: " + planCode));
        if (plan.getStatus() != PlanStatus.ACTIVE) {
            throw new BadRequestException("Pass plan " + planCode + " is not active");
        }
        if (versionId != null) {
            return passVersionRepository.findById(versionId)
                    .filter(v -> v.getPlan().getId().equals(plan.getId()))
                    .filter(v -> v.getStatus() == PlanStatus.ACTIVE)
                    .orElseThrow(() -> new ResourceNotFoundException("Pass plan version not found"));
        }
        return passVersionRepository.findFirstByPlanAndStatusOrderByVersionNumberDesc(plan, PlanStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No active version for pass plan " + planCode));
    }

    public PassPlanPrice resolvePrice(PassPlanVersion version, String currency) {
        String targetCurrency = StringUtils.hasText(currency) ? currency.trim().toUpperCase() : "XAF";
        return passPriceRepository.findByPassVersionAndCurrency(version, targetCurrency)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No price defined for pass plan version " + version.getId()
                                + " in currency " + targetCurrency));
    }
}
