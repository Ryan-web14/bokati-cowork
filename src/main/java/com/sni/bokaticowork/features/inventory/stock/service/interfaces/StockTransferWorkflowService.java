package com.sni.bokaticowork.features.inventory.stock.service.interfaces;

import com.sni.bokaticowork.features.inventory.stock.dto.request.StockTransferWorkflowRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockTransferWorkflowResponse;
import com.sni.bokaticowork.features.inventory.stock.enums.StockTransferWorkflowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StockTransferWorkflowService {
    StockTransferWorkflowResponse request(StockTransferWorkflowRequest request);

    StockTransferWorkflowResponse approve(String transferCode, String approvedBy);

    StockTransferWorkflowResponse ship(String transferCode, String shippedBy);

    StockTransferWorkflowResponse receive(String transferCode, String receivedBy);

    StockTransferWorkflowResponse cancel(String transferCode, String cancelledBy, String reason);

    Page<StockTransferWorkflowResponse> list(StockTransferWorkflowStatus status, Pageable pageable);
}
