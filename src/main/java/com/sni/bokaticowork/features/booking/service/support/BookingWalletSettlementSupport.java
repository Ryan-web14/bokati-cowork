package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentFromBillingDocumentRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreateWalletHoldRequest;
import com.sni.bokaticowork.features.payment.dto.request.WalletPaymentRequest;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.dto.response.WalletResponse;
import com.sni.bokaticowork.features.payment.enums.WalletHoldStatus;
import com.sni.bokaticowork.features.payment.model.WalletHold;
import com.sni.bokaticowork.features.payment.repository.WalletHoldRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletHoldService;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Reglement au portefeuille d'une reservation, en deux temps.
 *
 * <p><b>A la confirmation</b>, les fonds sont bloques, pas preleves. Le solde disponible est
 * verifie dans le meme geste, donc un portefeuille insuffisant fait echouer la confirmation
 * sur-le-champ, avec un message explicite, plutot que d'annoncer au client une reservation qui ne
 * sera jamais payee. Le blocage acquis, la reservation est payee : elle est confirmee
 * immediatement, sans passer par l'attente de reglement que suppose un paiement venu de
 * l'exterieur. Le montant est sorti du solde disponible, le client ne peut plus le depenser
 * ailleurs, et l'entreprise le tient.</p>
 *
 * <p><b>Une minute plus tard</b>, le blocage devient un debit, en arriere-plan et sans que
 * personne n'attende. Ce delai est la fenetre de retractation : une reservation annulee dans
 * l'intervalle rend les fonds sans qu'aucun mouvement comptable n'ait eu lieu, la ou un
 * prelevement immediat aurait impose un avoir et un remboursement. Passe ce delai, le blocage est
 * libere puis aussitot debite par le circuit de paiement habituel, dans la meme transaction :
 * l'argent ne redevient jamais reellement disponible, et la facture se solde par la chaine qui
 * traite deja tous les autres moyens de paiement.</p>
 *
 * <p>Le blocage porte une echeance de securite bien au-dela du delai de reglement. Elle ne sert
 * jamais en fonctionnement normal : elle existe pour qu'un blocage orphelin, laisse par une
 * transaction de confirmation annulee apres coup, finisse par etre rendu au client par
 * {@code WalletHoldExpiryWorker} meme si plus rien d'autre ne le reclame.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingWalletSettlementSupport {

    /** Valeur portee par {@code wallet_hold.source_type} pour un blocage de reservation. */
    public static final String HOLD_SOURCE_TYPE = "BOOKING";

    private static final String SYSTEM_ACTOR = "SYSTEM";
    private static final String BILLABLE_SOURCE_TYPE = "BILLABLE_ITEM";

    /** Etats ou plus rien n'est du : le blocage doit etre rendu, jamais encaisse. */
    private static final Set<BookingStatus> RELEASING_STATUSES = Set.of(
            BookingStatus.CANCELLED, BookingStatus.REJECTED
    );

    private final BookingRepository bookingRepository;
    private final BillingDocumentRepository billingDocumentRepository;
    private final WalletHoldRepository walletHoldRepository;
    private final WalletHoldService walletHoldService;
    private final WalletService walletService;
    private final PaymentService paymentService;

    @Value("${bokati.booking.wallet.settlement-delay-minutes:1}")
    private int settlementDelayMinutes;

    @Value("${bokati.booking.wallet.hold-grace-minutes:60}")
    private int holdGraceMinutes;

    // -----------------------------------------------------------------------------------------
    // Blocage
    // -----------------------------------------------------------------------------------------

    /**
     * Bloque le montant de la reservation sur le portefeuille de son titulaire.
     *
     * <p>Le portefeuille n'est pas demande a l'appelant : il est celui du titulaire deja resolu sur
     * la reservation, cree au besoin dans la devise de celle-ci.</p>
     *
     * @throws BadRequestException si le solde disponible ne couvre pas le montant
     */
    public void hold(Booking booking) {
        BigDecimal amount = booking.getTotalAmount();
        if (amount == null || amount.signum() <= 0) {
            return;
        }
        if (hasActiveHold(booking.getBookingNumber())) {
            return;
        }

        WalletResponse wallet = walletService.getOrCreate(
                booking.getOwnerType().name(), booking.getOwnerCode(), booking.getCurrency());
        try {
            walletHoldService.create(new CreateWalletHoldRequest(
                    wallet.walletNumber(),
                    amount,
                    HOLD_SOURCE_TYPE,
                    booking.getBookingNumber(),
                    Instant.now().plus(Duration.ofMinutes((long) settlementDelayMinutes + holdGraceMinutes)),
                    SYSTEM_ACTOR
            ));
        } catch (BadRequestException ex) {
            throw new BadRequestException("Solde du portefeuille insuffisant pour cette reservation · "
                    + amount.stripTrailingZeros().toPlainString() + " " + booking.getCurrency() + " requis");
        }
        log.info("Reservation {} · {} {} bloques sur le portefeuille {} en attente de reglement",
                booking.getBookingNumber(), amount.stripTrailingZeros().toPlainString(),
                booking.getCurrency(), wallet.walletNumber());
    }

    /**
     * Vrai tant que des fonds sont bloques et non encore debites pour cette reservation, donc tant
     * que l'annuler ne coute rien a personne.
     */
    @Transactional(readOnly = true)
    public boolean hasPendingHold(Booking booking) {
        return hasActiveHold(booking.getBookingNumber());
    }

    /**
     * Rend au client tout blocage encore actif pose pour cette reservation. Sans effet s'il n'y en
     * a aucun, ce qui est le cas des reservations reglees autrement ou deja debitees.
     */
    public void release(Booking booking, String reason) {
        for (WalletHold hold : activeHolds(booking.getBookingNumber())) {
            walletHoldService.release(hold.getHoldNumber(), SYSTEM_ACTOR);
            log.info("Reservation {} · blocage {} rendu au portefeuille · {}",
                    booking.getBookingNumber(), hold.getHoldNumber(), reason);
        }
    }

    // -----------------------------------------------------------------------------------------
    // Reglement
    // -----------------------------------------------------------------------------------------

    /**
     * Numeros des blocages de reservation arrives a echeance. Le pilotage du lot appartient a
     * l'appelant, qui traite chaque numero isolement pour qu'un echec n'emporte pas les autres.
     */
    @Transactional(readOnly = true)
    public List<String> dueHoldNumbers(int limit) {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(settlementDelayMinutes));
        return walletHoldRepository.findDueForSettlement(HOLD_SOURCE_TYPE, cutoff, limit).stream()
                .map(WalletHold::getHoldNumber)
                .toList();
    }

    /**
     * Transforme un blocage en debit, ou le rend si plus rien n'est du.
     *
     * <p>Le blocage est libere avant le debit parce que le prelevement lit le solde disponible.
     * Les deux ecritures partagent la meme transaction : entre elles, aucune autre operation ne
     * peut voir ni consommer les fonds redevenus disponibles.</p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void settle(String holdNumber) {
        settleInternal(holdNumber);
    }

    private void settleInternal(String holdNumber) {
        WalletHold hold = walletHoldRepository.findByHoldNumber(holdNumber).orElse(null);
        if (hold == null || hold.getStatus() != WalletHoldStatus.ACTIVE) {
            return;
        }

        Booking booking = bookingRepository.findByBookingNumber(hold.getSourceCode()).orElse(null);
        if (booking == null) {
            releaseOrphan(hold, "reservation introuvable");
            return;
        }
        if (RELEASING_STATUSES.contains(booking.getStatus())) {
            releaseOrphan(hold, "reservation " + booking.getStatus());
            return;
        }

        BillingDocument invoice = invoiceOf(booking);
        if (invoice == null) {
            releaseOrphan(hold, "aucune facture rattachee");
            return;
        }
        if (invoice.getBalanceDue() == null || invoice.getBalanceDue().signum() <= 0) {
            releaseOrphan(hold, "facture " + invoice.getDocumentNumber() + " deja soldee");
            return;
        }

        walletHoldService.release(holdNumber, SYSTEM_ACTOR);

        PaymentIntentResponse intent = paymentService.createIntentFromBillingDocument(
                new CreatePaymentIntentFromBillingDocumentRequest(
                        invoice.getDocumentNumber(), null, "BOOKING_WALLET:" + booking.getBookingNumber(), null, null));
        WalletResponse wallet = walletService.getOrCreate(
                booking.getOwnerType().name(), booking.getOwnerCode(), booking.getCurrency());
        paymentService.payWithWallet(intent.intentNumber(),
                new WalletPaymentRequest(wallet.walletNumber(), null, SYSTEM_ACTOR, null));

        log.info("Reservation {} · blocage {} debite sur la facture {}",
                booking.getBookingNumber(), holdNumber, invoice.getDocumentNumber());
    }

    // -----------------------------------------------------------------------------------------

    private void releaseOrphan(WalletHold hold, String reason) {
        walletHoldService.release(hold.getHoldNumber(), SYSTEM_ACTOR);
        log.info("Blocage {} rendu au portefeuille sans debit · {}", hold.getHoldNumber(), reason);
    }

    private BillingDocument invoiceOf(Booking booking) {
        if (!StringUtils.hasText(booking.getBillableNumber())) {
            return null;
        }
        return billingDocumentRepository.findFirstBySourceAndType(
                BILLABLE_SOURCE_TYPE, booking.getBillableNumber(), BillingDocumentType.INVOICE.name()).orElse(null);
    }

    private List<WalletHold> activeHolds(String bookingNumber) {
        return walletHoldRepository.findAllByStatusAndSourceTypeAndSourceCode(
                WalletHoldStatus.ACTIVE.name(), HOLD_SOURCE_TYPE, bookingNumber);
    }

    private boolean hasActiveHold(String bookingNumber) {
        return !activeHolds(bookingNumber).isEmpty();
    }
}
