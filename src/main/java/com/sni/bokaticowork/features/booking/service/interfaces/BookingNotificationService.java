package com.sni.bokaticowork.features.booking.service.interfaces;

public interface BookingNotificationService {
    int queueReminder(String bookingNumber, int minutesBefore);
    int dispatchDue(int limit);

    /** Lot B6 : prevoit les rappels des reservations confirmees a venir ({@code bookings.reminders.offsets-minutes}). */
    default int scheduleUpcoming(int limit) {
        return 0;
    }
}
