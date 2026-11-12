package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.accounting.model.CashExchangeRate;
import com.sni.bokaticowork.features.accounting.repository.CashExchangeRateRepository;
import com.sni.bokaticowork.features.payment.model.CashMovement;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.repository.CashMovementRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import com.sni.bokaticowork.features.payment.service.support.CashFlowRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Devises des caisses (lot C5) : taux du jour saisi par un responsable, solde d'une caisse par
 * devise, conversion en XAF pour la comptabilite (taux du mouvement, a defaut taux du jour).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ExchangeRateService {

    public static final String BASE = "XAF";

    private final CashExchangeRateRepository rateRepository;
    private final CashRegisterRepository registerRepository;
    private final CashSessionRepository sessionRepository;
    private final CashMovementRepository movementRepository;
    private final CashActorResolver actors;

    public CashExchangeRate setRate(String currency, BigDecimal rate, LocalDate validOn) {
        if (!actors.hasAuthority("CASH_EXCHANGE_RATE:MANAGE")) {
            throw new ForbiddenException("Saisir un taux demande CASH_EXCHANGE_RATE:MANAGE");
        }
        String code = normalize(currency);
        if (BASE.equals(code) || rate == null || rate.signum() <= 0) {
            throw new BadRequestException("Un taux est positif, pour une devise autre que le XAF");
        }
        LocalDate day = validOn == null ? LocalDate.now() : validOn;
        CashExchangeRate existing = rateRepository.findByCurrencyAndValidOn(code, day)
                .orElseGet(() -> CashExchangeRate.builder().currency(code).validOn(day).build());
        existing.setRate(rate.setScale(6, RoundingMode.HALF_UP));
        existing.setSetBy(actors.resolve("SYSTEM"));
        return rateRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public List<CashExchangeRate> recent() {
        return rateRepository.findTop30ByOrderByValidOnDescCurrencyAsc();
    }

    /** Taux d'une devise en XAF a une date (le dernier connu a cette date). */
    @Transactional(readOnly = true)
    public Optional<BigDecimal> rate(String currency, LocalDate date) {
        String code = normalize(currency);
        if (BASE.equals(code)) {
            return Optional.of(BigDecimal.ONE);
        }
        return rateRepository.findFirstByCurrencyAndValidOnLessThanEqualOrderByValidOnDesc(code, date).map(CashExchangeRate::getRate);
    }

    /** Montant en XAF d'un mouvement : taux du mouvement, sinon taux du jour du mouvement. */
    @Transactional(readOnly = true)
    public BigDecimal toBase(CashMovement movement, LocalDate date) {
        String currency = normalize(movement.getCurrency());
        if (BASE.equals(currency)) {
            return movement.getAmount();
        }
        BigDecimal rate = movement.getExchangeRate() != null && movement.getExchangeRate().signum() > 0 ? movement.getExchangeRate()
                : rate(currency, date).orElseThrow(() -> new BadRequestException("Aucun taux " + currency + " connu au " + date));
        return movement.getAmount().multiply(rate).setScale(0, RoundingMode.HALF_UP).setScale(4, RoundingMode.HALF_UP);
    }

    public record CurrencyBalance(String currency, BigDecimal balance, BigDecimal rate, BigDecimal baseEquivalent) {
    }

    /** Solde de la session ouverte d'une caisse, par devise, avec sa contre-valeur en XAF au taux du jour. */
    @Transactional(readOnly = true)
    public List<CurrencyBalance> balances(String registerCode) {
        CashRegister register = registerRepository.findByRegisterCode(registerCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Caisse introuvable : " + registerCode));
        Map<String, BigDecimal> balances = new TreeMap<>();
        sessionRepository.findBlockingSessionByCashRegisterId(register.getId()).ifPresent(session ->
                movementRepository.findByCashSession_IdOrderByCreatedAtAsc(session.getId()).forEach(movement ->
                        balances.merge(normalize(movement.getCurrency()), CashFlowRules.signedAmount(movement), BigDecimal::add)));
        LocalDate today = LocalDate.now();
        List<CurrencyBalance> result = new ArrayList<>();
        balances.forEach((currency, balance) -> {
            BigDecimal rate = rate(currency, today).orElse(null);
            result.add(new CurrencyBalance(currency, balance.setScale(4, RoundingMode.HALF_UP), rate,
                    rate == null ? null : balance.multiply(rate).setScale(0, RoundingMode.HALF_UP)));
        });
        return result;
    }

    static String normalize(String currency) {
        return StringUtils.hasText(currency) ? currency.trim().toUpperCase(Locale.ROOT) : BASE;
    }
}
