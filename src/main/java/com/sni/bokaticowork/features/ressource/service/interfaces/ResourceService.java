package com.sni.bokaticowork.features.ressource.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.ressource.dto.request.ChangeTypeRequest;
import com.sni.bokaticowork.features.ressource.dto.request.LinkAmenityToResourceRequest;
import com.sni.bokaticowork.features.ressource.dto.request.ResourceRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateResourceRequest;
import com.sni.bokaticowork.features.ressource.dto.response.AmenityResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceSummaryResponse;
import com.sni.bokaticowork.features.ressource.enums.ResourceStatus;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.repository.specification.criteria.ResourceSearchCriteria;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ResourceService {

    void createResource(ResourceRequest request);
    void updateResource(String code, UpdateResourceRequest request);
    void deleteResource(String code);
    void deleteAllResources();

    ResourceResponse getResource(String code);
    Resource getResourceForService(String code);
    Resource getResourceForService(Long id);

    PaginatedResponse<ResourceResponse> list(Pageable pageable);
    PaginatedResponse<ResourceSummaryResponse> listSummary(Pageable pageable);
    PaginatedResponse<ResourceSummaryResponse> listSummaryByType(String typeCode, Pageable pageable);
    PaginatedResponse<ResourceSummaryResponse> listSummaryByGroup(String groupCode, Pageable pageable);
    PaginatedResponse<ResourceSummaryResponse> listSummaryByPolicy(String policyCode, Pageable pageable);
    List<ResourceSummaryResponse> basicSearch(String query);
    PaginatedResponse<ResourceSummaryResponse> search(ResourceSearchCriteria criteria, Pageable pageable);

    void changeClassification(String code, ChangeTypeRequest request);
    void updateStatus(String code, ResourceStatus status);
    void updateBookingEnabled(String code, Boolean bookingEnabled);
    void updatePortalVisible(String code, Boolean portalVisible);
    void updateActive(String code, Boolean active);
    void updateDisplayOrder(String code, Integer displayOrder);
    void linkAmenity(LinkAmenityToResourceRequest request);
    void unlinkAmenity(String resourceCode, String amenityCode);
    List<AmenityResponse> listAmenities(String resourceCode);
}
