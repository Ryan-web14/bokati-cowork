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

    // ------------------------------------------------------------------ multidevise

    /** Parite fixe du franc CFA : 1 EUR = 655,957 XAF. */
    public static final BigDecimal EUR_PARITY = new BigDecimal("655.957");

    @org.springframework.beans.factory.annotation.Autowired
    private com.sni.bokaticowork.features.accounting.service.support.AccountingSettings settings;

    /** Devises suivies (reglage {@code accounting.currencies}). */
    public List<String> currencies() {
        String value = settings == null ? null : settings.text("accounting.currencies", "EUR,USD,GBP,CHF,CNY,JPY,CAD,ZAR");
        return Arrays.stream((value == null ? "EUR" : value).split(",")).map(String::trim).map(code -> code.toUpperCase(Locale.ROOT))
                .filter(code -> code.matches("[A-Z]{3}") && !BASE.equals(code)).distinct().toList();
    }

    public record CurrencyRate(String currency, BigDecimal rate, LocalDate validOn, String source, String provider) {
    }

    /** Dernier taux connu de chaque devise suivie. */
    @Transactional(readOnly = true)
    public List<CurrencyRate> board(LocalDate date) {
        LocalDate day = date == null ? LocalDate.now() : date;
        List<CurrencyRate> result = new ArrayList<>();
        for (String currency : currencies()) {
            Optional<CashExchangeRate> rate = rateRepository.findFirstByCurrencyAndValidOnLessThanEqualOrderByValidOnDesc(currency, day);
            if (rate.isPresent()) {
                result.add(new CurrencyRate(currency, rate.get().getRate(), rate.get().getValidOn(), rate.get().getSource(), rate.get().getProvider()));
            } else if ("EUR".equals(currency)) {
                result.add(new CurrencyRate("EUR", EUR_PARITY, day, "PARITY", "Parite fixe"));
            } else {
                result.add(new CurrencyRate(currency, null, null, null, null));
            }
        }
        return result;
    }

    /** Conversion entre deux devises quelconques, par le XAF. */
    @Transactional(readOnly = true)
    public BigDecimal convert(BigDecimal amount, String from, String to, LocalDate date) {
        LocalDate day = date == null ? LocalDate.now() : date;
        BigDecimal fromRate = rate(from, day).orElseThrow(() -> new BadRequestException("Aucun taux " + normalize(from) + " au " + day));
        BigDecimal toRate = rate(to, day).orElseThrow(() -> new BadRequestException("Aucun taux " + normalize(to) + " au " + day));
        return amount.multiply(fromRate).divide(toRate, 4, RoundingMode.HALF_UP);
    }

    /**
     * Recuperation des taux du jour sur internet (reglage {@code accounting.exchange.provider-url},
     * par defaut l'API Frankfurter, taux de reference de la Banque centrale europeenne, base EUR), puis
     * conversion en XAF par la parite fixe. Un taux saisi a la main pour le meme jour n'est jamais
     * remplace.
     */
    public List<CashExchangeRate> fetch() {
        String url = settings.text("accounting.exchange.provider-url", "https://api.frankfurter.dev/v1/latest?from=EUR");
        String body;
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).followRedirects(java.net.http.HttpClient.Redirect.NORMAL).build();
            java.net.http.HttpResponse<String> response = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(20)).header("Accept", "application/json").GET().build(),
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new BadRequestException("Le fournisseur de taux a repondu " + response.statusCode());
            }
            body = response.body();
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException("Fournisseur de taux injoignable (" + url + ") : " + exception.getMessage());
        }
        return store(parseEuroRates(body), host(url));
    }

    /** Lecture d'une reponse {"base":"EUR","date":"AAAA-MM-JJ","rates":{"USD":1.08,...}}. */
    public static ProviderRates parseEuroRates(String json) {
        try {
            com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
            String base = node.path("base").asText(node.path("base_code").asText("EUR"));
            if (!"EUR".equalsIgnoreCase(base)) {
                throw new BadRequestException("Le fournisseur doit donner des taux en base EUR (recu : " + base + ")");
            }
            String date = node.path("date").asText(null);
            com.fasterxml.jackson.databind.JsonNode rates = node.has("rates") ? node.path("rates") : node.path("conversion_rates");
            Map<String, BigDecimal> values = new TreeMap<>();
            rates.fields().forEachRemaining(entry -> values.put(entry.getKey().toUpperCase(Locale.ROOT), entry.getValue().decimalValue()));
            return new ProviderRates(date == null || date.isBlank() ? LocalDate.now() : LocalDate.parse(date.substring(0, 10)), values);
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException("Reponse du fournisseur de taux illisible : " + exception.getMessage());
        }
    }

    public record ProviderRates(LocalDate date, Map<String, BigDecimal> perEuro) {
    }

    /** Enregistre les taux en XAF : 1 devise = parite / (devises pour 1 EUR). */
    public List<CashExchangeRate> store(ProviderRates rates, String provider) {
        BigDecimal parity = parity();
        List<CashExchangeRate> saved = new ArrayList<>();
        for (String currency : currencies()) {
            BigDecimal xaf;
            String source;
            if ("EUR".equals(currency)) {
                xaf = parity;
                source = "PARITY";
            } else {
                BigDecimal perEuro = rates.perEuro().get(currency);
                if (perEuro == null || perEuro.signum() <= 0) {
                    continue;
                }
                xaf = parity.divide(perEuro, 6, RoundingMode.HALF_UP);
                source = "AUTO";
            }
            Optional<CashExchangeRate> existing = rateRepository.findByCurrencyAndValidOn(currency, rates.date());
            if (existing.isPresent() && "MANUAL".equals(existing.get().getSource())) {
                continue;
            }
            CashExchangeRate rate = existing.orElseGet(() -> CashExchangeRate.builder().currency(currency).validOn(rates.date()).build());
            rate.setRate(xaf.setScale(6, RoundingMode.HALF_UP));
            rate.setSource(source);
            rate.setProvider("PARITY".equals(source) ? "Parite fixe" : provider);
            rate.setSetBy("SYSTEM");
            saved.add(rateRepository.save(rate));
        }
        return saved;
    }

    BigDecimal parity() {
        try {
            return new BigDecimal(settings.text("accounting.exchange.eur-parity", "655.957").trim());
        } catch (RuntimeException exception) {
            return EUR_PARITY;
        }
    }

    private static String host(String url) {
        try {
            return java.net.URI.create(url).getHost();
        } catch (RuntimeException exception) {
            return url;
        }
    }

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
        existing.setSource("MANUAL");
        existing.setProvider(null);
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
        Optional<BigDecimal> found = rateRepository.findFirstByCurrencyAndValidOnLessThanEqualOrderByValidOnDesc(code, date).map(CashExchangeRate::getRate);
        // L'euro a une parite fixe avec le franc CFA : sans taux saisi, elle s'applique.
        return found.isPresent() || !"EUR".equals(code) ? found : Optional.of(settings == null ? EUR_PARITY : parity());
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
