package com.sni.bokaticowork.features.client.customer.service.interfaces;

import com.sni.bokaticowork.features.client.customer.dto.request.CustomerRequest;
import com.sni.bokaticowork.features.client.customer.dto.request.ChangeCustomerStatusRequest;
import com.sni.bokaticowork.features.client.customer.dto.response.CustomerResponse;
import com.sni.bokaticowork.features.client.customer.dto.response.CustomerSummaryresponse;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomerService {

    Customer createCustomer(CustomerRequest request);
    Customer createCustomerForMember(CustomerRequest request);
    void updateCustomer(String customerId, CustomerRequest request);
    void changeStatus(String customerId, ChangeCustomerStatusRequest request);
    CustomerResponse getCustomerByCustomerId(String code);
    CustomerResponse getCustomerByEmail(String email);
    List<CustomerResponse> getAllCustomers();
    List<CustomerSummaryresponse> getAllCustomersSummary();
    List<CustomerSummaryresponse> basicSearch(String query, CustomerType type);
    PaginatedResponse<CustomerResponse> list(Pageable page);
    PaginatedResponse<CustomerSummaryresponse> listSummary(Pageable page);
    void deleteCustomer(String customerId);
    Customer getCustomerForService(Long id);
    Customer getCustomerForService(String id);

}

