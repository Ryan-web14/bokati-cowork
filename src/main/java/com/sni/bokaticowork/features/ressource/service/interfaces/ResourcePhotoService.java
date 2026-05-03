package com.sni.bokaticowork.features.ressource.service.interfaces;

import com.sni.bokaticowork.features.ressource.dto.request.ResourcePhotoRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourcePhotoResponse;

import java.util.List;

public interface ResourcePhotoService {
    ResourcePhotoResponse add(String resourceCode, ResourcePhotoRequest request);
    List<ResourcePhotoResponse> list(String resourceCode);
    ResourcePhotoResponse update(Long id, ResourcePhotoRequest request);
    void delete(Long id);
}
