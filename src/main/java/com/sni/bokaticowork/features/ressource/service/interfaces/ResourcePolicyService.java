package com.sni.bokaticowork.features.ressource.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.ressource.dto.request.CreateResourcePolicyRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateResourcePolicyRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourcePolicyResponse;
import com.sni.bokaticowork.features.ressource.model.ResourcePolicy;
import org.springframework.data.domain.Pageable;

public interface ResourcePolicyService {

    void createPolicy(CreateResourcePolicyRequest request);
    void updatePolicy(String code, UpdateResourcePolicyRequest request);
    void updatePolicyStatus(String code, Boolean status);
    void updateCancellationPolicy(String code, Boolean status);
    void deletePolicy(String code);
    void deleteAllPolicies();
    ResourcePolicyResponse getPolicy(String code);
    ResourcePolicy getPolicyForService(String code);
    ResourcePolicy getPolicyForService(Long id);
    PaginatedResponse<ResourcePolicyResponse> list(Pageable pageable);
    PaginatedResponse<ResourcePolicyResponse> search(String query);


}
