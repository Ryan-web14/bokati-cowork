package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.dto.request.CashCountRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashCountResponse;
import com.sni.bokaticowork.features.payment.enums.CashCountMoment;
import com.sni.bokaticowork.features.payment.enums.CashDenominationKind;
import com.sni.bokaticowork.features.payment.model.CashCount;
import com.sni.bokaticowork.features.payment.model.CashCountLine;
import com.sni.bokaticowork.features.payment.model.CashDenomination;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.repository.CashCountLineRepository;
import com.sni.bokaticowork.features.payment.repository.CashCountRepository;
import com.sni.bokaticowork.features.payment.repository.CashDenominationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Enregistre les comptages (plan des caisses, section 6).
 *
 * <p>Le billetage se saisit par quantite et le total se calcule ; les billets abimes ou suspects
 * sont exclus du total disponible. Un comptage n'est jamais modifie : un nouveau le remplace
 * ({@code SUPERSEDED}) et l'ancien reste visible.</p>
 */
@Component
@RequiredArgsConstructor
public class CashCountRecorder {

    public static final String DEFAULT_CURRENCY = "XAF";

    private final CashCountRepository countRepository;
    private final CashCountLineRepository lineRepository;
    private final CashDenominationRepository denominationRepository;
    private final SequenceGeneratorFacade sequenceGenerator;

    /** Total compte d'une demande, sans rien enregistrer. */
    public Totals totals(CashRegister register, CashCountRequest request) {
        if (request == null) {
            throw new BadRequestException("Comptage manquant");
        }
        String currency = currency(request);
        List<CashCountRequest.Line> lines = request.lines() == null ? List.of() : request.lines();
        if (lines.isEmpty()) {
            if (Boolean.TRUE.equals(register.getCountLinesRequired())) {
                throw new BadRequestException("La caisse " + register.getRegisterCode()
                        + " exige un billetage : saisissez les quantites par coupure");
            }
            if (request.totalAmount() == null || request.totalAmount().signum() < 0) {
                throw new BadRequestException("Indiquez le montant compte, ou le billetage par coupure");
            }
            return new Totals(currency, money(request.totalAmount()), BigDecimal.ZERO.setScale(4), false, List.of());
        }
        List<CashDenomination> active = denominationRepository.findByCurrencyOrderBySortOrderAsc(currency).stream()
                .filter(denomination -> isActive(denomination, LocalDate.now()))
                .toList();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal damaged = BigDecimal.ZERO;
        List<CashCountLine> built = new ArrayList<>();
        for (CashCountRequest.Line line : lines) {
            if (line == null || line.value() == null) {
                continue;
            }
            CashDenominationKind kind = line.kind() == null ? CashDenominationKind.BILL : line.kind();
            boolean known = active.stream().anyMatch(denomination -> denomination.getKind() == kind
                    && denomination.getValue().compareTo(line.value()) == 0);
            if (!known) {
                throw new BadRequestException("Coupure inconnue ou retiree : " + line.value().stripTrailingZeros().toPlainString()
                        + " " + currency + " (" + (kind == CashDenominationKind.BILL ? "billet" : "piece") + ")");
            }
            int quantity = line.quantity() == null ? 0 : line.quantity();
            int damagedQuantity = line.damagedQuantity() == null ? 0 : line.damagedQuantity();
            if (quantity < 0 || damagedQuantity < 0) {
                throw new BadRequestException("Une quantite ne peut pas etre negative");
            }
            BigDecimal amount = line.value().multiply(BigDecimal.valueOf(quantity));
            total = total.add(amount);
            damaged = damaged.add(line.value().multiply(BigDecimal.valueOf(damagedQuantity)));
            built.add(CashCountLine.builder().value(line.value()).kind(kind).quantity(quantity)
                    .damagedQuantity(damagedQuantity).amount(money(amount)).build());
        }
        return new Totals(currency, money(total), money(damaged), true, built);
    }

    /** Enregistre un comptage valide. */
    public CashCount record(CashRegister register, String sessionNumber, CashCountMoment moment, CashCountRequest request,
                            String countedBy, BigDecimal expected, String referenceNumber, String secondCounter, String witness) {
        Totals totals = totals(register, request);
        BigDecimal expectedAmount = expected == null ? null : money(expected);
        CashCount count = countRepository.save(CashCount.builder()
                .countNumber(sequenceGenerator.next("cash_count"))
                .registerCode(register.getRegisterCode())
                .sessionNumber(sessionNumber)
                .moment(moment)
                .currency(totals.currency())
                .total(totals.total())
                .damagedTotal(totals.damaged())
                .withLines(totals.withLines())
                .expectedAmount(expectedAmount)
                .variance(expectedAmount == null ? null : totals.total().subtract(expectedAmount))
                .countedBy(countedBy)
                .secondCounter(secondCounter)
                .witness(witness)
                .referenceNumber(referenceNumber)
                .note(request == null ? null : trim(request.note()))
                .build());
        if (!totals.lines().isEmpty()) {
            totals.lines().forEach(line -> line.setCountId(count.getId()));
            lineRepository.saveAll(totals.lines());
        }
        return count;
    }

    /** Remplace un comptage par un plus recent ; l'ancien reste visible. */
    public void supersede(String countNumber, String byCountNumber) {
        if (!StringUtils.hasText(countNumber)) {
            return;
        }
        countRepository.findByCountNumber(countNumber).ifPresent(previous -> {
            previous.setStatus("SUPERSEDED");
            previous.setSupersededBy(byCountNumber);
            countRepository.save(previous);
        });
    }

    public CashCountResponse toResponse(CashCount count, boolean revealExpected) {
        if (count == null) {
            return null;
        }
        List<CashCountResponse.Line> lines = count.getId() == null ? List.of() : lineRepository.findByCountIdOrderByValueDesc(count.getId()).stream()
                .map(line -> new CashCountResponse.Line(line.getValue(), line.getKind(), line.getQuantity(), line.getDamagedQuantity(), line.getAmount()))
                .toList();
        return new CashCountResponse(count.getCountNumber(), count.getRegisterCode(), count.getSessionNumber(), count.getMoment(),
                count.getCurrency(), count.getTotal(), count.getDamagedTotal(), count.getWithLines(),
                revealExpected ? count.getExpectedAmount() : null, revealExpected ? count.getVariance() : null,
                count.getCountedBy(), count.getSecondCounter(), count.getWitness(), count.getStatus(), count.getSupersededBy(),
                count.getReferenceNumber(), count.getNote(), count.getCreatedAt(), lines);
    }

    public static boolean isActive(CashDenomination denomination, LocalDate on) {
        return (denomination.getActiveFrom() == null || !denomination.getActiveFrom().isAfter(on))
                && (denomination.getActiveTo() == null || !denomination.getActiveTo().isBefore(on));
    }

    private static String currency(CashCountRequest request) {
        return StringUtils.hasText(request.currency()) ? request.currency().trim().toUpperCase(Locale.ROOT) : DEFAULT_CURRENCY;
    }

    private static BigDecimal money(BigDecimal amount) {
        return amount.setScale(4, RoundingMode.HALF_UP);
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public record Totals(String currency, BigDecimal total, BigDecimal damaged, boolean withLines, List<CashCountLine> lines) {
    }
}
