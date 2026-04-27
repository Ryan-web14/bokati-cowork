package com.sni.bokaticowork.features.booking.service.interfaces;

public interface BookingNotificationService {
    int queueReminder(String bookingNumber, int minutesBefore);
    int dispatchDue(int limit);
}
