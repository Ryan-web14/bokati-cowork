package com.sni.bokaticowork.features.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentRequest;
import com.sni.bokaticowork.features.payment.dto.request.InitiateMobileMoneyDepositRequest;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyCallbackResponse;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyProviderOptionResponse;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyTestDepositResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.provider.pawaypay.CongoCorrespondent;
import com.sni.bokaticowork.features.payment.provider.pawaypay.PawapayProperties;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import com.sni.bokaticowork.features.payment.service.pawaypay.MobileMoneySupervisionService;
import com.sni.bokaticowork.features.payment.service.pawaypay.PawapayCallbackGateway;
import com.sni.bokaticowork.features.payment.service.pawaypay.PawapayDepositService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(ApiPath.V1 + "/payments/mobile-money")
@RequiredArgsConstructor
public class MobileMoneyController {

    private static final String STAFF = "hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')";

    private final PaymentService paymentService;
    private final PawapayDepositService depositService;
    private final PawapayCallbackGateway callbackGateway;
    private final MobileMoneySupervisionService supervision;
    private final PawapayProperties properties;
    private final ObjectMapper objectMapper;

    /** La liste des operateurs alimente le formulaire de paiement · elle ne dit rien de personne. */
    @GetMapping("/providers")
    public ResponseEntity<List<MobileMoneyProviderOptionResponse>> providers() {
        return ResponseEntity.ok(depositService.providers());
    }

    // ---------------------------------------------------------------------------------------
    // Suivi · reserve au personnel
    // ---------------------------------------------------------------------------------------

    /**
     * Un depot porte un numero de telephone, un montant et un client · il ne se lit pas sans compte.
     *
     * <p>Cette route repondait a qui la demandait : un identifiant de depot suffisait a apprendre
     * qui avait paye quoi et avec quel numero. Le client suit desormais son propre paiement par
     * {@code /client/billing/mobile-money/deposits/{depositId}}, qui verifie qu'il est bien le sien.</p>
     */
    @GetMapping("/deposits/{depositId}")
    @PreAuthorize(STAFF)
    public ResponseEntity<MobileMoneyDepositResponse> getDeposit(@PathVariable String depositId) {
        return ResponseEntity.ok(depositService.get(depositId));
    }

    /** Les depots, filtrables par statut · {@code UNRESOLVED} donne la file de rapprochement. */
    @GetMapping("/deposits")
    @PreAuthorize(STAFF)
    public ResponseEntity<PaginatedResponse<MobileMoneyDepositResponse>> listDeposits(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(supervision.deposits(status, pageable));
    }

    /** Relit le statut aupres de l'operateur sans attendre la prochaine echeance. */
    @PostMapping("/deposits/{depositId}/recheck")
    @PreAuthorize(STAFF)
    public ResponseEntity<MobileMoneyDepositResponse> recheck(@PathVariable String depositId) {
        return ResponseEntity.ok(supervision.recheck(depositId));
    }

    /** Les rappels recus et ce qu'on en a fait · de quoi expliquer un paiement manquant. */
    @GetMapping("/callbacks")
    @PreAuthorize(STAFF)
    public ResponseEntity<PaginatedResponse<MobileMoneyCallbackResponse>> listCallbacks(
            @RequestParam(required = false) String outcome,
            @PageableDefault(size = 20, sort = "receivedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(supervision.callbacks(outcome, pageable));
    }

    // ---------------------------------------------------------------------------------------
    // Essai
    // ---------------------------------------------------------------------------------------

    /**
     * Envoie une vraie demande de 10 XAF sur un vrai telephone · d'ou deux verrous.
     *
     * <p>Cette route etait ouverte a tous en production : n'importe qui pouvait faire sonner
     * n'importe quel numero, autant de fois qu'il le voulait, avec notre compte marchand. Elle
     * demande desormais un compte d'administration et doit etre explicitement activee.</p>
     */
    @PostMapping("/pawaypay/test/{phoneNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<MobileMoneyTestDepositResponse> testDeposit(@PathVariable String phoneNumber) {
        if (!properties.isTestEndpointEnabled()) {
            throw new ForbiddenException("L'essai mobile money est désactivé. "
                    + "Activez bokati.payment.pawaypay.test-endpoint-enabled pour l'utiliser.");
        }
        String normalizedPhone = phoneNumber == null ? "" : phoneNumber.replaceAll("[^\\d]", "");
        String testRun = String.valueOf(Instant.now().toEpochMilli());
        String testCode = "MM-TEST-" + normalizedPhone + "-" + testRun;
        PaymentIntentResponse intent = paymentService.createIntent(new CreatePaymentIntentRequest(
                "CUSTOMER",
                testCode,
                BigDecimal.TEN,
                "XAF",
                "Bokati test " + testRun.substring(Math.max(0, testRun.length() - 6)),
                "MOBILE_MONEY_TEST",
                normalizedPhone + "-" + testRun,
                "MM-TEST-" + testRun,
                Instant.now().plus(15, ChronoUnit.MINUTES),
                writeJson(Map.of(
                        "test", true,
                        "testRun", testRun,
                        "phoneNumber", normalizedPhone,
                        "provider", CongoCorrespondent.MTN_MOMO_COG.providerCode()
                ))
        ));
        MobileMoneyDepositResponse deposit = paymentService.initiateMobileMoneyDeposit(
                intent.intentNumber(),
                new InitiateMobileMoneyDepositRequest(
                        intent.intentNumber(),
                        normalizedPhone,
                        CongoCorrespondent.MTN_MOMO_COG,
                        BigDecimal.TEN,
                        "backend-test",
                        null
                )
        );
        return ResponseEntity.ok(new MobileMoneyTestDepositResponse(
                deposit,
                paymentService.getIntent(intent.intentNumber()),
                depositService.providers()
        ));
    }

    // ---------------------------------------------------------------------------------------
    // Rappels de l'operateur
    // ---------------------------------------------------------------------------------------

    /**
     * Rappel de PawaPay sur un depot · toujours 200, un non-2xx le fait rejouer sans fin.
     *
     * <p>Tout le discernement est dans la passerelle : signature, relecture du statut chez
     * l'operateur quand elle manque, et trace de ce qui a ete fait.</p>
     */
    @PostMapping({"/pawapay/callback", "/pawaypay/callback"})
    public ResponseEntity<Void> depositCallback(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-PawaPay-Signature", required = false) String signature,
            HttpServletRequest request) {
        callbackGateway.receiveDeposit(rawBody, signature, clientIp(request));
        return ResponseEntity.ok().build();
    }

    /** Rappel de PawaPay sur un remboursement · meme discernement, meme trace. */
    @PostMapping({"/pawapay/refund-callback", "/pawaypay/refund-callback"})
    public ResponseEntity<Void> refundCallback(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-PawaPay-Signature", required = false) String signature,
            HttpServletRequest request) {
        callbackGateway.receiveRefund(rawBody, signature, clientIp(request));
        return ResponseEntity.ok().build();
    }

    /**
     * Retour du navigateur apres la page de paiement hebergee.
     *
     * <p>Le depot est relu aupres de l'operateur, puis le client est renvoye vers la page de
     * resultat. Rien n'est cru sur parole ici non plus · le parametre d'URL ne dit rien.</p>
     */
    @GetMapping("/pawapay/return")
    public ResponseEntity<Void> paymentPageReturn(@RequestParam("depositId") String depositId) {
        String status = "PROCESSING";
        try {
            MobileMoneyDepositResponse deposit = supervision.recheck(depositId);
            status = deposit.phase() == null ? deposit.status() : deposit.phase();
        } catch (Exception ex) {
            log.warn("Retour PawaPay · statut du depot {} non relu : {}", depositId, ex.getMessage());
        }

        String base = properties.getPaymentPageResultUrl();
        String target = StringUtils.hasText(base)
                ? base + (base.contains("?") ? "&" : "?") + "depositId=" + depositId + "&status=" + status
                : "/";
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request == null ? null : request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request == null ? null : request.getRemoteAddr();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }
}
