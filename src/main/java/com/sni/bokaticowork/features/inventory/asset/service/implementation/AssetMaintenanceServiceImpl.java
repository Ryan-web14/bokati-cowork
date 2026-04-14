package com.sni.bokaticowork.features.inventory.asset.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.inventory.asset.dto.request.AssetMaintenanceCompleteRequest;
import com.sni.bokaticowork.features.inventory.asset.dto.request.AssetMaintenanceRequest;
import com.sni.bokaticowork.features.inventory.asset.dto.response.AssetMaintenanceResponse;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetCondition;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetMaintenanceStatus;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetStatus;
import com.sni.bokaticowork.features.inventory.asset.model.Asset;
import com.sni.bokaticowork.features.inventory.asset.model.AssetMaintenance;
import com.sni.bokaticowork.features.inventory.asset.mapper.interfaces.AssetMaintenanceMapper;
import com.sni.bokaticowork.features.inventory.asset.repository.AssetMaintenanceRepository;
import com.sni.bokaticowork.features.inventory.asset.repository.AssetRepository;
import com.sni.bokaticowork.features.inventory.asset.service.interfaces.AssetMaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AssetMaintenanceServiceImpl implements AssetMaintenanceService {

    private final AssetRepository assetRepository;
    private final AssetMaintenanceRepository maintenanceRepository;
    private final AssetMaintenanceMapper mapper;
    private final SequenceGeneratorFacade sequenceGenerator;

    @Override
    public AssetMaintenanceResponse plan(String assetCode, AssetMaintenanceRequest request) {
        Asset asset = findAssetForUpdate(assetCode);
        if (request.getMaintenanceType() == null) {
            throw new BadRequestException("Maintenance type is required");
        }
        AssetMaintenance maintenance = mapper.toEntity(request);
        maintenance.setMaintenanceCode(generateMaintenanceCode());
        maintenance.setAsset(asset);
        maintenance.setProviderName(trimToNull(request.getProviderName()));
        maintenance.setDescription(trimToNull(request.getDescription()));
        return mapper.toResponse(maintenanceRepository.save(maintenance));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetMaintenanceResponse> list(String assetCode) {
        Asset asset = findAsset(assetCode);
        return maintenanceRepository.findAllByAssetOrderByScheduledAtDesc(asset).stream().map(mapper::toResponse).toList();
    }

    @Override
    public AssetMaintenanceResponse start(String maintenanceCode) {
        AssetMaintenance maintenance = findMaintenance(maintenanceCode);
        if (maintenance.getStatus() != AssetMaintenanceStatus.PLANNED) {
            throw new BadRequestException("Only planned maintenance can be started");
        }
        Asset asset = assetRepository.findByAssetCodeForUpdate(maintenance.getAsset().getAssetCode())
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));
        if (asset.getStatus() == AssetStatus.ASSIGNED || asset.getStatus() == AssetStatus.IN_USE) {
            throw new BadRequestException("Assigned asset must be returned before maintenance");
        }
        asset.setStatus(AssetStatus.IN_MAINTENANCE);
        assetRepository.save(asset);
        maintenance.setStatus(AssetMaintenanceStatus.IN_PROGRESS);
        maintenance.setStartedAt(Instant.now());
        return mapper.toResponse(maintenanceRepository.save(maintenance));
    }

    @Override
    public AssetMaintenanceResponse complete(String maintenanceCode, AssetMaintenanceCompleteRequest request) {
        AssetMaintenance maintenance = findMaintenance(maintenanceCode);
        if (maintenance.getStatus() != AssetMaintenanceStatus.IN_PROGRESS) {
            throw new BadRequestException("Only in-progress maintenance can be completed");
        }
        Asset asset = assetRepository.findByAssetCodeForUpdate(maintenance.getAsset().getAssetCode())
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));
        AssetCondition condition = request.getAssetCondition() == null ? AssetCondition.GOOD : request.getAssetCondition();
        asset.setCondition(condition);
        asset.setStatus(condition == AssetCondition.DAMAGED || condition == AssetCondition.UNUSABLE
                ? AssetStatus.DAMAGED
                : AssetStatus.AVAILABLE);
        assetRepository.save(asset);
        maintenance.setStatus(AssetMaintenanceStatus.COMPLETED);
        maintenance.setCompletedAt(Instant.now());
        maintenance.setResolution(trimToNull(request.getResolution()));
        if (request.getCost() != null) {
            maintenance.setCost(request.getCost());
        }
        return mapper.toResponse(maintenanceRepository.save(maintenance));
    }

    @Override
    public AssetMaintenanceResponse cancel(String maintenanceCode) {
        AssetMaintenance maintenance = findMaintenance(maintenanceCode);
        if (maintenance.getStatus() == AssetMaintenanceStatus.COMPLETED) {
            throw new BadRequestException("Completed maintenance cannot be cancelled");
        }
        if (maintenance.getStatus() == AssetMaintenanceStatus.IN_PROGRESS) {
            Asset asset = assetRepository.findByAssetCodeForUpdate(maintenance.getAsset().getAssetCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));
            asset.setStatus(AssetStatus.AVAILABLE);
            assetRepository.save(asset);
        }
        maintenance.setStatus(AssetMaintenanceStatus.CANCELLED);
        return mapper.toResponse(maintenanceRepository.save(maintenance));
    }

    private Asset findAssetForUpdate(String assetCode) {
        return assetRepository.findByAssetCodeForUpdate(normalizeCode(assetCode))
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));
    }

    private Asset findAsset(String assetCode) {
        return assetRepository.findByAssetCode(normalizeCode(assetCode))
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));
    }

    private AssetMaintenance findMaintenance(String maintenanceCode) {
        return maintenanceRepository.findByMaintenanceCode(normalizeCode(maintenanceCode))
                .orElseThrow(() -> new ResourceNotFoundException("Asset maintenance not found"));
    }

    private String generateMaintenanceCode() {
        String code;
        do {
            code = sequenceGenerator.next("asset_maintenance") + "-" + System.currentTimeMillis();
        } while (maintenanceRepository.existsByMaintenanceCode(code));
        return code;
    }

    private String normalizeCode(String value) {
        if (!StringUtils.hasText(value)) throw new BadRequestException("Maintenance code is required");
        return value.trim().replaceAll("[^A-Za-z0-9]+", "-").replaceAll("-+", "-")
                .replaceAll("^-|-$", "").toUpperCase(java.util.Locale.ROOT);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
