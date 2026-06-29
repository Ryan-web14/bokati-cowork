package com.sni.bokaticowork.features.ressource.service.interfaces;

import com.sni.bokaticowork.features.ressource.dto.response.AdminResourceCalendarResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceOccupiedSlotResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface AdminResourceCalendarService {

    AdminResourceCalendarResponse calendar(String resourceCode, LocalDate fromDate, LocalDate toDate);

    List<ResourceOccupiedSlotResponse> occupiedSlots(String resourceCode, LocalDate fromDate, LocalDate toDate);
}
