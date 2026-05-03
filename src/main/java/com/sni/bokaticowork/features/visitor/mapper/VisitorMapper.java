package com.sni.bokaticowork.features.visitor.mapper;

import com.sni.bokaticowork.features.visitor.dto.VisitorDtos.VisitorLogResponse;
import com.sni.bokaticowork.features.visitor.dto.VisitorDtos.VisitorPassResponse;
import com.sni.bokaticowork.features.visitor.model.VisitorCheckIn;
import com.sni.bokaticowork.features.visitor.model.VisitorPass;
import org.springframework.stereotype.Component;

@Component
public class VisitorMapper {
    public VisitorPassResponse toPassResponse(VisitorPass pass) {
        return new VisitorPassResponse(
                pass.getPassNumber(),
                pass.getVisitor().getFullName(),
                pass.getVisitor().getEmail(),
                pass.getVisitor().getPhone(),
                pass.getVisitor().getCompany(),
                pass.getHostMemberCode(),
                pass.getHostName(),
                pass.getValidFrom(),
                pass.getValidUntil(),
                pass.getPurpose(),
                pass.getStatus(),
                pass.getQrValue()
        );
    }

    public VisitorLogResponse toLogResponse(VisitorCheckIn checkIn) {
        VisitorPass pass = checkIn.getPass();
        return new VisitorLogResponse(
                pass.getPassNumber(),
                pass.getVisitor().getFullName(),
                pass.getHostName(),
                checkIn.getCheckedInAt(),
                checkIn.getCheckedOutAt(),
                checkIn.getCheckInAgent(),
                checkIn.getCheckOutAgent(),
                checkIn.getNotes()
        );
    }
}
