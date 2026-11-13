package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentAdvanceRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentClauseRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentDiscountRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateEarlyPaymentDiscountRequest;
import com.sni.bokaticowork.features.billing.enums.BillingAdvanceType;
import com.sni.bokaticowork.features.billing.enums.BillingDiscountType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentAdvance;
import com.sni.bokaticowork.features.billing.model.BillingDocumentClause;
import com.sni.bokaticowork.features.billing.model.BillingDocumentDiscount;
import com.sni.bokaticowork.features.billing.model.BillingDocumentEarlyPaymentDiscount;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.model.BillingDocumentTax;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentAdvanceRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentClauseRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentDiscountRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentEarlyPaymentDiscountRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentLineRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentTaxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BillingDocumentWriter {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BillingDocumentRepository documentRepository;
    private final BillingDocumentLineRepository lineRepository;
    private final BillingDocumentDiscountRepository discountRepository;
    private final BillingDocumentTaxRepository taxRepository;
    private final BillingDocumentClauseRepository clauseRepository;
    private final BillingDocumentAdvanceRepository advanceRepository;
    private final BillingDocumentEarlyPaymentDiscountRepository earlyPaymentDiscountRepository;

    public BillingDocument save(BillingDocument document,
                                BillingCalculationService.CalculatedDocument calculation,
                                List<CreateBillingDocumentDiscountRequest> discounts,
                                List<CreateBillingDocumentClauseRequest> clauses,
                                CreateBillingDocumentAdvanceRequest advance) {
        BillingDocument saved = documentRepository.save(document);
        calculation.lines().forEach(line -> {
            line.setDocument(saved);
            lineRepository.save(line);
        });
        saveDiscounts(saved, discounts, calculation.discountAmounts());
        saveTaxes(saved, calculation);
        saveClauses(saved, clauses);
        if (advance != null) {
            saveAdvance(saved, advance, calculation.lines());
        }
        return saved;
    }

    /**
     * Reecrit le detail d'un document deja enregistre apres un recalcul de ses totaux · taxes,
     * remises et clauses, jamais les lignes.
     *
     * <p>La mise a jour d'un brouillon rappelait {@link #save}, qui reenregistre toutes les lignes
     * recalculees comme nouvelles et ajoute les taxes sans retirer les anciennes : lignes et taxes
     * se dupliquaient a chaque modification. Les lignes sont deja en base, a jour.</p>
     *
     * @param discounts nouvelles remises, a enregistrer (les anciennes sont deja supprimees) ·
     *                  {@code null} garde les remises en base et ne met a jour que leur montant
     * @param clauses   nouvelles clauses, a enregistrer · {@code null} garde celles en base
     */
    public void rewriteDetails(BillingDocument document,
                               BillingCalculationService.CalculatedDocument calculation,
                               List<CreateBillingDocumentDiscountRequest> discounts,
                               List<CreateBillingDocumentClauseRequest> clauses) {
        if (discounts != null) {
            saveDiscounts(document, discounts, calculation.discountAmounts());
        } else {
            List<BillingDocumentDiscount> existing = discountRepository.findAllByDocumentOrderByIdAsc(document);
            List<BigDecimal> amounts = calculation.discountAmounts();
            for (int i = 0; i < existing.size(); i++) {
                BillingDocumentDiscount discount = existing.get(i);
                discount.setAmount(amounts != null && i < amounts.size() ? amounts.get(i) : BigDecimal.ZERO);
                discountRepository.save(discount);
            }
        }
        taxRepository.deleteAllByDocument(document);
        saveTaxes(document, calculation);
        if (clauses != null) {
            saveClauses(document, clauses);
        }
    }

    public void saveEarlyPaymentDiscount(BillingDocument document, CreateEarlyPaymentDiscountRequest request) {
        if (request == null) {
            return;
        }
        BigDecimal computed = document.getTotalAmount()
                .multiply(request.discountRate())
                .divide(HUNDRED, 4, java.math.RoundingMode.HALF_UP);
        String label = request.label() != null && !request.label().isBlank()
                ? request.label().trim()
                : String.format("Escompte %.2f%% si règlement avant le %s",
                        request.discountRate(),
                        request.ifPaidBefore().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        earlyPaymentDiscountRepository.save(BillingDocumentEarlyPaymentDiscount.builder()
                .document(document)
                .discountRate(request.discountRate())
                .ifPaidBefore(request.ifPaidBefore())
                .computedAmount(computed)
                .label(label)
                .build());
    }

    private void saveAdvance(BillingDocument document,
                             CreateBillingDocumentAdvanceRequest request,
                             List<BillingDocumentLine> lines) {
        List<Integer> included = request.includedLineOrders();
        List<Integer> excluded = request.excludedLineOrders();

        List<BillingDocumentLine> baseLines;
        if (included != null && !included.isEmpty()) {
            Set<Integer> includeSet = Set.copyOf(included);
            baseLines = lines.stream().filter(l -> includeSet.contains(l.getLineOrder())).toList();
        } else if (excluded != null && !excluded.isEmpty()) {
            Set<Integer> excludeSet = Set.copyOf(excluded);
            baseLines = lines.stream().filter(l -> !excludeSet.contains(l.getLineOrder())).toList();
        } else {
            baseLines = lines;
        }

        BigDecimal base = baseLines.stream()
                .map(BillingDocumentLine::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal computed;
        if (request.advanceType() == BillingAdvanceType.PERCENTAGE) {
            computed = base.multiply(request.advanceValue()).divide(HUNDRED, 4, RoundingMode.HALF_UP);
        } else {
            computed = request.advanceValue().min(base);
        }
        computed = computed.setScale(4, RoundingMode.HALF_UP);

        if (computed.signum() <= 0) {
            throw new BadRequestException("Le montant calculé de l'acompte doit être positif");
        }

        advanceRepository.save(BillingDocumentAdvance.builder()
                .document(document)
                .advanceType(request.advanceType())
                .advanceValue(request.advanceValue())
                .computedAmount(computed)
                .includedLineOrders(serializeOrders(included))
                .excludedLineOrders(serializeOrders(excluded))
                .paymentReference(trim(request.paymentReference()))
                .referenceLabel(trim(request.referenceLabel()))
                .dueDate(request.dueDate())
                .notes(trim(request.notes()))
                .build());
    }

    private String serializeOrders(List<Integer> orders) {
        if (orders == null || orders.isEmpty()) {
            return null;
        }
        return orders.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Enregistre les remises avec le montant <b>calcule</b>, sans le recalculer.
     *
     * <p>Il l'etait auparavant ici meme, sur {@code calculation.totalAmount()} · le TTC apres
     * remise · alors que le calcul travaillait sur le net HT. Une remise de 10 % sur 100 000 HT
     * s'enregistrait a 10 701 pour 10 000 reellement deduits : le detail contredisait le total,
     * et personne ne pouvait dire lequel des deux faisait foi.
     */
    private void saveDiscounts(BillingDocument document, List<CreateBillingDocumentDiscountRequest> discounts,
                               List<BigDecimal> computedAmounts) {
        if (discounts == null || discounts.isEmpty()) {
            return;
        }
        int index = 0;
        for (CreateBillingDocumentDiscountRequest request : discounts) {
            BigDecimal amount = computedAmounts != null && index < computedAmounts.size()
                    ? computedAmounts.get(index) : BigDecimal.ZERO;
            index++;
            discountRepository.save(BillingDocumentDiscount.builder()
                    .document(document)
                    .discountCode(request.discountCode())
                    .description(request.description().trim())
                    .discountType(request.discountType())
                    .value(request.value())
                    .amount(amount)
                    .sourceType(request.sourceType())
                    .sourceCode(request.sourceCode())
                    .build());
        }
    }

    /**
     * Taux effectivement applique, tire des lignes.
     *
     * <p>Il etait ecrit {@code BigDecimal.ZERO} en dur · invisible tant qu'aucun document ne
     * l'affichait. La facture annoncait « TVA 0 % » sur une TVA de 16 486.
     *
     * <p>Quand toutes les lignes taxees portent le meme taux, c'est celui-la. Sinon le taux
     * effectif, deduit des montants : il reste exact, la ou un taux choisi au hasard parmi
     * plusieurs serait faux.
     */
    private BigDecimal effectiveRate(List<BillingDocumentLine> lines,
                                     java.util.function.Function<BillingDocumentLine, BigDecimal> rateOf,
                                     BigDecimal base, BigDecimal amount) {
        java.util.Set<BigDecimal> rates = lines.stream()
                .filter(l -> Boolean.TRUE.equals(l.getTaxable()))
                .map(rateOf)
                .filter(java.util.Objects::nonNull)
                .filter(r -> r.signum() > 0)
                .map(r -> r.stripTrailingZeros())
                .collect(java.util.stream.Collectors.toSet());
        if (rates.size() == 1) {
            return rates.iterator().next().setScale(4, RoundingMode.HALF_UP);
        }
        if (base == null || base.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(BigDecimal.valueOf(100)).divide(base, 4, RoundingMode.HALF_UP);
    }

    private void saveTaxes(BillingDocument document, BillingCalculationService.CalculatedDocument calculation) {
        List<BillingDocumentTax> byRate = vatByRate(document, calculation);
        if (!byRate.isEmpty()) {
            // Lot C1 : plusieurs taux sur la piece, une ligne de TVA par taux.
            byRate.forEach(taxRepository::save);
        } else if (calculation.vatAmount().signum() > 0) {
            taxRepository.save(BillingDocumentTax.builder()
                    .document(document)
                    .taxCode("VAT")
                    .taxName("TVA")
                    .rate(effectiveRate(calculation.lines(), BillingDocumentLine::getVatRate,
                            calculation.taxableAmount(), calculation.vatAmount()))
                    .taxableAmount(calculation.taxableAmount())
                    .taxAmount(calculation.vatAmount())
                    .build());
        }
        if (calculation.additionalCentAmount().signum() > 0) {
            taxRepository.save(BillingDocumentTax.builder()
                    .document(document)
                    .taxCode("ADDITIONAL_CENT")
                    .taxName("Centime additionnel")
                    .rate(effectiveRate(calculation.lines(), BillingDocumentLine::getAdditionalCentRate,
                            calculation.vatAmount(), calculation.additionalCentAmount()))
                    .taxableAmount(calculation.vatAmount())
                    .taxAmount(calculation.additionalCentAmount())
                    .build());
        }
    }

    /**
     * Lot C1 : ventilation de la TVA par taux quand les lignes taxables en portent plusieurs. Les
     * bases et montants de chaque taux sont repartis au prorata des lignes, puis l'arrondi est
     * reporte sur le taux le plus lourd pour que la somme egale exactement les totaux de la piece
     * (remise globale comprise). Un seul taux : liste vide, la ligne unique habituelle s'applique.
     */
    static List<BillingDocumentTax> vatByRate(BillingDocument document, BillingCalculationService.CalculatedDocument calculation) {
        java.util.Map<BigDecimal, BigDecimal[]> sums = new java.util.TreeMap<>();
        for (BillingDocumentLine line : calculation.lines()) {
            if (!Boolean.TRUE.equals(line.getTaxable()) || line.getVatRate() == null || line.getVatRate().signum() <= 0
                    || Boolean.TRUE.equals(line.getOptional())) {
                continue;
            }
            BigDecimal[] acc = sums.computeIfAbsent(line.getVatRate().stripTrailingZeros(), rate -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            acc[0] = acc[0].add(line.getTaxableAmount() == null ? BigDecimal.ZERO : line.getTaxableAmount());
            acc[1] = acc[1].add(line.getVatAmount() == null ? BigDecimal.ZERO : line.getVatAmount());
        }
        if (sums.size() < 2 || calculation.vatAmount().signum() <= 0) {
            return List.of();
        }
        BigDecimal lineBase = sums.values().stream().map(a -> a[0]).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal lineVat = sums.values().stream().map(a -> a[1]).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (lineBase.signum() == 0 || lineVat.signum() == 0) {
            return List.of();
        }
        List<BillingDocumentTax> rows = new java.util.ArrayList<>();
        BigDecimal baseLeft = calculation.taxableAmount();
        BigDecimal vatLeft = calculation.vatAmount();
        BillingDocumentTax heaviest = null;
        for (java.util.Map.Entry<BigDecimal, BigDecimal[]> entry : sums.entrySet()) {
            BigDecimal base = calculation.taxableAmount().multiply(entry.getValue()[0]).divide(lineBase, 2, RoundingMode.HALF_UP);
            BigDecimal vat = calculation.vatAmount().multiply(entry.getValue()[1]).divide(lineVat, 2, RoundingMode.HALF_UP);
            baseLeft = baseLeft.subtract(base);
            vatLeft = vatLeft.subtract(vat);
            BillingDocumentTax row = BillingDocumentTax.builder()
                    .document(document)
                    .taxCode("VAT")
                    .taxName("TVA " + entry.getKey().toPlainString() + " %")
                    .rate(entry.getKey().setScale(4, RoundingMode.HALF_UP))
                    .taxableAmount(base)
                    .taxAmount(vat)
                    .build();
            rows.add(row);
            if (heaviest == null || row.getTaxAmount().compareTo(heaviest.getTaxAmount()) > 0) {
                heaviest = row;
            }
        }
        heaviest.setTaxableAmount(heaviest.getTaxableAmount().add(baseLeft));
        heaviest.setTaxAmount(heaviest.getTaxAmount().add(vatLeft));
        return rows;
    }

    private void saveClauses(BillingDocument document, List<CreateBillingDocumentClauseRequest> clauses) {
        if (clauses == null || clauses.isEmpty()) {
            return;
        }
        int order = 1;
        for (CreateBillingDocumentClauseRequest request : clauses) {
            clauseRepository.save(BillingDocumentClause.builder()
                    .document(document)
                    .clauseCode(request.clauseCode())
                    .title(request.title().trim())
                    .body(request.body().trim())
                    .displayOrder(request.displayOrder() == null ? order : request.displayOrder())
                    .build());
            order++;
        }
    }
}
