package com.sni.bokaticowork.features.booking.worker;

import com.sni.bokaticowork.core.settings.platform.SettingTriggers;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingHoldService;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

/**
 * Lot B1 (ecarts 5 et 9) : blocages expires (capacite rendue) et reservations dont l'echeance de
 * paiement est passee (statut EXPIRED, creneau rendu, facture annulee), toutes les minutes par
 * defaut ({@code bokati.booking.expiry.worker-delay-ms}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpiryWorker implements SchedulingConfigurer {

    private static final String DELAY_KEY = "bokati.booking.expiry.worker-delay-ms";
    private static final long DEFAULT_DELAY_MS = 60_000L;

    private final BookingHoldService holdService;
    private final BookingService bookingService;
    private SettingTriggers triggers;
    private ObjectProvider<BookingExpiryWorker> self;

    public void process() {
        try {
            int holds = holdService.expireDue(200);
            int bookings = bookingService.expireUnpaid(100);
            if (holds > 0 || bookings > 0) {
                log.info("Expiration : {} blocage(s), {} reservation(s) non reglee(s) a l'echeance", holds, bookings);
            }
            // Lot B3 : rappel avant l'echeance de paiement.
            int reminders = bookingService.remindPaymentsDue(100);
            if (reminders > 0) {
                log.info("Rappel de reglement : {} reservation(s)", reminders);
            }
        } catch (Exception ex) {
            log.error("BookingExpiryWorker : {}", ex.getMessage(), ex);
        }
    }

    @Autowired
    void setSettingTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<BookingExpiryWorker> self) {
        this.self = self;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().process(), triggers.fixedDelay(DELAY_KEY, DEFAULT_DELAY_MS));
    }
}
