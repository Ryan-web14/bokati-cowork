package com.sni.bokaticowork.features.ressource.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.ressource.dto.request.CreateResourceClosureRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceClosureResponse;
import org.springframework.data.domain.Pageable;

public interface ResourceClosureService {

    void createClosure(CreateResourceClosureRequest request);

    ResourceClosureResponse getClosure(Long id);

    PaginatedResponse<ResourceClosureResponse> list(Pageable pageable);

    PaginatedResponse<ResourceClosureResponse> listByResource(String resourceCode, Pageable pageable);

    void updateActive(Long id, Boolean active);

    void deleteClosure(Long id);
}
