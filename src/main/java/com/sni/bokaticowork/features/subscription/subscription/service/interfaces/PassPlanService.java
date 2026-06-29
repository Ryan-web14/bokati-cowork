package com.sni.bokaticowork.features.subscription.subscription.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.PassPlanDtos;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassType;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PassPlanService {

    PassPlanDtos.PassPlanResponse createPlan(PassPlanDtos.CreatePassPlanRequest request);

    PassPlanDtos.PassPlanResponse updatePlan(String planCode, PassPlanDtos.UpdatePassPlanRequest request);

    PassPlanDtos.PassPlanResponse getPlan(String planCode);

    PassPlanDtos.PassPlanResponse publishPlan(String planCode);

    PassPlanDtos.PassPlanResponse archivePlan(String planCode);

    PaginatedResponse<PassPlanDtos.PassPlanResponse> searchPlans(PassType passType, PlanStatus status, String searchText, Pageable pageable);

    PassPlanDtos.PassPlanVersionResponse createVersion(String planCode, PassPlanDtos.CreatePassPlanVersionRequest request);

    PassPlanDtos.PassPlanVersionResponse getVersion(Long versionId);

    List<PassPlanDtos.PassPlanVersionResponse> listVersions(String planCode);

    PassPlanDtos.PassPlanVersionResponse publishVersion(Long versionId);

    PassPlanDtos.PassPlanPriceResponse setPrice(Long versionId, PassPlanDtos.SetPassPlanPriceRequest request);

    List<PassPlanDtos.PassPlanPriceResponse> listPrices(Long versionId);

    PassPlanDtos.PassPlanEntitlementResponse addEntitlement(Long versionId, PassPlanDtos.AddPassPlanEntitlementRequest request);

    void removeEntitlement(Long entitlementId);

    List<PassPlanDtos.PassPlanEntitlementResponse> listEntitlements(Long versionId);
}
