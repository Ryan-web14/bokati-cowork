package com.sni.bokaticowork.features.booking.service.interfaces;

import com.sni.bokaticowork.features.booking.dto.response.BookingRepairResponse;

public interface BookingRepairService {
    BookingRepairResponse repair(int limit);
}
