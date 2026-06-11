package com.sni.bokaticowork.features.billing.service.interfaces;

import com.sni.bokaticowork.features.billing.dto.request.CreateServiceCatalogItemRequest;
import com.sni.bokaticowork.features.billing.dto.request.UpdateServiceCatalogItemRequest;
import com.sni.bokaticowork.features.billing.dto.response.CatalogLookupItemResponse;
import com.sni.bokaticowork.features.billing.dto.response.ServiceCatalogItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ServiceCatalogService {

    ServiceCatalogItemResponse create(CreateServiceCatalogItemRequest request);

    Page<ServiceCatalogItemResponse> list(Boolean active, String category, String searchText, Pageable pageable);

    ServiceCatalogItemResponse update(String itemCode, UpdateServiceCatalogItemRequest request);

    ServiceCatalogItemResponse activate(String itemCode);

    ServiceCatalogItemResponse deactivate(String itemCode);

    void delete(String itemCode);

    List<CatalogLookupItemResponse> lookup(String q, List<String> sources, String category);
}
