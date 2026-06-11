package com.sni.bokaticowork.features.inventory.asset.service.interfaces;

import com.sni.bokaticowork.features.inventory.asset.dto.request.AssetAssignRequest;
import com.sni.bokaticowork.features.inventory.asset.dto.request.AssetRequest;
import com.sni.bokaticowork.features.inventory.asset.dto.request.AssetReserveRequest;
import com.sni.bokaticowork.features.inventory.asset.dto.request.AssetReturnRequest;
import com.sni.bokaticowork.features.inventory.asset.dto.response.AssetAssignmentResponse;
import com.sni.bokaticowork.features.inventory.asset.dto.response.AssetLocationHistoryResponse;
import com.sni.bokaticowork.features.inventory.asset.dto.response.AssetResponse;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetAssigneeType;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AssetService {

    AssetResponse create(AssetRequest request);

    AssetResponse update(String assetCode, AssetRequest request);

    AssetResponse get(String assetCode);

    Page<AssetResponse> search(String query, String itemCode, AssetStatus status, String locationCode,
                               AssetAssigneeType assignedToType, String assignedToCode, Pageable pageable);

    AssetAssignmentResponse assign(String assetCode, AssetAssignRequest request);

    AssetAssignmentResponse reserve(String assetCode, AssetReserveRequest request);

    AssetAssignmentResponse cancelReservation(String assetCode, String cancelledBy, String reason);

    AssetAssignmentResponse returnAsset(String assetCode, AssetReturnRequest request);

    AssetResponse markLost(String assetCode);

    AssetResponse markDamaged(String assetCode);

    AssetResponse retire(String assetCode);

    List<AssetLocationHistoryResponse> locationHistory(String assetCode);

    byte[] loanSheetPdf(String assetCode, Long assignmentId);
}
