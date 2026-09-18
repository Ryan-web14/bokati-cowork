package com.sni.bokaticowork.security.authentication.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.security.authentication.model.OneTimeToken;
import com.sni.bokaticowork.security.authentication.repository.OneTimeTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Les codes a usage unique, vus du support.
 *
 * <p>Quand un inscrit dit ne pas avoir recu son code, le support doit pouvoir voir qu'un code
 * existe, pour qui, depuis quand, et s'il a servi. Le code lui-meme est montre : c'est ce qui
 * permet de le lire au telephone a quelqu'un qui n'a pas recu le courriel.</p>
 *
 * <p>Ce n'est pas anodin · un code a usage unique est aussi un moyen de connexion. Chaque
 * consultation est journalisee avec le nom de qui l'a faite et ce qu'elle a filtre, et l'acces
 * est reserve aux administrateurs. Un code echu ou deja utilise est montre comme tel : il ne sert
 * plus a rien, sinon a comprendre ce qui s'est passe.</p>
 */
@Slf4j
@RestController
@RequestMapping(ApiPath.V1 + "/admin/one-time-tokens")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class OneTimeTokenAdminController {

    private final OneTimeTokenRepository tokenRepository;

    /**
     * @param status VALID, USED ou EXPIRED · un code non utilise et non echu est VALID
     */
    public record OneTimeTokenView(
            String token,
            String email,
            String userId,
            String firstname,
            String lastname,
            Instant createdAt,
            Instant expiredAt,
            boolean used,
            boolean expired,
            String status
    ) {
        static OneTimeTokenView of(OneTimeToken ott, Instant now) {
            boolean expired = ott.getExpiredAt() != null && !now.isBefore(ott.getExpiredAt());
            String status = ott.isUsed() ? "USED" : expired ? "EXPIRED" : "VALID";
            return new OneTimeTokenView(
                    ott.getToken(),
                    ott.getUsers() == null ? null : ott.getUsers().getEmail(),
                    ott.getUsers() == null ? null : ott.getUsers().getUserId(),
                    ott.getUsers() == null ? null : ott.getUsers().getFirstname(),
                    ott.getUsers() == null ? null : ott.getUsers().getLastname(),
                    ott.getCreatedAt(),
                    ott.getExpiredAt(),
                    ott.isUsed(),
                    expired,
                    status);
        }
    }

    /**
     * Liste les codes, les plus recents d'abord.
     *
     * @param email restreint a un titulaire · sans lui, tout le monde
     * @param used  {@code true} les codes deja utilises, {@code false} les autres, absent tous
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<PaginatedResponse<OneTimeTokenView>> list(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Boolean used,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        String normalizedEmail = StringUtils.hasText(email) ? email.trim().toLowerCase() : null;
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Instant now = Instant.now();

        log.info("Codes a usage unique consultes par {} · filtre email={} used={}",
                authentication == null ? "?" : authentication.getName(), normalizedEmail, used);

        return ResponseEntity.ok(new PaginatedResponse<>(
                tokenRepository.search(normalizedEmail, used, unsorted)
                        .map(ott -> OneTimeTokenView.of(ott, now))));
    }
}
