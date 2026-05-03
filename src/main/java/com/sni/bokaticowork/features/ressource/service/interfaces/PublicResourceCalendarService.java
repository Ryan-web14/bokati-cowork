package com.sni.bokaticowork.features.ressource.service.interfaces;

import com.sni.bokaticowork.features.ressource.dto.response.PublicResourceCalendarResponse;

import java.time.LocalDate;

public interface PublicResourceCalendarService {
    PublicResourceCalendarResponse calendar(String resourceCode, LocalDate fromDate, LocalDate toDate);
}
