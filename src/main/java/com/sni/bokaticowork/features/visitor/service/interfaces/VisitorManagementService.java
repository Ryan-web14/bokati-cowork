package com.sni.bokaticowork.features.visitor.service.interfaces;

import com.sni.bokaticowork.features.visitor.dto.VisitorDtos.*;
import com.sni.bokaticowork.features.visitor.enums.VisitorPassStatus;

import java.util.List;

public interface VisitorManagementService {
    VisitorPassResponse createPass(CreateVisitorPassRequest request);
    List<VisitorPassResponse> list(VisitorPassStatus status);
    List<VisitorPassResponse> today();
    VisitorPassResponse checkIn(String passNumber, CheckInRequest request);
    VisitorPassResponse checkOut(String passNumber, CheckInRequest request);
    List<VisitorLogResponse> log();
    byte[] generateBadge(String passNumber);
}
