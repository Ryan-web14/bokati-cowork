package com.sni.bokaticowork.features.payment.service.pawaypay;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.utils.phone.PhoneNumbers;
import com.sni.bokaticowork.features.payment.dto.request.InitiateMobileMoneyDepositRequest;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;
import com.sni.bokaticowork.features.payment.mapper.interfaces.PaymentMapper;
import com.sni.bokaticowork.features.payment.model.PawapayDeposit;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.provider.pawaypay.CongoCorrespondent;
import com.sni.bokaticowork.features.payment.provider.pawaypay.PawapayClient;
import com.sni.bokaticowork.features.payment.repository.PawapayDepositRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentIntentRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * La page de paiement servie par notre serveur, derriere un lien de paiement.
 *
 * <p>Le payeur choisit son pays et saisit son numero · l'operateur est devine par PawaPay, et
 * demande au payeur seulement si PawaPay ne sait pas le dire. Le depot part par le meme chemin que
 * tous les autres ({@link PaymentService#initiateMobileMoneyDeposit}) : signe, suivi dans
 * {@code pawapay_deposit}, relu par le worker, rapproche par les rappels. La page hebergee de
 * PawaPay, utilisee avant, ne creait aucun suivi · un lien jamais paye bloquait l'intention.</p>
 *
 * <p>Le lien donne acces a une seule intention, par son jeton · un depot d'une autre intention
 * repond introuvable, comme un depot inexistant.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicPaymentPageService {

    static final String CREATED_BY = "payment-link";

    /** Au plus une relecture chez l'operateur par depot dans cet intervalle, quel que soit le rafraichissement. */
    static final Duration RECHECK_INTERVAL = Duration.ofSeconds(20);

    private static final Set<PaymentIntentStatus> PAYABLE = Set.of(
            PaymentIntentStatus.PENDING, PaymentIntentStatus.PROCESSING, PaymentIntentStatus.AUTHORIZED);

    private final PaymentIntentRepository intentRepository;
    private final PaymentService paymentService;
    private final PawapayDepositRepository depositRepository;
    private final PawapayDepositService depositService;
    private final MobileMoneySupervisionService supervision;
    private final MobileMoneyInitiationGuard guard;
    private final PaymentMapper mapper;
    private final ObjectProvider<PawapayClient> client;

    /** Le lien ne mene plus a un paiement · le motif sert a la page, il n'est pas une erreur technique. */
    public static class LinkClosedException extends RuntimeException {
        private final String reason;

        public LinkClosedException(String reason) {
            super(reason);
            this.reason = reason;
        }

        public String reason() {
            return reason;
        }
    }

    /** PawaPay n'a pas devine l'operateur · la page le demande au payeur. */
    public static class OperatorNeededException extends RuntimeException {
        public OperatorNeededException() {
            super("Choisissez votre opérateur.");
        }
    }

    public record Operator(String code, String name) {
    }

    public record Country(String code, String name, String dialCode, List<Operator> operators) {
    }

    public record Page(String intentNumber, BigDecimal amount, String currency, String customerName,
                       List<Country> countries, String depositInProgress) {
    }

    // -----------------------------------------------------------------------------------------
    // Ouverture
    // -----------------------------------------------------------------------------------------

    public Page open(String token) {
        PaymentIntent intent = payableIntent(token);
        PaymentIntentResponse view = mapper.toIntentResponse(intent);
        String inProgress = depositRepository.findAllByIntentNumberOrderByCreatedAtDesc(intent.getIntentNumber()).stream()
                .filter(PawapayDeposit::pending)
                .map(PawapayDeposit::getDepositId)
                .findFirst()
                .orElse(null);
        return new Page(intent.getIntentNumber(), view.remainingAmount(), intent.getCurrency(), view.customerName(),
                countriesFor(intent.getCurrency()), inProgress);
    }

    /**
     * Les pays proposes · ceux dont un operateur active encaisse dans la devise de l'intention.
     * Un paiement en XAF ne se regle ni en XOF ni en CDF.
     */
    public List<Country> countriesFor(String currency) {
        Map<String, List<CongoCorrespondent>> byCountry = new LinkedHashMap<>();
        Arrays.stream(CongoCorrespondent.values())
                .filter(CongoCorrespondent::selectable)
                .filter(guard::isEnabled)
                .filter(operator -> currency == null || currency.equalsIgnoreCase(operator.getCurrency()))
                .forEach(operator -> byCountry.computeIfAbsent(operator.getCountryCode(), k -> new java.util.ArrayList<>())
                        .add(operator));
        return byCountry.entrySet().stream()
                .map(entry -> new Country(entry.getKey(), countryName(entry.getKey()),
                        entry.getValue().getFirst().dialCode(),
                        entry.getValue().stream().map(op -> new Operator(op.name(), op.getDisplayName())).toList()))
                .toList();
    }

    // -----------------------------------------------------------------------------------------
    // Paiement
    // -----------------------------------------------------------------------------------------

    /**
     * Lance le depot · rend son identifiant, pour la page de suivi.
     *
     * @param operatorCode l'operateur choisi par le payeur, ou vide pour le faire deviner par PawaPay
     */
    public String start(String token, String countryCode, String phone, String operatorCode) {
        PaymentIntent intent = payableIntent(token);
        Country country = countriesFor(intent.getCurrency()).stream()
                .filter(c -> c.code().equalsIgnoreCase(countryCode == null ? "" : countryCode.trim()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Choisissez votre pays."));
        if (!StringUtils.hasText(phone)) {
            throw new BadRequestException("Saisissez votre numéro de téléphone.");
        }
        CongoCorrespondent operator = StringUtils.hasText(operatorCode)
                ? chosenOperator(country, operatorCode)
                : predictedOperator(country, phone);

        BigDecimal amount = mapper.toIntentResponse(intent).remainingAmount();
        MobileMoneyDepositResponse deposit = paymentService.initiateMobileMoneyDeposit(intent.getIntentNumber(),
                new InitiateMobileMoneyDepositRequest(intent.getIntentNumber(), phone, operator, amount, CREATED_BY, null));
        return deposit.depositId();
    }

    private CongoCorrespondent chosenOperator(Country country, String operatorCode) {
        return country.operators().stream()
                .filter(op -> op.code().equalsIgnoreCase(operatorCode.trim()))
                .map(op -> CongoCorrespondent.valueOf(op.code()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Cet opérateur n'est pas disponible pour ce pays."));
    }

    /**
     * L'operateur du numero, d'apres PawaPay · parmi ceux du pays choisi et actives.
     *
     * <p>Un pays a un seul operateur actif n'a pas besoin de prediction. Sinon, un numero que PawaPay
     * ne sait pas classer, ou classe hors du pays choisi, fait demander l'operateur au payeur.</p>
     */
    private CongoCorrespondent predictedOperator(Country country, String phone) {
        if (country.operators().size() == 1) {
            return CongoCorrespondent.valueOf(country.operators().getFirst().code());
        }
        CongoCorrespondent any = CongoCorrespondent.valueOf(country.operators().getFirst().code());
        String msisdn = PhoneNumbers.toE164(phone, any.dialCode(), any.keepsTrunkZero())
                .orElseThrow(() -> new BadRequestException("Le numéro de téléphone n'est pas valide."));
        PawapayClient pawapay = client.getIfAvailable();
        if (pawapay == null) {
            throw new OperatorNeededException();
        }
        PawapayClient.ProviderPrediction prediction;
        try {
            prediction = pawapay.predictProvider(msisdn);
        } catch (PawapayClient.ProviderUnreachableException ex) {
            log.warn("Prediction d'operateur indisponible · le payeur choisira : {}", ex.getMessage());
            throw new OperatorNeededException();
        }
        if (prediction == null || prediction.provider() == null) {
            throw new OperatorNeededException();
        }
        return country.operators().stream()
                .map(op -> CongoCorrespondent.valueOf(op.code()))
                .filter(op -> op.providerCode().equalsIgnoreCase(prediction.provider())
                        || op.name().equalsIgnoreCase(prediction.provider()))
                .findFirst()
                .orElseThrow(OperatorNeededException::new);
    }

    // -----------------------------------------------------------------------------------------
    // Suivi
    // -----------------------------------------------------------------------------------------

    /**
     * Ou en est le depot · relu chez l'operateur au plus toutes les {@link #RECHECK_INTERVAL}.
     *
     * <p>Le suivi reste lisible apres le paiement, quand l'intention n'est plus payable · c'est la
     * que le payeur voit « paiement confirme ».</p>
     */
    public MobileMoneyDepositResponse status(String token, String depositId) {
        PaymentIntent intent = intentByToken(token);
        PawapayDeposit deposit = depositRepository.findByDepositId(depositId == null ? "" : depositId.trim())
                .filter(d -> intent.getIntentNumber().equals(d.getIntentNumber()))
                .orElseThrow(() -> new ResourceNotFoundException("Paiement introuvable"));
        if (deposit.pending() && dueForRecheck(deposit)) {
            try {
                return supervision.recheck(deposit.getDepositId());
            } catch (RuntimeException ex) {
                log.warn("Relecture du depot {} impossible depuis la page de paiement : {}", depositId, ex.getMessage());
            }
        }
        return depositService.get(deposit.getDepositId());
    }

    private static boolean dueForRecheck(PawapayDeposit deposit) {
        Instant last = deposit.getLastStatusCheckedAt();
        return last == null || last.isBefore(Instant.now().minus(RECHECK_INTERVAL));
    }

    // -----------------------------------------------------------------------------------------
    // Jeton
    // -----------------------------------------------------------------------------------------

    private PaymentIntent intentByToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new ResourceNotFoundException("Lien de paiement introuvable");
        }
        return intentRepository.findByPaymentLinkToken(token.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Lien de paiement introuvable"));
    }

    private PaymentIntent payableIntent(String token) {
        PaymentIntent intent = intentByToken(token);
        if (!PAYABLE.contains(intent.getStatus())) {
            throw new LinkClosedException(intent.getStatus() == PaymentIntentStatus.SUCCEEDED
                    ? "Ce paiement est déjà réglé. Merci."
                    : "Ce lien de paiement n'est plus valable. Demandez-en un nouveau.");
        }
        if (intent.getPaymentLinkExpiresAt() != null && intent.getPaymentLinkExpiresAt().isBefore(Instant.now())) {
            throw new LinkClosedException("Ce lien de paiement a expiré. Demandez-en un nouveau.");
        }
        BigDecimal remaining = mapper.toIntentResponse(intent).remainingAmount();
        if (remaining == null || remaining.signum() <= 0) {
            throw new LinkClosedException("Ce paiement est déjà réglé. Merci.");
        }
        return intent;
    }

    static String countryName(String code) {
        return switch (code) {
            case "COG" -> "Congo-Brazzaville";
            case "COD" -> "RD Congo";
            case "GAB" -> "Gabon";
            case "CMR" -> "Cameroun";
            case "CIV" -> "Côte d'Ivoire";
            case "SEN" -> "Sénégal";
            default -> code;
        };
    }
}
