package com.sni.bokaticowork.features.payment.controller;

import com.sni.bokaticowork.core.exception.customs.BaseException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import com.sni.bokaticowork.features.payment.service.pawaypay.PublicPaymentPageService;
import com.sni.bokaticowork.features.payment.service.pawaypay.PublicPaymentPageService.LinkClosedException;
import com.sni.bokaticowork.features.payment.service.pawaypay.PublicPaymentPageService.OperatorNeededException;
import com.sni.bokaticowork.features.payment.service.pawaypay.PublicPaymentPageService.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * La page de paiement d'un lien · servie par notre serveur, sans compte, derriere le jeton du lien.
 *
 * <p>Trois ecrans : le formulaire (pays, numero, operateur si PawaPay ne le devine pas), le suivi
 * qui se met a jour seul jusqu'a la reponse de l'operateur, et la page d'un lien qui ne mene plus a
 * rien (deja regle, expire, inconnu). Chemin public par {@code /public/**} · le depot passe par la
 * limite des ecritures publiques (10 par 10 minutes et par adresse).</p>
 */
@Controller
@RequestMapping("/public/pay")
@RequiredArgsConstructor
public class PublicPaymentPageController {

    private final PublicPaymentPageService service;

    @GetMapping("/{token}")
    public ModelAndView open(@PathVariable String token) {
        try {
            Page page = service.open(token);
            if (page.depositInProgress() != null) {
                return new ModelAndView("redirect:" + statusPath(token, page.depositInProgress()));
            }
            return form(token, page, null, null, null, false);
        } catch (LinkClosedException ex) {
            return closed(ex.reason());
        } catch (ResourceNotFoundException ex) {
            return closed("Ce lien de paiement n'existe pas. Vérifiez l'adresse reçue.");
        }
    }

    @PostMapping("/{token}")
    public ModelAndView pay(@PathVariable String token,
                            @RequestParam(required = false) String country,
                            @RequestParam(required = false) String phone,
                            @RequestParam(required = false) String operator) {
        try {
            String depositId = service.start(token, country, phone, operator);
            return new ModelAndView("redirect:" + statusPath(token, depositId));
        } catch (LinkClosedException ex) {
            return closed(ex.reason());
        } catch (ResourceNotFoundException ex) {
            return closed("Ce lien de paiement n'existe pas. Vérifiez l'adresse reçue.");
        } catch (OperatorNeededException ex) {
            return form(token, service.open(token), country, phone,
                    "Nous n'avons pas reconnu l'opérateur de ce numéro. Choisissez-le ci-dessous.", true);
        } catch (BaseException ex) {
            return form(token, service.open(token), country, phone, payerMessage(ex.getMessage()), false);
        }
    }

    @GetMapping("/{token}/suivi/{depositId}")
    public ModelAndView status(@PathVariable String token, @PathVariable String depositId) {
        try {
            MobileMoneyDepositResponse deposit = service.status(token, depositId);
            ModelAndView mav = new ModelAndView("payment/pay-status");
            mav.addObject("deposit", deposit);
            mav.addObject("stateUrl", statusPath(token, depositId) + "/etat");
            mav.addObject("retryUrl", formPath(token));
            return mav;
        } catch (ResourceNotFoundException ex) {
            return closed("Ce paiement est introuvable. Vérifiez l'adresse reçue.");
        }
    }

    /** L'etat du depot, pour la page de suivi qui se rafraichit seule. */
    @GetMapping(value = "/{token}/suivi/{depositId}/etat", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> state(@PathVariable String token, @PathVariable String depositId) {
        try {
            MobileMoneyDepositResponse deposit = service.status(token, depositId);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("phase", deposit.phase());
            body.put("status", deposit.status());
            // Le message ecrit pour le client, jamais le code ni le texte brut de l'operateur.
            body.put("message", deposit.userMessage());
            return ResponseEntity.ok(body);
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    private ModelAndView form(String token, Page page, String country, String phone, String error, boolean needOperator) {
        ModelAndView mav = new ModelAndView("payment/pay-form");
        mav.addObject("page", page);
        mav.addObject("action", formPath(token));
        mav.addObject("selectedCountry", country != null ? country
                : page.countries().isEmpty() ? null : page.countries().getFirst().code());
        mav.addObject("phone", phone);
        mav.addObject("error", error);
        mav.addObject("needOperator", needOperator);
        return mav;
    }

    private ModelAndView closed(String reason) {
        ModelAndView mav = new ModelAndView("payment/pay-closed");
        mav.addObject("reason", reason);
        return mav;
    }

    /** Les messages internes en anglais ne parlent pas au payeur · on traduit ceux qu'il peut rencontrer. */
    static String payerMessage(String message) {
        if (message == null) {
            return "Le paiement n'a pas pu être lancé. Réessayez.";
        }
        if (message.startsWith("Payment intent has pending mobile money")) {
            return "Un paiement est déjà en cours sur ce lien. Validez-le sur votre téléphone, ou réessayez dans quelques minutes.";
        }
        if (message.startsWith("Payment intent has expired")) {
            return "Ce lien de paiement a expiré. Demandez-en un nouveau.";
        }
        return message;
    }

    private static String formPath(String token) {
        return UriComponentsBuilder.fromPath("/public/pay/{token}").buildAndExpand(token).encode().toUriString();
    }

    private static String statusPath(String token, String depositId) {
        return UriComponentsBuilder.fromPath("/public/pay/{token}/suivi/{depositId}")
                .buildAndExpand(token, depositId).encode().toUriString();
    }
}
