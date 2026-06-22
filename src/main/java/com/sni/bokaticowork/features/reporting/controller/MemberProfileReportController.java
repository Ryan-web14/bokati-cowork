package com.sni.bokaticowork.features.reporting.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.reporting.dto.response.MemberProfileReportResponse;
import com.sni.bokaticowork.features.reporting.service.interfaces.MemberProfileReportService;
import com.sni.bokaticowork.features.reporting.service.interfaces.ReportPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping({ApiPath.V1 + "/reports/member-profile", ApiPath.V1 + "/reporting/member-profile"})
public class MemberProfileReportController {

    private final MemberProfileReportService service;
    private final ReportPdfService reportPdfService;

    @GetMapping("/{memberId}")
    public ResponseEntity<MemberProfileReportResponse> memberProfile(@PathVariable String memberId) {
        return ResponseEntity.ok(service.memberProfile(memberId));
    }

    @GetMapping("/{memberId}/pdf")
    public ResponseEntity<byte[]> memberProfilePdf(@PathVariable String memberId) {
        byte[] pdf = reportPdfService.memberProfileReportPdf(memberId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("fiche-membre-" + memberId + ".pdf").build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
