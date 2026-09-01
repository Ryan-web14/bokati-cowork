package com.sni.bokaticowork.features.ressource.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.ressource.dto.request.BulkCreateResourceAvailabilityRequest;
import com.sni.bokaticowork.features.ressource.dto.response.BulkResourceOperationResponse;
import com.sni.bokaticowork.features.ressource.dto.request.CreateResourceAvailabilityRequest;
import com.sni.bokaticowork.features.ressource.dto.request.ReleaseResourceAvailabilityRequest;
import com.sni.bokaticowork.features.ressource.dto.request.ReserveResourceAvailabilityRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceAvailabilityResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceAvailabilityGroupResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceAvailabilityWindowResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface ResourceAvailabilityService {

    void createAvailability(CreateResourceAvailabilityRequest request);

    /**
     * Ouvre la meme plage sur plusieurs ressources · chacune dans sa propre transaction, avec un
     * compte rendu par ressource plutot qu'un tout-ou-rien.
     */
    BulkResourceOperationResponse createAvailabilityBulk(BulkCreateResourceAvailabilityRequest request);

    PaginatedResponse<ResourceAvailabilityResponse> list(Pageable pageable);

    PaginatedResponse<ResourceAvailabilityResponse> listByResource(String resourceCode, Pageable pageable);

    List<ResourceAvailabilityGroupResponse> listGroupedByResource();

    List<ResourceAvailabilityWindowResponse> findRemainingWindows(
            String resourceCode,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            Integer durationMinutes,
            Integer quantity
    );

    void reserve(ReserveResourceAvailabilityRequest request);

    void release(ReleaseResourceAvailabilityRequest request);
}
