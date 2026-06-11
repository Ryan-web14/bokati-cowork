package com.sni.bokaticowork.features.payment.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.payment.dto.request.CreateCashRequestRequest;
import com.sni.bokaticowork.features.payment.dto.request.ReviewCashRequestRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashRequestResponse;
import com.sni.bokaticowork.features.payment.enums.CashRequestStatus;
import com.sni.bokaticowork.features.payment.enums.CashRequestType;
import org.springframework.data.domain.Pageable;

public interface CashRequestService {

    CashRequestResponse submit(String sessionNumber, CreateCashRequestRequest request);

    CashRequestResponse approve(String requestNumber, ReviewCashRequestRequest request);

    CashRequestResponse reject(String requestNumber, ReviewCashRequestRequest request);

    CashRequestResponse get(String requestNumber);

    PaginatedResponse<CashRequestResponse> list(String registerCode, String sessionNumber, CashRequestType requestType,
                                                CashRequestStatus status, String requestedBy, String searchText, Pageable pageable);
}
