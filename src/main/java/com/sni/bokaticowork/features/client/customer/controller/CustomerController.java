package com.sni.bokaticowork.features.client.customer.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.client.customer.dto.request.CustomerRequest;
import com.sni.bokaticowork.features.client.customer.dto.request.ChangeCustomerStatusRequest;
import com.sni.bokaticowork.features.client.customer.dto.response.CustomerResponse;
import com.sni.bokaticowork.features.client.customer.dto.response.CustomerSummaryresponse;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.service.interfaces.CustomerService;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/customers")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @Audited(module = "CUSTOMER", action = "CREATE", ressource = "customer")
    @Idempotent(operation = "CUSTOMER_CREATE")
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        Customer customer = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerService.getCustomerByCustomerId(customer.getCustomerId()));
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getByCustomerId(@PathVariable String customerId) {
        return ResponseEntity.ok(customerService.getCustomerByCustomerId(customerId));
    }

    @GetMapping("/by-email")
    public ResponseEntity<CustomerResponse> getByEmail(@RequestParam String email) {
        return ResponseEntity.ok(customerService.getCustomerByEmail(email));
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "false") boolean summary,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (summary) {
            PaginatedResponse<CustomerSummaryresponse> response = customerService.listSummary(pageable);
            return ResponseEntity.ok(response);
        }

        PaginatedResponse<CustomerResponse> response = customerService.list(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<PaginatedResponse<CustomerSummaryresponse>> listSummary(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(customerService.listSummary(pageable));
    }

    @GetMapping("/summary/all")
    public ResponseEntity<List<CustomerSummaryresponse>> listAllSummary() {
        return ResponseEntity.ok(customerService.getAllCustomersSummary());
    }

    @GetMapping("/search/basic")
    public ResponseEntity<List<CustomerSummaryresponse>> basicSearch(
            @RequestParam("query") String query,
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(customerService.basicSearch(query, parseCustomerType(type)));
    }

    @PatchMapping("/{customerId}")
    @Audited(module = "CUSTOMER", action = "UPDATE", ressource = "customer")
    public ResponseEntity<Void> update(@PathVariable String customerId, @Valid @RequestBody CustomerRequest request) {
        customerService.updateCustomer(customerId, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{customerId}/status")
    @Audited(module = "CUSTOMER", action = "CHANGE_STATUS", ressource = "customer")
    @Idempotent(operation = "CUSTOMER_CHANGE_STATUS", required = false)
    public ResponseEntity<Void> changeStatus(@PathVariable String customerId,
                                             @Valid @RequestBody ChangeCustomerStatusRequest request) {
        customerService.changeStatus(customerId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{customerId}")
    @Audited(module = "CUSTOMER", action = "DELETE", ressource = "customer")
    public ResponseEntity<Void> delete(@PathVariable String customerId) {
        customerService.deleteCustomer(customerId);
        return ResponseEntity.noContent().build();
    }

    private CustomerType parseCustomerType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return CustomerType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid customer type: " + value, ex);
        }
    }
}
