package com.sni.bokaticowork.features.visitor.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.visitor.dto.VisitorDtos.*;
import com.sni.bokaticowork.features.visitor.enums.VisitorPassStatus;
import com.sni.bokaticowork.features.visitor.mapper.VisitorMapper;
import com.sni.bokaticowork.features.visitor.model.Visitor;
import com.sni.bokaticowork.features.visitor.model.VisitorCheckIn;
import com.sni.bokaticowork.features.visitor.model.VisitorPass;
import com.sni.bokaticowork.features.visitor.repository.VisitorCheckInRepository;
import com.sni.bokaticowork.features.visitor.repository.VisitorPassRepository;
import com.sni.bokaticowork.features.visitor.repository.VisitorRepository;
import com.sni.bokaticowork.features.visitor.service.interfaces.VisitorManagementService;
import com.sni.bokaticowork.features.visitor.service.support.VisitorBadgeService;
import com.sni.bokaticowork.features.visitor.service.support.VisitorEmailNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class VisitorManagementServiceImpl implements VisitorManagementService {
    private final VisitorRepository visitorRepository;
    private final VisitorPassRepository passRepository;
    private final VisitorCheckInRepository checkInRepository;
    private final VisitorMapper mapper;
    private final VisitorEmailNotifier emailNotifier;
    private final VisitorBadgeService badgeService;

    @Value("${app.time-zone:Africa/Lagos}")
    private String appTimeZone;

    @Override
    public VisitorPassResponse createPass(CreateVisitorPassRequest request) {
        if (request == null || !StringUtils.hasText(request.fullName()) || !StringUtils.hasText(request.validFrom()) || !StringUtils.hasText(request.validUntil())) {
            throw new BadRequestException("Visitor name and validity dates are required");
        }
        Instant validFrom = parseFrontendInstant(request.validFrom(), "validFrom");
        Instant validUntil = parseFrontendInstant(request.validUntil(), "validUntil");
        if (!validUntil.isAfter(validFrom)) {
            throw new BadRequestException("validUntil must be after validFrom");
        }
        Visitor visitor = visitorRepository.save(Visitor.builder()
                .fullName(request.fullName())
                .email(request.email())
                .phone(request.phone())
                .company(request.company())
                .build());
        String passNumber = "VIS-" + Instant.now().toEpochMilli();
        VisitorPass pass = passRepository.save(VisitorPass.builder()
                .passNumber(passNumber)
                .visitor(visitor)
                .hostMemberCode(request.hostMemberCode())
                .hostName(request.hostName())
                .validFrom(validFrom)
                .validUntil(validUntil)
                .purpose(request.purpose())
                .createdBy(request.createdBy())
                .status(VisitorPassStatus.SCHEDULED)
                .qrValue("bokati:visitor:" + passNumber)
                .build());
        emailNotifier.sendInvitation(pass);
        return mapper.toPassResponse(pass);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateBadge(String passNumber) {
        return badgeService.generateBadge(passNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VisitorPassResponse> list(VisitorPassStatus status) {
        List<VisitorPass> passes = status == null ? passRepository.findAll() : passRepository.findAllByStatus(status);
        return passes.stream().map(mapper::toPassResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VisitorPassResponse> today() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant start = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return passRepository.findAllByValidFromBeforeAndValidUntilAfter(end, start).stream()
                .map(mapper::toPassResponse)
                .toList();
    }

    @Override
    public VisitorPassResponse checkIn(String passNumber, CheckInRequest request) {
        VisitorPass pass = getPass(passNumber);
        if (pass.getStatus() == VisitorPassStatus.CHECKED_IN) {
            throw new BadRequestException("Visitor is already checked in");
        }
        pass.setStatus(VisitorPassStatus.CHECKED_IN);
        checkInRepository.save(VisitorCheckIn.builder()
                .pass(pass)
                .checkedInAt(Instant.now())
                .checkInAgent(request == null ? null : request.agent())
                .notes(request == null ? null : request.notes())
                .build());
        return mapper.toPassResponse(passRepository.save(pass));
    }

    @Override
    public VisitorPassResponse checkOut(String passNumber, CheckInRequest request) {
        VisitorPass pass = getPass(passNumber);
        VisitorCheckIn checkIn = checkInRepository.findFirstByPassAndCheckedOutAtIsNullOrderByCheckedInAtDesc(pass)
                .orElseThrow(() -> new BadRequestException("Visitor is not checked in"));
        checkIn.setCheckedOutAt(Instant.now());
        checkIn.setCheckOutAgent(request == null ? null : request.agent());
        if (request != null && StringUtils.hasText(request.notes())) {
            checkIn.setNotes(request.notes());
        }
        checkInRepository.save(checkIn);
        pass.setStatus(VisitorPassStatus.CHECKED_OUT);
        return mapper.toPassResponse(passRepository.save(pass));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VisitorLogResponse> log() {
        return checkInRepository.findAllByOrderByCheckedInAtDesc().stream()
                .map(mapper::toLogResponse)
                .toList();
    }

    private Instant parseFrontendInstant(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(fieldName + " is required");
        }
        String trimmed = value.trim();
        try {
            return Instant.parse(trimmed);
        } catch (Exception ignored) {
        }
        try {
            return OffsetDateTime.parse(trimmed).toInstant();
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(trimmed).atZone(ZoneId.of(appTimeZone)).toInstant();
        } catch (Exception ex) {
            throw new BadRequestException(fieldName + " must be a valid date-time", ex);
        }
    }

    private VisitorPass getPass(String passNumber) {
        if (!StringUtils.hasText(passNumber)) {
            throw new BadRequestException("Visitor pass number is required");
        }
        return passRepository.findByPassNumber(passNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor pass not found: " + passNumber));
    }
}
