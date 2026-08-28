package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.payment.dto.request.CreateWalletHoldRequest;
import com.sni.bokaticowork.features.payment.dto.response.WalletHoldResponse;
import com.sni.bokaticowork.features.payment.enums.WalletHoldStatus;
import com.sni.bokaticowork.features.payment.mapper.interfaces.PaymentMapper;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.model.WalletHold;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.features.payment.repository.WalletHoldRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletHoldService;
import com.sni.bokaticowork.features.payment.service.support.WalletLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
@Transactional
@RequiredArgsConstructor
public class WalletHoldServiceImpl implements WalletHoldService {

    private final WalletHoldRepository holdRepository;
    private final WalletAccountRepository walletRepository;
    private final WalletLedgerService ledgerService;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final PaymentMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<WalletHoldResponse> list(String walletNumber, String status, String sourceType, String sourceCode, Pageable pageable) {
        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return new PaginatedResponse<>(holdRepository.list(
                normalize(walletNumber),
                parseStatus(status),
                normalize(sourceType),
                normalize(sourceCode),
                unsortedPageable
        ).map(mapper::toWalletHoldResponse));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WalletHoldResponse create(CreateWalletHoldRequest request) {
        WalletAccount wallet = walletRepository.findByWalletNumber(request.walletNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        // Le numero de blocage est genere avant l'ecriture : il sert de cle d'idempotence, donc
        // il doit exister au moment ou le grand livre est ecrit.
        String holdNumber = sequenceGenerator.next("wallet_hold");
        ledgerService.placeHold(wallet, request.amount(), request.sourceType(), request.sourceCode(),
                holdNumber, request.createdBy(), "HOLD:" + holdNumber);
        return mapper.toWalletHoldResponse(holdRepository.save(WalletHold.builder()
                .holdNumber(holdNumber)
                .wallet(wallet)
                .amount(request.amount())
                .currency(wallet.getCurrency())
                .status(WalletHoldStatus.ACTIVE)
                .sourceType(request.sourceType())
                .sourceCode(request.sourceCode())
                .expiresAt(request.expiresAt())
                .createdBy(request.createdBy())
                .build()));
    }

    @Override
    public WalletHoldResponse capture(String holdNumber, String createdBy) {
        WalletHold hold = activeHold(holdNumber);
        ledgerService.captureHold(hold.getWallet(), hold.getAmount(), hold.getSourceType(), hold.getSourceCode(),
                "CAPTURE_HOLD", createdBy, "HOLD_CAPTURE:" + hold.getHoldNumber());
        hold.setStatus(WalletHoldStatus.CAPTURED);
        return mapper.toWalletHoldResponse(holdRepository.save(hold));
    }

    @Override
    public WalletHoldResponse release(String holdNumber, String createdBy) {
        WalletHold hold = activeHold(holdNumber);
        releaseHold(hold, createdBy, WalletHoldStatus.RELEASED);
        return mapper.toWalletHoldResponse(hold);
    }

    @Override
    public int expireDueHolds() {
        int count = 0;
        for (WalletHold hold : holdRepository.findAllByStatusAndExpiresAtLessThanEqual(WalletHoldStatus.ACTIVE.name(), Instant.now())) {
            releaseHold(hold, "SYSTEM", WalletHoldStatus.EXPIRED);
            count++;
        }
        return count;
    }

    private void releaseHold(WalletHold hold, String createdBy, WalletHoldStatus status) {
        ledgerService.releaseHold(hold.getWallet(), hold.getAmount(), hold.getSourceType(), hold.getSourceCode(),
                status.name(), createdBy, "HOLD_RELEASE:" + hold.getHoldNumber());
        hold.setStatus(status);
        holdRepository.save(hold);
    }

    private WalletHold activeHold(String holdNumber) {
        WalletHold hold = holdRepository.findByHoldNumber(holdNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet hold not found"));
        if (hold.getStatus() != WalletHoldStatus.ACTIVE) {
            throw new BadRequestException("Wallet hold is not active");
        }
        return hold;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private WalletHoldStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return WalletHoldStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid wallet hold status");
        }
    }

}
