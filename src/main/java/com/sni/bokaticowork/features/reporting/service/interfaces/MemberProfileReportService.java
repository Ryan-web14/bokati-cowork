package com.sni.bokaticowork.features.reporting.service.interfaces;

import com.sni.bokaticowork.features.reporting.dto.response.MemberProfileReportResponse;

public interface MemberProfileReportService {

    MemberProfileReportResponse memberProfile(String memberId);
}
