package com.sni.bokaticowork.features.booking.service.implementation;

import com.sni.bokaticowork.features.booking.dto.response.BookingRepairResponse;
import com.sni.bokaticowork.features.booking.enums.BookingEventType;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingRepairService;
import com.sni.bokaticowork.features.booking.service.support.BookingEventWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class BookingRepairServiceImpl implements BookingRepairService {

    private final BookingRepository bookingRepository;
    private final BookingEventWriter eventWriter;

    @Override
    public BookingRepairResponse repair(int limit) {
        var candidates = bookingRepository.findRepairCandidates(limit);
        candidates.forEach(candidate -> eventWriter.write(candidate, BookingEventType.REPAIR_APPLIED, "Booking inspected by repair job", "No automatic mutation was required", null));
        return new BookingRepairResponse(candidates.size(), 0, 0);
    }
}
