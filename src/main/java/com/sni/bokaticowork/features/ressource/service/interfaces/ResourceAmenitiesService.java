package com.sni.bokaticowork.features.ressource.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.ressource.dto.request.CreateAmenityRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateAmenityRequest;
import com.sni.bokaticowork.features.ressource.dto.response.AmenityResponse;
import com.sni.bokaticowork.features.ressource.model.ResourceAmenities;
import org.springframework.data.domain.Pageable;

public interface ResourceAmenitiesService {

    void createAmenity(CreateAmenityRequest request);
    void updateAmenity(String code, UpdateAmenityRequest request);
    void deleteAmenity(String code);
    void deleteAllAmenities();
    AmenityResponse getAmenity(String code);
    ResourceAmenities getAmenityForService(String code);
    ResourceAmenities getAmenityForService(Long id);
    PaginatedResponse<AmenityResponse> list(Pageable pageable);
    PaginatedResponse<AmenityResponse> search(String query);
}
