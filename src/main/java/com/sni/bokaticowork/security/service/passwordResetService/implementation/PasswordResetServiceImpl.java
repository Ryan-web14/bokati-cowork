package com.sni.bokaticowork.security.service.passwordResetService.implementation;

import com.sni.bokaticowork.core.communication.mailService.interfaces.PasswordResetMailService;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.admin.user.service.interfaces.UserService;
import com.sni.bokaticowork.security.model.PasswordResetToken;
import com.sni.bokaticowork.security.repository.PasswordResetTokenRepository;
import com.sni.bokaticowork.security.repository.RefreshTokenRepository;
import com.sni.bokaticowork.security.service.passwordResetService.interfaces.PasswordResetService;
import com.sni.bokaticowork.security.service.passwordResetService.support.PasswordResetNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private final PasswordResetTokenRepository passwordResetTokenRepo;
    private final UserService userService;
    private final PasswordResetMailService mailService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetNotifier notifier;

    @Value("${app.security.password-reset.expiration-ms:86400000}")
    private long expiration;

    @Override
    @Transactional
    public void generatePasswordResetToken(Users user) {
        generatePasswordResetToken(user, null);
    }

    @Override
    @Transactional
    public void generatePasswordResetToken(Users user, String triggeredBy) {
        passwordResetTokenRepo.invalidateAllTokensForUser(user.getId());

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .passwordToken(token)
                .used(false)
                .createdAt(Instant.now())
                .expiryDate(Instant.now().plusMillis(expiration))
                .expiration(expiration)
                .createdBy(triggeredBy)
                .user(user)
                .build();

        passwordResetTokenRepo.save(resetToken);

        mailService.sendPasswordResetMail(user, resetToken)
                .thenAccept(sent -> {
                    if (Boolean.TRUE.equals(sent)) {
                        log.info("Password reset email sent to {} (triggered by: {})",
                                user.getEmail(), triggeredBy != null ? triggeredBy : "self");
                    } else {
                        log.warn("Failed to send password reset email to {}", user.getEmail());
                    }
                });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTokenValid(String token) {
        return passwordResetTokenRepo.findByPasswordToken(token)
                .map(t -> !Boolean.TRUE.equals(t.getUsed()) && t.getExpiryDate().isAfter(Instant.now()))
                .orElse(false);
    }

    @Override
    @Transactional
    public void validatePasswordResetToken(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepo.findByPasswordToken(token)
                .orElseThrow(() -> new BadRequestException("Lien de réinitialisation invalide ou expiré"));

        if (Boolean.TRUE.equals(resetToken.getUsed())) {
            throw new BadRequestException("Ce lien de réinitialisation a déjà été utilisé");
        }

        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            throw new BadRequestException("Ce lien de réinitialisation a expiré. Veuillez en demander un nouveau");
        }

        Users user = resetToken.getUser();
        String userEmail = user.getEmail();
        String triggeredBy = resetToken.getCreatedBy();

        userService.resetUserPassword(user, newPassword);
        passwordResetTokenRepo.invalidateToken(token);
        refreshTokenRepository.revokeActiveTokensByUserId(user.getId());

        user.setFailedLoginAttempts(0);
        user.setIsAccountLocked(false);

        log.info("Password successfully reset for user: {} (triggered by: {})",
                userEmail, triggeredBy != null ? triggeredBy : "self");

        // Fire async notifications after commit — non-blocking, no thread held
        runAfterCommit(() -> {
            notifier.sendPasswordChangedConfirmation(userEmail);
            notifier.notifyAdminResetCompleted(triggeredBy, userEmail);
        });
    }

    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }
}
