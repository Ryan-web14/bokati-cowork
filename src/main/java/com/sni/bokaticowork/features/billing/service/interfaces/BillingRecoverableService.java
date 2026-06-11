package com.sni.bokaticowork.features.billing.service.interfaces;

import com.sni.bokaticowork.features.billing.dto.request.AddRecoverableItemsRequest;
import com.sni.bokaticowork.features.billing.dto.request.RecoverItemRequest;
import com.sni.bokaticowork.features.billing.dto.request.WriteOffRecoverableRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingAgingReportResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingRecoverableResponse;

import java.util.List;

public interface BillingRecoverableService {
    List<BillingRecoverableResponse> addItems(String documentNumber, AddRecoverableItemsRequest request);
    List<BillingRecoverableResponse> list(String documentNumber, String status);
    BillingRecoverableResponse markRecovered(String documentNumber, String recoverableNumber, RecoverItemRequest request);
    BillingRecoverableResponse writeOff(String documentNumber, String recoverableNumber, WriteOffRecoverableRequest request);
    BillingAgingReportResponse agingReport(String customerType, String customerCode, String currency);
}
