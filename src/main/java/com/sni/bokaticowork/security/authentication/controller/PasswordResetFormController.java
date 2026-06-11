package com.sni.bokaticowork.security.authentication.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.security.service.passwordResetService.interfaces.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping(ApiPath.V1 + "/auth/password-reset")
public class PasswordResetFormController {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final PasswordResetService passwordResetService;

    @GetMapping("/form")
    public String showForm(@RequestParam(required = false) String token, Model model) {
        if (!StringUtils.hasText(token)) {
            model.addAttribute("reason", "Lien invalide. Aucun jeton fourni.");
            return "auth/password-reset-error";
        }
        if (!passwordResetService.isTokenValid(token)) {
            model.addAttribute("reason", "Ce lien de réinitialisation est invalide ou a expiré. Veuillez en demander un nouveau.");
            return "auth/password-reset-error";
        }
        model.addAttribute("token", token);
        return "auth/password-reset-form";
    }

    @PostMapping("/form")
    public String processForm(@RequestParam String token,
                              @RequestParam String newPassword,
                              @RequestParam String confirmPassword,
                              Model model) {
        if (!StringUtils.hasText(token)) {
            model.addAttribute("reason", "Jeton manquant. Veuillez utiliser le lien reçu par email.");
            return "auth/password-reset-error";
        }

        if (!StringUtils.hasText(newPassword) || newPassword.length() < MIN_PASSWORD_LENGTH) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Le mot de passe doit contenir au moins " + MIN_PASSWORD_LENGTH + " caractères.");
            return "auth/password-reset-form";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Les mots de passe ne correspondent pas.");
            return "auth/password-reset-form";
        }

        try {
            passwordResetService.validatePasswordResetToken(token, newPassword);
            return "auth/password-reset-success";
        } catch (Exception ex) {
            log.warn("Password reset form submission failed: {}", ex.getMessage());
            model.addAttribute("reason", ex.getMessage());
            return "auth/password-reset-error";
        }
    }
}
