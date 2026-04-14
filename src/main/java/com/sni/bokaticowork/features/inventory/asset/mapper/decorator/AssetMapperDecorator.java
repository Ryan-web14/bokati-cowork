package com.sni.bokaticowork.features.inventory.asset.mapper.decorator;

import com.sni.bokaticowork.features.inventory.asset.dto.request.AssetRequest;
import com.sni.bokaticowork.features.inventory.asset.dto.response.AssetAssignmentResponse;
import com.sni.bokaticowork.features.inventory.asset.dto.response.AssetResponse;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetCondition;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetStatus;
import com.sni.bokaticowork.features.inventory.asset.mapper.interfaces.AssetMapper;
import com.sni.bokaticowork.features.inventory.asset.model.Asset;
import com.sni.bokaticowork.features.inventory.asset.model.AssetAssignment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public abstract class AssetMapperDecorator implements AssetMapper {

    @Autowired
    @Qualifier("delegate")
    private AssetMapper delegate;

    @Override
    public Asset toEntity(AssetRequest request) {
        Asset asset = delegate.toEntity(request);
        applyDefaults(asset);
        return asset;
    }

    @Override
    public void updateEntity(Asset asset, AssetRequest request) {
        delegate.updateEntity(asset, request);
        applyDefaults(asset);
    }

    @Override
    public AssetResponse toResponse(Asset asset) {
        AssetResponse response = delegate.toResponse(asset);
        if (asset == null) return response;
        response.setItemCode(asset.getItem().getItemCode());
        response.setItemName(asset.getItem().getName());
        if (asset.getLocation() != null) {
            response.setLocationCode(asset.getLocation().getLocationCode());
            response.setLocationName(asset.getLocation().getName());
        }
        response.setDepreciatedValue(calculateDepreciatedValue(asset));
        return response;
    }

    @Override
    public AssetAssignmentResponse toAssignmentResponse(AssetAssignment assignment) {
        AssetAssignmentResponse response = delegate.toAssignmentResponse(assignment);
        if (assignment != null) {
            response.setAssetCode(assignment.getAsset().getAssetCode());
        }
        return response;
    }

    private void applyDefaults(Asset asset) {
        if (asset.getStatus() == null) asset.setStatus(AssetStatus.AVAILABLE);
        if (asset.getCondition() == null) asset.setCondition(AssetCondition.GOOD);
    }

    private Long calculateDepreciatedValue(Asset asset) {
        if (asset.getPurchaseCost() == null || asset.getUsefulLifeMonths() == null || asset.getUsefulLifeMonths() <= 0) {
            return asset.getPurchaseCost();
        }
        long residual = asset.getResidualValue() == null ? 0L : asset.getResidualValue();
        long depreciable = Math.max(0L, asset.getPurchaseCost() - residual);
        long elapsedMonths = asset.getPurchaseDate() == null ? 0L : Math.max(0L, ChronoUnit.MONTHS.between(asset.getPurchaseDate(), LocalDate.now()));
        long depreciation = Math.min(depreciable, depreciable * elapsedMonths / asset.getUsefulLifeMonths());
        return Math.max(residual, asset.getPurchaseCost() - depreciation);
    }
}
