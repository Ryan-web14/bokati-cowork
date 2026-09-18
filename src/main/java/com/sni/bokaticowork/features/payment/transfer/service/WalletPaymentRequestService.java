package com.sni.bokaticowork.features.payment.transfer.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.transfer.model.WalletPaymentRequest;
import com.sni.bokaticowork.features.payment.transfer.model.WalletPaymentRequestStatus;
import com.sni.bokaticowork.features.payment.transfer.repository.WalletPaymentRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Demander de l'argent a un autre abonne.
 *
 * <p>La demande ne deplace rien et n'exige aucun code : elle est une invitation. Le paiement, lui,
 * est un transfert ordinaire du payeur vers le demandeur, avec tout ce que le transfert exige.
 * C'est pourquoi le service qui regle une demande est {@link WalletTransferService}, pas
 * celui-ci : une demande ne doit jamais devenir un chemin de sortie d'argent qui contournerait le
 * transfert.</p>
 */
@Service
@RequiredArgsConstructor
public class WalletPaymentRequestService {

    private final WalletPaymentRequestRepository requestRepository;
    private final WalletCounterpartyResolver counterpartyResolver;
    private final WalletNotifier notifier;
    private final SequenceGeneratorFacade sequenceGenerator;

    @Value("${bokati.wallet.payment-request.validity-days:7}")
    private int validityDays;

    @Transactional
    public WalletPaymentRequest create(WalletAccount requester, String payerReference, BigDecimal amount, String reason) {
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Le montant doit être positif");
        }
        WalletCounterpartyResolver.Counterparty counterparty =
                counterpartyResolver.resolve(payerReference, requester.getCurrency());
        WalletAccount payer = counterparty.wallet();
        if (payer.getId().equals(requester.getId())) {
            throw new BadRequestException("Vous ne pouvez pas vous adresser une demande de paiement");
        }

        WalletPaymentRequest request = requestRepository.save(WalletPaymentRequest.builder()
                .requestNumber(sequenceGenerator.next("wallet_payment_request"))
                .requesterWallet(requester)
                .payerWallet(payer)
                .amount(amount.setScale(4, RoundingMode.HALF_UP))
                .currency(requester.getCurrency())
                .reason(StringUtils.hasText(reason) ? reason.trim() : null)
                .status(WalletPaymentRequestStatus.PENDING)
                .expiresAt(Instant.now().plus(Duration.ofDays(validityDays)))
                .build());
        notifier.paymentRequestReceived(payer, requester, request.getAmount(), request.getRequestNumber(), request.getReason());
        return request;
    }

    @Transactional(readOnly = true)
    public Page<WalletPaymentRequest> list(WalletAccount wallet, Pageable pageable) {
        return requestRepository.findInvolving(wallet.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public WalletPaymentRequest get(WalletAccount wallet, String requestNumber) {
        return involving(wallet, requestNumber);
    }

    /** Le payeur refuse · le demandeur le saura par le statut, sans qu'on lui doive une raison. */
    @Transactional
    public WalletPaymentRequest decline(WalletAccount payer, String requestNumber) {
        WalletPaymentRequest request = involving(payer, requestNumber);
        if (!request.getPayerWallet().getId().equals(payer.getId())) {
            throw new BadRequestException("Seul le destinataire de la demande peut la décliner");
        }
        requireOpen(request);
        return close(request, WalletPaymentRequestStatus.DECLINED);
    }

    /** Le demandeur retire · une demande reglee ne se retire plus, l'argent est deja passe. */
    @Transactional
    public WalletPaymentRequest cancel(WalletAccount requester, String requestNumber) {
        WalletPaymentRequest request = involving(requester, requestNumber);
        if (!request.getRequesterWallet().getId().equals(requester.getId())) {
            throw new BadRequestException("Seul l'auteur de la demande peut la retirer");
        }
        requireOpen(request);
        return close(request, WalletPaymentRequestStatus.CANCELLED);
    }

    @Transactional
    public int expireStale() {
        List<WalletPaymentRequest> expired = requestRepository.findExpired(WalletPaymentRequestStatus.PENDING, Instant.now());
        expired.forEach(request -> close(request, WalletPaymentRequestStatus.EXPIRED));
        return expired.size();
    }

    // -----------------------------------------------------------------------------------------

    private WalletPaymentRequest close(WalletPaymentRequest request, WalletPaymentRequestStatus status) {
        request.setStatus(status);
        request.setResolvedAt(Instant.now());
        return requestRepository.save(request);
    }

    private void requireOpen(WalletPaymentRequest request) {
        if (!request.openAt(Instant.now())) {
            throw new BadRequestException("Cette demande de paiement n'est plus ouverte");
        }
    }

    private WalletPaymentRequest involving(WalletAccount wallet, String requestNumber) {
        WalletPaymentRequest request = requestRepository.findByRequestNumber(requestNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de paiement introuvable"));
        boolean involved = request.getRequesterWallet().getId().equals(wallet.getId())
                || request.getPayerWallet().getId().equals(wallet.getId());
        if (!involved) {
            throw new ResourceNotFoundException("Demande de paiement introuvable");
        }
        return request;
    }
}
