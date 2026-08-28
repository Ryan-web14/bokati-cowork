package com.sni.bokaticowork.core.maintenance;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Purge ciblee d'un membre et de toutes ses donnees.
 *
 * <p>Complete {@link DataPurgeController}, qui vide la base entiere : ici la suppression porte sur
 * un membre nomme, ce qui permet de retirer des donnees de test d'une base de production sans
 * toucher au reste.
 *
 * <p>Parcours prevu : lister les candidats, previsualiser ce que la suppression emporterait,
 * puis supprimer. Les deux premieres etapes sont en lecture seule et restent disponibles meme
 * lorsque l'interrupteur {@code bokati.maintenance.member-purge.enabled} est a false.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/admin/maintenance/members")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
public class MemberPurgeController {

    private final MemberPurgeService memberPurgeService;

    /** Membres crees avant une date · sert a retrouver les jeux de test restes en production. */
    @GetMapping("/purge-candidates")
    public ResponseEntity<List<MemberPurgeService.MemberCandidate>> candidates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdBefore,
            @RequestParam(defaultValue = "200") int limit) {
        return ResponseEntity.ok(memberPurgeService.candidates(createdBefore, limit));
    }

    /** Compte ce que la purge supprimerait, sans rien supprimer. */
    @GetMapping("/{memberCode}/purge-preview")
    public ResponseEntity<MemberPurgeService.PurgeReport> preview(@PathVariable String memberCode) {
        return ResponseEntity.ok(memberPurgeService.preview(memberCode));
    }

    @DeleteMapping("/{memberCode}")
    @Audited(module = "MAINTENANCE", action = "PURGE_MEMBER", ressource = "member")
    public ResponseEntity<?> purge(@PathVariable String memberCode,
                                   @RequestParam(defaultValue = "false") boolean confirm,
                                   @RequestParam(required = false) String reason) {
        if (!confirm) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "message", "Suppression definitive du membre " + memberCode
                            + " et de toutes ses donnees. Ajouter ?confirm=true pour proceder.",
                    "preview", "GET " + ApiPath.V1 + "/admin/maintenance/members/" + memberCode + "/purge-preview"
            ));
        }
        return ResponseEntity.ok(memberPurgeService.purge(memberCode, reason));
    }

    @PostMapping("/purge-batch")
    @Audited(module = "MAINTENANCE", action = "PURGE_MEMBER_BATCH", ressource = "member")
    public ResponseEntity<MemberPurgeService.BatchPurgeReport> purgeBatch(
            @RequestBody BatchPurgeRequest request,
            @RequestParam(defaultValue = "false") boolean confirm) {
        if (!confirm) {
            throw new BadRequestException("Suppression definitive de " + request.memberCodes().size()
                    + " membre(s) · ajouter ?confirm=true pour proceder.");
        }
        return ResponseEntity.ok(memberPurgeService.purgeAll(request.memberCodes(), request.reason()));
    }

    /**
     * Les codes sont enumeres explicitement, jamais deduits d'une date : une purge de masse ne
     * doit pas pouvoir partir d'un seul parametre mal saisi.
     */
    public record BatchPurgeRequest(@NotEmpty List<String> memberCodes, String reason) {}
}
