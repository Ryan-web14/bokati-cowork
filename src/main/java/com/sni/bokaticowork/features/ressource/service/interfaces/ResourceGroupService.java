package com.sni.bokaticowork.features.ressource.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.ressource.dto.request.CreateResourceGroupRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateResourceGroupRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceGroupResponse;
import com.sni.bokaticowork.features.ressource.model.ResourceGroup;

public interface ResourceGroupService {

    void createResourceGroup(CreateResourceGroupRequest request);
    void updateResourceGroup(String code, UpdateResourceGroupRequest request);
    void deleteResourceGroup(String code);
    void deleteAllResourceGroups();
    ResourceGroupResponse getResourceGroup(String code);
    PaginatedResponse<ResourceGroupResponse> search(String query);
    ResourceGroup getResourceGroupForService(String code);
    PaginatedResponse<ResourceGroupResponse> list();

}
