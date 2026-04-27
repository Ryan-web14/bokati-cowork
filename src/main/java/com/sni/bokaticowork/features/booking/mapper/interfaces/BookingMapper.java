package com.sni.bokaticowork.features.booking.mapper.interfaces;

import com.sni.bokaticowork.features.booking.dto.request.BookingParticipantRequest;
import com.sni.bokaticowork.features.booking.dto.request.CreateBookingRequest;
import com.sni.bokaticowork.features.booking.dto.response.*;
import com.sni.bokaticowork.features.booking.mapper.decorator.BookingMapperDecorator;
import com.sni.bokaticowork.features.booking.model.*;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(BookingMapperDecorator.class)
public interface BookingMapper {

    default Booking toEntity(CreateBookingRequest request) {
        return null;
    }

    default BookingParticipant toEntity(BookingParticipantRequest request) {
        return null;
    }

    default BookingResponse toResponse(Booking booking) {
        return null;
    }

    default BookingLineResponse toResponse(BookingLine line) {
        return null;
    }

    default BookingParticipantResponse toResponse(BookingParticipant participant) {
        return null;
    }

    default BookingStatusHistoryResponse toResponse(BookingStatusHistory history) {
        return null;
    }

    default BookingEventResponse toResponse(BookingEvent event) {
        return null;
    }
}
