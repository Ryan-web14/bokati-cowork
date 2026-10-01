package com.sni.bokaticowork.features.payment.cash.worker;

import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import com.sni.bokaticowork.features.payment.cash.model.CashPaymentDeclaration;
import com.sni.bokaticowork.features.payment.cash.repository.CashPaymentDeclarationRepository;
import com.sni.bokaticowork.features.payment.cash.service.CashDeclarationNotifier;
import com.sni.bokaticowork.features.payment.cash.service.CashPaymentDeclarationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

/**
 * Une annonce que personne n honore finit par tomber · et rend ce qu elle tenait.
 *
 * <p>Un creneau reserve pour quelqu un qui a annonce qu il passerait payer est un creneau que
 * personne d autre ne peut prendre. Sans echeance, une annonce oubliee immobilisait une salle
 * indefiniment, et la file de la caisse se remplissait de lignes mortes.</p>
 *
 * <p>Ce qui tombe, et ce qui ne tombe pas : la <b>reservation</b> est annulee, son creneau rendu.
 * La <b>facture</b>, elle, reste due · le client n a pas paye, et une annonce non honoree n a
 * jamais eteint une creance. Elle n est simplement plus annoncee.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CashDeclarationExpiryWorker {

    private static final int BATCH = 100;

    private final CashPaymentDeclarationRepository repository;
    private final CashPaymentDeclarationService declarationService;
    private final CashDeclarationNotifier notifier;
    private final BookingService bookingService;

    /** Ce qu une passe a donne · utile aux tests et au journal. */
    public record Sweep(int expired, int bookingsReleased) {
    }

    @Scheduled(fixedDelayString = "${bokati.payment.cash-declaration.expiry-delay-ms:900000}")
    public void expireStaleDeclarations() {
        try {
            Sweep sweep = run();
            if (sweep.expired() > 0) {
                log.info("Annonces d especes · {} expiree(s), {} reservation(s) rendue(s)",
                        sweep.expired(), sweep.bookingsReleased());
            }
        } catch (Exception ex) {
            log.error("CashDeclarationExpiryWorker failed: {}", ex.getMessage(), ex);
        }
    }

    public Sweep run() {
        List<CashPaymentDeclaration> due = repository.findExpired(Instant.now(), PageRequest.of(0, BATCH));
        int expired = 0;
        int released = 0;
        for (CashPaymentDeclaration declaration : due) {
            try {
                declarationService.expire(declaration, "Délai dépassé sans encaissement");
                expired++;
                if (releaseBooking(declaration)) {
                    released++;
                }
                notifier.expired(declaration);
            } catch (Exception ex) {
                // Une annonce qui resiste ne doit pas bloquer les suivantes · la prochaine passe
                // la reprendra, elle est toujours echue.
                log.error("Expiration de l'annonce {} en erreur", declaration.getDeclarationNumber(), ex);
            }
        }
        return new Sweep(expired, released);
    }

    /**
     * Rend le creneau si la reservation l attendait encore.
     *
     * <p>Une reservation deja confirmee, deja annulee ou deja passee n est pas touchee : le client
     * a pu regler autrement entre-temps, et annuler une reservation payee serait bien pire que
     * laisser une annonce en trop.</p>
     */
    private boolean releaseBooking(CashPaymentDeclaration declaration) {
        if (!StringUtils.hasText(declaration.getBookingNumber())) {
            return false;
        }
        try {
            BookingStatus status = bookingService.get(declaration.getBookingNumber()).status();
            if (status != BookingStatus.PENDING_PAYMENT && status != BookingStatus.DRAFT) {
                log.info("Reservation {} en {} · laissee telle quelle malgre l'annonce expiree",
                        declaration.getBookingNumber(), status);
                return false;
            }
            bookingService.systemCancel(declaration.getBookingNumber(),
                    "Paiement en espèces annoncé puis non reçu · créneau rendu");
            log.info("Reservation {} rendue apres expiration de l'annonce {}",
                    declaration.getBookingNumber(), declaration.getDeclarationNumber());
            return true;
        } catch (Exception ex) {
            log.warn("Reservation {} non rendue apres l'annonce {} · {}",
                    declaration.getBookingNumber(), declaration.getDeclarationNumber(), ex.getMessage());
            return false;
        }
    }
}
