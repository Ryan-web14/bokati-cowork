package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.dto.request.CreateWalletHoldRequest;
import com.sni.bokaticowork.features.payment.dto.response.WalletHoldResponse;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.enums.WalletHoldStatus;
import com.sni.bokaticowork.features.payment.mapper.interfaces.PaymentMapper;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.model.WalletHold;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.features.payment.repository.WalletHoldRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletHoldService;
import com.sni.bokaticowork.features.payment.service.support.WalletLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public WalletHoldResponse create(CreateWalletHoldRequest request) {
        WalletAccount wallet = walletRepository.findByWalletNumber(request.walletNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        if (wallet.getAvailableBalance().compareTo(request.amount()) < 0) {
            throw new BadRequestException("Insufficient wallet balance");
        }
        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(request.amount()));
        wallet.setHeldBalance(wallet.getHeldBalance().add(request.amount()));
        walletRepository.save(wallet);
        ledgerService.entryOnly(wallet, request.amount(), WalletEntryType.HOLD, request.sourceType(), request.sourceCode(), null, request.createdBy());
        return mapper.toWalletHoldResponse(holdRepository.save(WalletHold.builder()
                .holdNumber(sequenceGenerator.next("wallet_hold"))
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
        WalletAccount wallet = hold.getWallet();
        wallet.setHeldBalance(wallet.getHeldBalance().subtract(hold.getAmount()));
        wallet.setLedgerBalance(wallet.getLedgerBalance().subtract(hold.getAmount()));
        walletRepository.save(wallet);
        ledgerService.entryOnly(wallet, hold.getAmount(), WalletEntryType.PAYMENT, hold.getSourceType(), hold.getSourceCode(), "CAPTURE_HOLD", createdBy);
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
        WalletAccount wallet = hold.getWallet();
        wallet.setHeldBalance(wallet.getHeldBalance().subtract(hold.getAmount()));
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(hold.getAmount()));
        walletRepository.save(wallet);
        ledgerService.entryOnly(wallet, hold.getAmount(), WalletEntryType.HOLD_RELEASE, hold.getSourceType(), hold.getSourceCode(), status.name(), createdBy);
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

}
