package com.sni.bokaticowork.features.company.controller;

import com.sni.bokaticowork.core.baseClasses.dto.request.AddressRequest;
import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.company.dto.request.BusinessEntityRequest;
import com.sni.bokaticowork.features.company.dto.request.BusinessSearchCriteria;
import com.sni.bokaticowork.features.company.dto.request.UpdateBusinessStatusRequest;
import com.sni.bokaticowork.features.company.dto.response.BusinessEntityResponse;
import com.sni.bokaticowork.features.company.service.interfaces.BusinessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping(ApiPath.V1 + "/businesses")
public class BusinessController {

    private final BusinessService businessService;

    @PostMapping
    @Audited(module = "BUSINESS", action = "CREATE", ressource = "business")
    @Idempotent(operation = "BUSINESS_CREATE")
    public ResponseEntity<BusinessEntityResponse> createBusiness(@Valid @RequestBody BusinessEntityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(businessService.createBusiness(request));
    }

    @GetMapping("/{code}")
    public ResponseEntity<BusinessEntityResponse> getBusiness(@PathVariable String code) {
        return ResponseEntity.ok(businessService.getBusinessByCode(code));
    }

    @GetMapping("/by-name")
    public ResponseEntity<BusinessEntityResponse> getBusinessByName(@RequestParam String name) {
        return ResponseEntity.ok(businessService.getBusinessByName(name));
    }

    @GetMapping
    public ResponseEntity<List<BusinessEntityResponse>> listBusinesses() {
        return ResponseEntity.ok(businessService.getAllBusiness());
    }

    @GetMapping("/paged")
    public ResponseEntity<PaginatedResponse<BusinessEntityResponse>> listBusinessesPaged(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(businessService.list(pageable));
    }

    @GetMapping("/search/basic")
    public ResponseEntity<List<BusinessEntityResponse>> basicSearch(@RequestParam("query") String query) {
        return ResponseEntity.ok(businessService.basicSearch(query));
    }

    @PostMapping("/search")
    public ResponseEntity<PaginatedResponse<BusinessEntityResponse>> search(
            @RequestBody(required = false) BusinessSearchCriteria criteria,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(businessService.search(criteria, pageable));
    }

    @PutMapping("/{code}")
    @Audited(module = "BUSINESS", action = "UPDATE", ressource = "business")
    @Idempotent(operation = "BUSINESS_UPDATE")
    public ResponseEntity<BusinessEntityResponse> updateBusiness(@PathVariable String code,
                                                                 @Valid @RequestBody BusinessEntityRequest request) {
        return ResponseEntity.ok(businessService.updateBusiness(code, request));
    }

    @PatchMapping("/{code}/address")
    @Audited(module = "BUSINESS", action = "UPDATE_ADDRESS", ressource = "business")
    @Idempotent(operation = "BUSINESS_UPDATE_ADDRESS")
    public ResponseEntity<Void> updateBusinessAddress(@PathVariable String code,
                                                      @Valid @RequestBody AddressRequest request) {
        businessService.updateBusinessAddress(code, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{code}/status")
    @Audited(module = "BUSINESS", action = "UPDATE_STATUS", ressource = "business")
    @Idempotent(operation = "BUSINESS_UPDATE_STATUS")
    public ResponseEntity<Void> updateBusinessStatus(@PathVariable String code,
                                                     @Valid @RequestBody UpdateBusinessStatusRequest request) {
        businessService.updateBusinessStatus(code, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{code}")
    @Audited(module = "BUSINESS", action = "DELETE", ressource = "business")
    public ResponseEntity<Void> deleteBusiness(@PathVariable String code) {
        businessService.deleteBusiness(code);
        return ResponseEntity.noContent().build();
    }
}
