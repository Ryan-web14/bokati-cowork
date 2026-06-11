package com.sni.bokaticowork.features.inventory.asset.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.inventory.asset.dto.request.AssetAssignRequest;
import com.sni.bokaticowork.features.inventory.asset.dto.request.AssetRequest;
import com.sni.bokaticowork.features.inventory.asset.dto.request.AssetReserveRequest;
import com.sni.bokaticowork.features.inventory.asset.dto.request.AssetReturnRequest;
import com.sni.bokaticowork.features.inventory.asset.dto.response.AssetAssignmentResponse;
import com.sni.bokaticowork.features.inventory.asset.dto.response.AssetLocationHistoryResponse;
import com.sni.bokaticowork.features.inventory.asset.dto.response.AssetResponse;
import com.sni.bokaticowork.features.inventory.asset.enums.*;
import com.sni.bokaticowork.features.inventory.asset.model.Asset;
import com.sni.bokaticowork.features.inventory.asset.model.AssetAssignment;
import com.sni.bokaticowork.features.inventory.asset.model.AssetLocationHistory;
import com.sni.bokaticowork.features.inventory.asset.mapper.interfaces.AssetMapper;
import com.sni.bokaticowork.features.inventory.asset.repository.AssetAssignmentRepository;
import com.sni.bokaticowork.features.inventory.asset.repository.AssetLocationHistoryRepository;
import com.sni.bokaticowork.features.inventory.asset.repository.AssetRepository;
import com.sni.bokaticowork.features.inventory.asset.service.support.AssetLoanSheetPdfRenderer;
import com.sni.bokaticowork.features.inventory.asset.repository.specification.AssetSpecification;
import com.sni.bokaticowork.features.inventory.intelligence.service.interfaces.InventoryAutomationService;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryItemType;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemLookupService;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.InventoryLocationService;
import com.sni.bokaticowork.features.inventory.asset.service.interfaces.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;
    private final AssetAssignmentRepository assignmentRepository;
    private final AssetLocationHistoryRepository locationHistoryRepository;
    private final InventoryItemLookupService itemLookupService;
    private final InventoryLocationService locationService;
    private final AssetMapper mapper;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final InventoryAutomationService automationService;
    private final AssetLoanSheetPdfRenderer loanSheetPdfRenderer;

    @Override
    public AssetResponse create(AssetRequest request) {
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(request.getItemCode());
        if (item.getItemType() != InventoryItemType.ASSET) {
            throw new BadRequestException("Inventory item must be ASSET to create an asset");
        }
        Asset asset = mapper.toEntity(request);
        asset.setAssetCode(normalizeCode(generateAssetCode(item)));
        if (assetRepository.existsByAssetCode(asset.getAssetCode())) {
            throw new ResourceAlreadyExistException("Asset code already exists");
        }
        apply(asset, request, item);
        Asset saved = assetRepository.save(asset);
        recordLocationHistoryIfChanged(saved, null, saved.getLocation(), null, "Asset created");
        return mapper.toResponse(saved);
    }

    @Override
    public AssetResponse update(String assetCode, AssetRequest request) {
        Asset asset = findForUpdate(assetCode);
        InventoryLocation previousLocation = asset.getLocation();
        apply(asset, request, asset.getItem());
        Asset saved = assetRepository.save(asset);
        recordLocationHistoryIfChanged(saved, previousLocation, saved.getLocation(), null, "Asset location updated");
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AssetResponse get(String assetCode) {
        return mapper.toResponse(findByCode(assetCode));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetResponse> search(String query, String itemCode, AssetStatus status, String locationCode,
                                      AssetAssigneeType assignedToType, String assignedToCode, Pageable pageable) {
        if (!StringUtils.hasText(query)) {
            return assetRepository.findAll(AssetSpecification.filters(itemCode, status, locationCode, assignedToType, assignedToCode), pageable)
                    .map(mapper::toResponse);
        }
        return assetRepository.nativeSearch(trimToNull(query), normalizeOptionalCode(itemCode),
                        status == null ? null : status.name(),
                        normalizeOptionalCode(locationCode),
                        assignedToType == null ? null : assignedToType.name(),
                        normalizeOptionalCode(assignedToCode), pageable)
                .map(mapper::toResponse);
    }

    @Override
    public AssetAssignmentResponse assign(String assetCode, AssetAssignRequest request) {
        Asset asset = findForUpdate(assetCode);
        AssetAssignment assignment = findConvertibleReservation(asset, request);
        if (assignment == null && asset.getStatus() != AssetStatus.AVAILABLE) {
            throw new BadRequestException("Asset is not available for assignment");
        }
        if (request.getAssigneeType() == null || !StringUtils.hasText(request.getAssigneeCode())) {
            throw new BadRequestException("Asset assignee is required");
        }
        if (assignment == null) {
            assignment = AssetAssignment.builder()
                    .asset(asset)
                    .assigneeType(request.getAssigneeType())
                    .assigneeCode(normalizeCode(request.getAssigneeCode()))
                    .build();
        }
        assignment.setStatus(AssetAssignmentStatus.ACTIVE);
        assignment.setStartAt(request.getStartAt() == null ? Instant.now() : request.getStartAt());
        assignment.setExpectedReturnAt(request.getExpectedReturnAt());
        assignment.setAssignedBy(trimToNull(request.getAssignedBy()));
        assignment.setPurpose(trimToNull(request.getPurpose()));
        assignment.setCheckoutCondition(asset.getCondition());
        assignment.setCheckoutPhotoUrl(trimToNull(request.getCheckoutPhotoUrl()));
        assignment.setReceiverSignatureUrl(trimToNull(request.getReceiverSignatureUrl()));
        assignment.setNotes(trimToNull(request.getNotes()));
        asset.setStatus(AssetStatus.ASSIGNED);
        asset.setAssignedToType(request.getAssigneeType());
        asset.setAssignedToCode(normalizeCode(request.getAssigneeCode()));
        assetRepository.save(asset);
        AssetAssignment saved = assignmentRepository.save(assignment);
        automationService.publishAssetEvent("inventory.asset.assigned", asset.getAssetCode(), java.util.Map.of(
                "assetCode", asset.getAssetCode(),
                "assigneeType", request.getAssigneeType(),
                "assigneeCode", asset.getAssignedToCode()
        ));
        return mapper.toAssignmentResponse(saved);
    }

    @Override
    public AssetAssignmentResponse reserve(String assetCode, AssetReserveRequest request) {
        Asset asset = findForUpdate(assetCode);
        if (asset.getStatus() != AssetStatus.AVAILABLE) {
            throw new BadRequestException("Asset is not available for reservation");
        }
        Instant startAt = request.getReservedFrom() == null ? Instant.now() : request.getReservedFrom();
        Instant endAt = request.getExpectedReturnAt() == null ? Instant.parse("9999-12-31T00:00:00Z") : request.getExpectedReturnAt();
        if (!endAt.isAfter(startAt)) {
            throw new BadRequestException("Reservation expected return date must be after start date");
        }
        if (assignmentRepository.existsOverlappingReservationOrAssignment(asset,
                Set.of(AssetAssignmentStatus.RESERVED, AssetAssignmentStatus.ACTIVE), startAt, endAt)) {
            throw new BadRequestException("Asset already has an overlapping reservation or assignment");
        }
        AssetAssignment reservation = AssetAssignment.builder()
                .asset(asset)
                .assigneeType(request.getAssigneeType())
                .assigneeCode(normalizeCode(request.getAssigneeCode()))
                .status(AssetAssignmentStatus.RESERVED)
                .startAt(startAt)
                .expectedReturnAt(request.getExpectedReturnAt())
                .assignedBy(trimToNull(request.getReservedBy()))
                .notes(trimToNull(request.getNotes()))
                .build();
        asset.setStatus(AssetStatus.RESERVED);
        asset.setAssignedToType(request.getAssigneeType());
        asset.setAssignedToCode(normalizeCode(request.getAssigneeCode()));
        assetRepository.save(asset);
        AssetAssignment saved = assignmentRepository.save(reservation);
        automationService.publishAssetEvent("inventory.asset.reserved", asset.getAssetCode(), java.util.Map.of(
                "assetCode", asset.getAssetCode(),
                "assigneeType", request.getAssigneeType(),
                "assigneeCode", asset.getAssignedToCode()
        ));
        return mapper.toAssignmentResponse(saved);
    }

    @Override
    public AssetAssignmentResponse cancelReservation(String assetCode, String cancelledBy, String reason) {
        Asset asset = findForUpdate(assetCode);
        AssetAssignment reservation = assignmentRepository.findFirstByAssetAndStatusOrderByStartAtDesc(asset, AssetAssignmentStatus.RESERVED)
                .orElseThrow(() -> new ResourceNotFoundException("No active asset reservation found"));
        reservation.setStatus(AssetAssignmentStatus.CANCELLED);
        reservation.setEndAt(Instant.now());
        reservation.setReturnedBy(trimToNull(cancelledBy));
        reservation.setNotes(trimToNull(reason));
        asset.setStatus(AssetStatus.AVAILABLE);
        asset.setAssignedToType(null);
        asset.setAssignedToCode(null);
        assetRepository.save(asset);
        AssetAssignment saved = assignmentRepository.save(reservation);
        automationService.publishAssetEvent("inventory.asset.reservation_cancelled", asset.getAssetCode(), java.util.Map.of("assetCode", asset.getAssetCode()));
        return mapper.toAssignmentResponse(saved);
    }

    @Override
    public AssetAssignmentResponse returnAsset(String assetCode, AssetReturnRequest request) {
        Asset asset = findForUpdate(assetCode);
        AssetAssignment assignment = assignmentRepository.findFirstByAssetAndStatusOrderByStartAtDesc(asset, AssetAssignmentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No active asset assignment found"));
        assignment.setStatus(AssetAssignmentStatus.RETURNED);
        assignment.setEndAt(Instant.now());
        assignment.setReturnedBy(trimToNull(request.getReturnedBy()));
        assignment.setReturnCondition(request.getReturnCondition());
        assignment.setReturnPhotoUrl(trimToNull(request.getReturnPhotoUrl()));
        assignment.setNotes(trimToNull(request.getNotes()));
        asset.setAssignedToType(null);
        asset.setAssignedToCode(null);
        asset.setCondition(request.getReturnCondition() == null ? asset.getCondition() : request.getReturnCondition());
        asset.setStatus(asset.getCondition() == AssetCondition.DAMAGED || asset.getCondition() == AssetCondition.UNUSABLE
                ? AssetStatus.DAMAGED
                : AssetStatus.AVAILABLE);
        assetRepository.save(asset);
        AssetAssignment saved = assignmentRepository.save(assignment);
        automationService.publishAssetEvent("inventory.asset.returned", asset.getAssetCode(), java.util.Map.of(
                "assetCode", asset.getAssetCode(),
                "status", asset.getStatus(),
                "condition", asset.getCondition()
        ));
        return mapper.toAssignmentResponse(saved);
    }

    @Override
    public AssetResponse markLost(String assetCode) {
        Asset asset = findForUpdate(assetCode);
        closeActiveAssignment(asset, AssetAssignmentStatus.CANCELLED);
        asset.setStatus(AssetStatus.LOST);
        asset.setAssignedToType(null);
        asset.setAssignedToCode(null);
        return mapper.toResponse(assetRepository.save(asset));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetLocationHistoryResponse> locationHistory(String assetCode) {
        Asset asset = findByCode(assetCode);
        return locationHistoryRepository.findAllByAssetOrderByChangedAtDesc(asset).stream()
                .map(history -> AssetLocationHistoryResponse.builder()
                        .assetCode(asset.getAssetCode())
                        .fromLocationCode(history.getFromLocation() == null ? null : history.getFromLocation().getLocationCode())
                        .toLocationCode(history.getToLocation() == null ? null : history.getToLocation().getLocationCode())
                        .changedBy(history.getChangedBy())
                        .reason(history.getReason())
                        .changedAt(history.getChangedAt())
                        .build())
                .toList();
    }

    @Override
    public AssetResponse markDamaged(String assetCode) {
        Asset asset = findForUpdate(assetCode);
        asset.setStatus(AssetStatus.DAMAGED);
        asset.setCondition(AssetCondition.DAMAGED);
        return mapper.toResponse(assetRepository.save(asset));
    }

    @Override
    public AssetResponse retire(String assetCode) {
        Asset asset = findForUpdate(assetCode);
        closeActiveAssignment(asset, AssetAssignmentStatus.CANCELLED);
        asset.setStatus(AssetStatus.RETIRED);
        asset.setAssignedToType(null);
        asset.setAssignedToCode(null);
        return mapper.toResponse(assetRepository.save(asset));
    }

    private void apply(Asset asset, AssetRequest request, InventoryItem item) {
        asset.setItem(item);
        asset.setSerialNumber(normalizeOptionalCode(request.getSerialNumber()));
        asset.setAssetTag(normalizeOptionalCode(StringUtils.hasText(request.getAssetTag()) ? request.getAssetTag() : generateAssetTag(asset.getAssetCode(), item)));
        asset.setStatus(request.getStatus() == null ? defaultStatus(asset) : request.getStatus());
        asset.setCondition(request.getCondition() == null ? defaultCondition(asset) : request.getCondition());
        asset.setLocation(StringUtils.hasText(request.getLocationCode()) ? locationService.findByLocationCodeOrThrow(request.getLocationCode()) : null);
        asset.setPurchaseDate(request.getPurchaseDate());
        asset.setPurchaseCost(request.getPurchaseCost());
        asset.setWarrantyEndDate(request.getWarrantyEndDate());
        asset.setUsefulLifeMonths(request.getUsefulLifeMonths());
        asset.setResidualValue(request.getResidualValue());
        asset.setNotes(trimToNull(request.getNotes()));
        ensureUniqueCodes(asset);
    }

    private AssetAssignment findConvertibleReservation(Asset asset, AssetAssignRequest request) {
        if (asset.getStatus() != AssetStatus.RESERVED) {
            return null;
        }
        AssetAssignment reservation = assignmentRepository.findFirstByAssetAndStatusOrderByStartAtDesc(asset, AssetAssignmentStatus.RESERVED)
                .orElseThrow(() -> new BadRequestException("Reserved asset has no reservation record"));
        String assigneeCode = normalizeCode(request.getAssigneeCode());
        if (reservation.getAssigneeType() != request.getAssigneeType() || !reservation.getAssigneeCode().equals(assigneeCode)) {
            throw new BadRequestException("Reserved asset can only be assigned to the reserved assignee");
        }
        return reservation;
    }

    private void recordLocationHistoryIfChanged(Asset asset, InventoryLocation from, InventoryLocation to, String changedBy, String reason) {
        Long fromId = from == null ? null : from.getId();
        Long toId = to == null ? null : to.getId();
        if (java.util.Objects.equals(fromId, toId)) {
            return;
        }
        locationHistoryRepository.save(AssetLocationHistory.builder()
                .asset(asset)
                .fromLocation(from)
                .toLocation(to)
                .changedBy(trimToNull(changedBy))
                .reason(trimToNull(reason))
                .changedAt(Instant.now())
                .build());
    }

    private void ensureUniqueCodes(Asset asset) {
        if (assetRepository.existsConflictingCodes(asset.getId(), asset.getSerialNumber(), asset.getAssetTag())) {
            throw new ResourceAlreadyExistException("Asset serial number or tag already exists");
        }
    }

    private void closeActiveAssignment(Asset asset, AssetAssignmentStatus status) {
        assignmentRepository.findFirstByAssetAndStatusOrderByStartAtDesc(asset, AssetAssignmentStatus.ACTIVE)
                .ifPresent(assignment -> {
                    assignment.setStatus(status);
                    assignment.setEndAt(Instant.now());
                    assignmentRepository.save(assignment);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] loanSheetPdf(String assetCode, Long assignmentId) {
        Asset asset = findByCode(assetCode);
        AssetAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException("Asset assignment not found"));
        if (!assignment.getAsset().getId().equals(asset.getId())) {
            throw new BadRequestException("Assignment does not belong to this asset");
        }
        return loanSheetPdfRenderer.render(asset, assignment);
    }

    private Asset findForUpdate(String assetCode) {
        return assetRepository.findByAssetCodeForUpdate(normalizeCode(assetCode))
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));
    }

    private Asset findByCode(String assetCode) {
        return assetRepository.findByAssetCode(normalizeCode(assetCode))
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));
    }

    private AssetStatus defaultStatus(Asset asset) {
        return asset.getStatus() == null ? AssetStatus.AVAILABLE : asset.getStatus();
    }

    private AssetCondition defaultCondition(Asset asset) {
        return asset.getCondition() == null ? AssetCondition.GOOD : asset.getCondition();
    }

    private String normalizeCode(String value) {
        if (!StringUtils.hasText(value)) throw new BadRequestException("Asset code is required");
        return value.trim().replaceAll("[^A-Za-z0-9]+", "-").replaceAll("-+", "-")
                .replaceAll("^-|-$", "").toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalCode(String value) {
        return StringUtils.hasText(value) ? normalizeCode(value) : null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String generateAssetCode(InventoryItem item) {
        String seq = sequenceGenerator.next("asset");
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String itemPart = normalizeCode(item.getItemCode());
        if (itemPart.length() > 12) {
            itemPart = itemPart.substring(0, 12);
        }
        return "AST-" + itemPart + "-" + yearMonth + "-" + zeroPadSeq(seq, 5);
    }

    private String generateAssetTag(String assetCode, InventoryItem item) {
        String hex = String.format("%04X", System.currentTimeMillis() & 0xFFFFL);
        String core = assetCode.startsWith("AST-") ? assetCode.substring(4) : assetCode;
        return "TAG-" + core + "-" + hex;
    }

    private String zeroPadSeq(String seq, int length) {
        String digits = seq.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return seq;
        try {
            return String.format("%0" + length + "d", Long.parseLong(digits));
        } catch (NumberFormatException e) {
            return seq;
        }
    }
}
