package com.sni.bokaticowork.features.subscription.subscription.pass.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.subscription.pass.enums.PassCredentialType;
import com.sni.bokaticowork.features.subscription.subscription.pass.service.PassCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Le support d'un pass · ce qu'on presente a la borne.
 *
 * <p>Distinct du pass lui-meme : revoquer un support n'annule pas le droit. Un telephone perdu
 * appelle un nouveau code, pas un nouveau pass.</p>
 */
@RestController
@RequestMapping(ApiPath.V1 + "/passes/{passNumber}/credentials")
@RequiredArgsConstructor
public class PassCredentialController {

    private final PassCredentialService credentialService;

    /**
     * Rend le support courant, renouvele s'il tourne et qu'il est arrive a echeance.
     *
     * <p>C'est ce que le portail appelle a chaque affichage. Le renouvellement a la lecture evite
     * de faire tourner des milliers de codes que personne ne regarde.</p>
     */
    @GetMapping
    public ResponseEntity<PassCredentialService.IssuedCredential> current(
            @PathVariable String passNumber,
            @RequestParam(required = false) PassCredentialType type) {
        return ResponseEntity.ok(credentialService.current(passNumber, type));
    }

    /** Emet un nouveau support et revoque le precedent du meme type. */
    @PostMapping
    public ResponseEntity<PassCredentialService.IssuedCredential> issue(
            @PathVariable String passNumber,
            @RequestParam(required = false) PassCredentialType type,
            @RequestParam(defaultValue = "true") boolean rotating,
            @RequestParam(required = false) String issuedBy) {
        return ResponseEntity.ok(credentialService.issue(passNumber, type, rotating, issuedBy));
    }

    /** Revoque tous les supports d'un pass · le geste du telephone perdu. */
    @DeleteMapping
    public ResponseEntity<Map<String, Integer>> revokeAll(@PathVariable String passNumber,
                                                          @RequestParam(required = false) String reason) {
        int revoked = credentialService.revokeAll(passNumber,
                reason == null ? "Révoqué à la demande du titulaire" : reason);
        return ResponseEntity.ok(Map.of("revoked", revoked));
    }
}
