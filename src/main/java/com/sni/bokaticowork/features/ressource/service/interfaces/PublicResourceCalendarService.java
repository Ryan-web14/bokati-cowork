package com.sni.bokaticowork.features.ressource.service.interfaces;

import com.sni.bokaticowork.features.ressource.dto.response.PublicResourceCalendarResponse;

import java.time.LocalDate;

public interface PublicResourceCalendarService {
    /** Le calendrier public · sans les soirees reservees aux abonnes. */
    PublicResourceCalendarResponse calendar(String resourceCode, LocalDate fromDate, LocalDate toDate);

    /** Le calendrier, avec les soirees des abonnes si le lecteur y a droit. */
    PublicResourceCalendarResponse calendar(String resourceCode, LocalDate fromDate, LocalDate toDate,
                                            boolean includeSubscribersOnly);
}
