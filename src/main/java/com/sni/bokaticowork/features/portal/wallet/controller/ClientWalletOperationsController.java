package com.sni.bokaticowork.features.portal.wallet.controller;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.transfer.model.WalletPaymentRequest;
import com.sni.bokaticowork.features.payment.transfer.model.WalletTransfer;
import com.sni.bokaticowork.features.payment.transfer.service.WalletPartyNames;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.features.payment.transfer.dto.WalletUsageDtos.BeneficiaryRequest;
import com.sni.bokaticowork.features.payment.transfer.dto.WalletUsageDtos.BeneficiaryView;
import com.sni.bokaticowork.features.payment.transfer.dto.WalletUsageDtos.DeviceUpdateRequest;
import com.sni.bokaticowork.features.payment.transfer.dto.WalletUsageDtos.DeviceView;
import com.sni.bokaticowork.features.payment.transfer.dto.WalletUsageDtos.InitiatedTransferView;
import com.sni.bokaticowork.features.payment.transfer.dto.WalletUsageDtos.OwnerStateView;
import com.sni.bokaticowork.features.payment.transfer.dto.WalletUsageDtos.PaymentRequestRequest;
import com.sni.bokaticowork.features.payment.transfer.dto.WalletUsageDtos.PaymentRequestView;
import com.sni.bokaticowork.features.payment.transfer.dto.WalletUsageDtos.PinRequest;
import com.sni.bokaticowork.features.payment.transfer.dto.WalletUsageDtos.PreferencesRequest;
import com.sni.bokaticowork.features.payment.transfer.dto.WalletUsageDtos.TopUpRequest;
import com.sni.bokaticowork.features.payment.transfer.dto.WalletUsageDtos.TransferRequest;
import com.sni.bokaticowork.features.payment.transfer.dto.WalletUsageDtos.TransferView;
import com.sni.bokaticowork.features.payment.transfer.repository.WalletTransferRepository;
import com.sni.bokaticowork.features.payment.transfer.service.WalletBeneficiaryService;
import com.sni.bokaticowork.features.payment.transfer.service.WalletDeviceService;
import com.sni.bokaticowork.features.payment.transfer.service.WalletOwnerControlService;
import com.sni.bokaticowork.features.payment.transfer.service.WalletPaymentRequestService;
import com.sni.bokaticowork.features.payment.transfer.service.WalletStatementService;
import com.sni.bokaticowork.features.payment.transfer.service.WalletTopUpService;
import com.sni.bokaticowork.features.payment.transfer.service.WalletTransferService;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Les usages du portefeuille, par son titulaire.
 *
 * <p>Chaque point d'entree commence par retrouver le portefeuille <em>du titulaire connecte</em>.
 * Un numero qui n'est pas le sien est « introuvable », jamais « interdit » : repondre interdit
 * confirmerait l'existence du portefeuille a qui essaie des numeros.</p>
 *
 * <p>L'appareil est identifie par l'en-tete {@code X-Device-Id}, que l'application mobile pose et
 * que le navigateur peut poser. Sans lui, l'operation passe quand meme · elle est simplement notee
 * comme venant d'un appareil anonyme, ce que la surveillance sait lire.</p>
 */
@RestController
@RequestMapping(ApiPath.V1 + "/client/wallet/{walletNumber}")
@RequiredArgsConstructor
public class ClientWalletOperationsController {

    private static final String OWNER_TYPE = "MEMBER";
    private static final String DEVICE_HEADER = "X-Device-Id";

    private final ClientContextService clientContextService;
    private final com.sni.bokaticowork.features.payment.transfer.service.WalletPartyNames partyNames;
    private final WalletAccountRepository walletRepository;
    private final WalletTransferRepository transferRepository;
    private final WalletTransferService transferService;
    private final WalletBeneficiaryService beneficiaryService;
    private final WalletPaymentRequestService paymentRequestService;
    private final WalletTopUpService topUpService;
    private final WalletOwnerControlService ownerControlService;
    private final WalletDeviceService deviceService;
    private final WalletStatementService statementService;

    // -------------------------------------------------------------------------------------
    // Transferts
    // -------------------------------------------------------------------------------------

    @PostMapping("/transfers/simulate")
    public ResponseEntity<WalletTransferService.TransferPreview> simulate(
            @PathVariable String walletNumber,
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = DEVICE_HEADER, required = false) String deviceId) {
        WalletAccount wallet = owned(walletNumber);
        return ResponseEntity.ok(transferService.simulate(wallet, request.toOrder(), deviceId));
    }

    @PostMapping("/transfers")
    public ResponseEntity<InitiatedTransferView> initiate(
            @PathVariable String walletNumber,
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = DEVICE_HEADER, required = false) String deviceId,
            HttpServletRequest http) {
        Member member = clientContextService.getAuthenticatedMember();
        WalletAccount wallet = owned(member, walletNumber);
        WalletTransferService.InitiatedTransfer initiated = transferService.initiate(
                wallet, request.toOrder(), member.getMemberId(), clientIp(http), deviceId);
        return ResponseEntity.ok(new InitiatedTransferView(
                TransferView.of(initiated.transfer(), wallet.getId(), counterpartyName(initiated.transfer(), wallet)),
                initiated.confirmation().getConfirmationCode(),
                initiated.confirmation().getChallengeType(),
                initiated.confirmation().getExpiresAt(),
                initiated.confirmation().getMaxAttempts()));
    }

    @PostMapping("/transfers/{transferNumber}/confirm")
    public ResponseEntity<TransferView> confirm(@PathVariable String walletNumber,
                                                @PathVariable String transferNumber,
                                                @Valid @RequestBody PinRequest request) {
        WalletAccount wallet = owned(walletNumber);
        WalletTransfer confirmed = transferService.confirm(wallet, transferNumber, request.pin());
        return ResponseEntity.ok(TransferView.of(confirmed, wallet.getId(), counterpartyName(confirmed, wallet)));
    }

    @PostMapping("/transfers/{transferNumber}/cancel")
    public ResponseEntity<TransferView> cancel(@PathVariable String walletNumber, @PathVariable String transferNumber) {
        WalletAccount wallet = owned(walletNumber);
        WalletTransfer cancelled = transferService.cancel(wallet, transferNumber);
        return ResponseEntity.ok(TransferView.of(cancelled, wallet.getId(), counterpartyName(cancelled, wallet)));
    }

    @GetMapping("/transfers")
    public ResponseEntity<PaginatedResponse<TransferView>> transfers(@PathVariable String walletNumber,
                                                                     @PageableDefault(size = 20) Pageable pageable) {
        WalletPartyNames.Lookup names = partyNames.lookup();
        WalletAccount wallet = owned(walletNumber);
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(new PaginatedResponse<>(transferRepository.findInvolving(wallet.getId(), unsorted)
                .map(transfer -> TransferView.of(transfer, wallet.getId(), names.of(counterpartyOf(transfer, wallet))))));
    }

    @GetMapping("/transfers/{transferNumber}")
    public ResponseEntity<TransferView> transfer(@PathVariable String walletNumber, @PathVariable String transferNumber) {
        WalletAccount wallet = owned(walletNumber);
        WalletTransfer transfer = transferService.get(wallet, transferNumber);
        return ResponseEntity.ok(TransferView.of(transfer, wallet.getId(), counterpartyName(transfer, wallet)));
    }

    // -------------------------------------------------------------------------------------
    // Beneficiaires
    // -------------------------------------------------------------------------------------

    @GetMapping("/beneficiaries")
    public ResponseEntity<List<BeneficiaryView>> beneficiaries(@PathVariable String walletNumber) {
        WalletPartyNames.Lookup names = partyNames.lookup();
        WalletAccount wallet = owned(walletNumber);
        return ResponseEntity.ok(beneficiaryService.list(wallet).stream().map(b -> BeneficiaryView.of(b, names.of(b.getBeneficiaryWallet()))).toList());
    }

    @PostMapping("/beneficiaries")
    public ResponseEntity<BeneficiaryView> addBeneficiary(@PathVariable String walletNumber,
                                                          @Valid @RequestBody BeneficiaryRequest request) {
        WalletAccount wallet = owned(walletNumber);
        var beneficiary = beneficiaryService.add(wallet, request.counterparty(), request.alias());
        return ResponseEntity.ok(BeneficiaryView.of(beneficiary, partyNames.of(beneficiary.getBeneficiaryWallet())));
    }

    @DeleteMapping("/beneficiaries/{id}")
    public ResponseEntity<Void> removeBeneficiary(@PathVariable String walletNumber, @PathVariable Long id) {
        beneficiaryService.remove(owned(walletNumber), id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------------------
    // Demandes de paiement
    // -------------------------------------------------------------------------------------

    @PostMapping("/payment-requests")
    public ResponseEntity<PaymentRequestView> requestPayment(@PathVariable String walletNumber,
                                                             @Valid @RequestBody PaymentRequestRequest request) {
        WalletAccount wallet = owned(walletNumber);
        return ResponseEntity.ok(PaymentRequestView.of(
                paymentRequestService.create(wallet, request.payer(), request.amount(), request.reason()), wallet.getId()));
    }

    @GetMapping("/payment-requests")
    public ResponseEntity<PaginatedResponse<PaymentRequestView>> paymentRequests(
            @PathVariable String walletNumber, @PageableDefault(size = 20) Pageable pageable) {
        WalletAccount wallet = owned(walletNumber);
        WalletPartyNames.Lookup names = partyNames.lookup();
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(new PaginatedResponse<>(paymentRequestService.list(wallet, unsorted)
                .map(request -> PaymentRequestView.of(request, wallet.getId(), names.of(counterpartyOf(request, wallet))))));
    }

    @PostMapping("/payment-requests/{requestNumber}/decline")
    public ResponseEntity<PaymentRequestView> decline(@PathVariable String walletNumber, @PathVariable String requestNumber) {
        WalletAccount wallet = owned(walletNumber);
        WalletPaymentRequest declined = paymentRequestService.decline(wallet, requestNumber);
        return ResponseEntity.ok(PaymentRequestView.of(declined, wallet.getId(), counterpartyName(declined, wallet)));
    }

    @PostMapping("/payment-requests/{requestNumber}/cancel")
    public ResponseEntity<PaymentRequestView> cancelRequest(@PathVariable String walletNumber, @PathVariable String requestNumber) {
        WalletAccount wallet = owned(walletNumber);
        WalletPaymentRequest cancelledRequest = paymentRequestService.cancel(wallet, requestNumber);
        return ResponseEntity.ok(PaymentRequestView.of(cancelledRequest, wallet.getId(), counterpartyName(cancelledRequest, wallet)));
    }

    // -------------------------------------------------------------------------------------
    // Rechargement
    // -------------------------------------------------------------------------------------

    @PostMapping("/top-ups")
    public ResponseEntity<MobileMoneyDepositResponse> topUp(@PathVariable String walletNumber,
                                                            @Valid @RequestBody TopUpRequest request) {
        Member member = clientContextService.getAuthenticatedMember();
        WalletAccount wallet = owned(member, walletNumber);
        return ResponseEntity.ok(topUpService.initiate(wallet,
                new WalletTopUpService.TopUpOrder(request.amount(), request.phoneNumber(), request.correspondent()),
                member.getMemberId()));
    }

    // -------------------------------------------------------------------------------------
    // Verrouillage et preferences
    // -------------------------------------------------------------------------------------

    @GetMapping("/state")
    public ResponseEntity<OwnerStateView> state(@PathVariable String walletNumber) {
        return ResponseEntity.ok(OwnerStateView.of(owned(walletNumber)));
    }

    /** Sans code · quelqu'un qui vient de perdre son telephone doit pouvoir fermer a la minute. */
    @PostMapping("/lock")
    public ResponseEntity<OwnerStateView> lock(@PathVariable String walletNumber,
                                               @RequestHeader(value = DEVICE_HEADER, required = false) String deviceId,
                                               HttpServletRequest http) {
        return ResponseEntity.ok(OwnerStateView.of(ownerControlService.lock(owned(walletNumber), clientIp(http), deviceId)));
    }

    /** Avec code s'il en existe un · c'est le sens qui interesse un compte vole. */
    @PostMapping("/unlock")
    public ResponseEntity<OwnerStateView> unlock(@PathVariable String walletNumber,
                                                 @RequestBody(required = false) PinRequest request,
                                                 @RequestHeader(value = DEVICE_HEADER, required = false) String deviceId,
                                                 HttpServletRequest http) {
        return ResponseEntity.ok(OwnerStateView.of(ownerControlService.unlock(owned(walletNumber),
                request == null ? null : request.pin(), clientIp(http), deviceId)));
    }

    @PutMapping("/preferences")
    public ResponseEntity<OwnerStateView> preferences(@PathVariable String walletNumber,
                                                      @RequestBody PreferencesRequest request) {
        return ResponseEntity.ok(OwnerStateView.of(ownerControlService.updatePreferences(owned(walletNumber),
                request.lowBalanceThreshold(), request.notifyOnCredit(), request.notifyOnDebit())));
    }

    // -------------------------------------------------------------------------------------
    // Appareils
    // -------------------------------------------------------------------------------------

    @GetMapping("/devices")
    public ResponseEntity<List<DeviceView>> devices(@PathVariable String walletNumber) {
        return ResponseEntity.ok(deviceService.list(owned(walletNumber)).stream().map(DeviceView::of).toList());
    }

    @PatchMapping("/devices/{id}")
    public ResponseEntity<DeviceView> updateDevice(@PathVariable String walletNumber, @PathVariable Long id,
                                                   @RequestBody DeviceUpdateRequest request) {
        WalletAccount wallet = owned(walletNumber);
        if (request.label() != null) {
            deviceService.rename(wallet, id, request.label());
        }
        if (request.trusted() != null) {
            deviceService.trust(wallet, id, request.trusted());
        }
        return ResponseEntity.ok(DeviceView.of(deviceService.list(wallet).stream()
                .filter(device -> device.getId().equals(id)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Appareil introuvable"))));
    }

    @PostMapping("/devices/{id}/revoke")
    public ResponseEntity<DeviceView> revokeDevice(@PathVariable String walletNumber, @PathVariable Long id) {
        return ResponseEntity.ok(DeviceView.of(deviceService.revoke(owned(walletNumber), id)));
    }

    // -------------------------------------------------------------------------------------
    // Documents
    // -------------------------------------------------------------------------------------

    @GetMapping(value = "/statement", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> statement(@PathVariable String walletNumber,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        WalletAccount wallet = owned(walletNumber);
        byte[] pdf = statementService.statement(wallet, from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"releve-" + walletNumber + "-" + from + "-" + to + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping(value = "/ledger/{transactionNumber}/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> receipt(@PathVariable String walletNumber, @PathVariable String transactionNumber) {
        WalletAccount wallet = owned(walletNumber);
        byte[] pdf = statementService.receipt(wallet, transactionNumber);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"recu-" + transactionNumber + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // -------------------------------------------------------------------------------------

    /** Le portefeuille d'en face, vu du titulaire. */
    private WalletAccount counterpartyOf(WalletTransfer transfer, WalletAccount viewer) {
        return transfer.getSourceWallet().getId().equals(viewer.getId())
                ? transfer.getTargetWallet() : transfer.getSourceWallet();
    }

    private WalletAccount counterpartyOf(WalletPaymentRequest request, WalletAccount viewer) {
        return request.getRequesterWallet().getId().equals(viewer.getId())
                ? request.getPayerWallet() : request.getRequesterWallet();
    }

    private String counterpartyName(WalletTransfer transfer, WalletAccount viewer) {
        return partyNames.of(counterpartyOf(transfer, viewer));
    }

    private String counterpartyName(WalletPaymentRequest request, WalletAccount viewer) {
        return partyNames.of(counterpartyOf(request, viewer));
    }

    private WalletAccount owned(String walletNumber) {
        return owned(clientContextService.getAuthenticatedMember(), walletNumber);
    }

    private WalletAccount owned(Member member, String walletNumber) {
        WalletAccount wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Portefeuille introuvable"));
        if (!OWNER_TYPE.equals(wallet.getOwnerType()) || !member.getMemberId().equals(wallet.getOwnerCode())) {
            throw new ResourceNotFoundException("Portefeuille introuvable");
        }
        return wallet;
    }

    private String clientIp(HttpServletRequest http) {
        String forwarded = http.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return http.getRemoteAddr();
    }
}
