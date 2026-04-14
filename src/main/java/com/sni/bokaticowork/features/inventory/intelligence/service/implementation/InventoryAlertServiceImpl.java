package com.sni.bokaticowork.features.inventory.intelligence.service.implementation;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.inventory.intelligence.dto.response.InventoryAlertResponse;
import com.sni.bokaticowork.features.inventory.intelligence.enums.InventoryAlertStatus;
import com.sni.bokaticowork.features.inventory.intelligence.mapper.interfaces.InventoryIntelligenceMapper;
import com.sni.bokaticowork.features.inventory.intelligence.model.InventoryAlert;
import com.sni.bokaticowork.features.inventory.intelligence.repository.InventoryAlertRepository;
import com.sni.bokaticowork.features.inventory.intelligence.service.interfaces.InventoryAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryAlertServiceImpl implements InventoryAlertService {

    private final InventoryAlertRepository repository;
    private final InventoryIntelligenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryAlertResponse> list(InventoryAlertStatus status, Pageable pageable) {
        return (status == null ? repository.findAllByOrderByCreatedAtDesc(pageable) : repository.findAllByStatusOrderByCreatedAtDesc(status, pageable))
                .map(mapper::toAlertResponse);
    }

    @Override
    public InventoryAlertResponse acknowledge(String alertCode) {
        InventoryAlert alert = byCode(alertCode);
        alert.setStatus(InventoryAlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedAt(Instant.now());
        return mapper.toAlertResponse(repository.save(alert));
    }

    @Override
    public InventoryAlertResponse resolve(String alertCode) {
        InventoryAlert alert = byCode(alertCode);
        alert.setStatus(InventoryAlertStatus.RESOLVED);
        alert.setResolvedAt(Instant.now());
        return mapper.toAlertResponse(repository.save(alert));
    }

    @Override
    public InventoryAlertResponse dismiss(String alertCode) {
        InventoryAlert alert = byCode(alertCode);
        alert.setStatus(InventoryAlertStatus.DISMISSED);
        alert.setResolvedAt(Instant.now());
        return mapper.toAlertResponse(repository.save(alert));
    }

    private InventoryAlert byCode(String alertCode) {
        return repository.findAll().stream()
                .filter(alert -> alert.getAlertCode().equalsIgnoreCase(alertCode))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Inventory alert not found"));
    }
}
