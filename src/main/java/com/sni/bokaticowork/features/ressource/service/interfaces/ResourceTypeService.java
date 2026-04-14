package com.sni.bokaticowork.features.ressource.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.ressource.dto.request.CreateResourceTypeRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateResourceTypeRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceTypeResponse;
import com.sni.bokaticowork.features.ressource.model.ResourceType;
import org.springframework.data.domain.Pageable;

public interface ResourceTypeService {

    void createType(CreateResourceTypeRequest request);
    void updateType(String code, UpdateResourceTypeRequest request);
    void deleteType(String code);
    void deleteAllTypes();
    ResourceTypeResponse getType(String code);
    ResourceType getTypeForService(String code);
    ResourceType getTypeForService(Long id);
    PaginatedResponse<ResourceTypeResponse> list(Pageable pageable);
    PaginatedResponse<ResourceTypeResponse> search(String query);
}
