package com.sni.bokaticowork.security.service.passwordResetService.implementation;


import com.sni.bokaticowork.core.communication.mailService.interfaces.PasswordResetMailService;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.admin.user.service.interfaces.UserService;
import com.sni.bokaticowork.security.model.PasswordResetToken;
import com.sni.bokaticowork.security.repository.PasswordResetTokenRepository;
import com.sni.bokaticowork.security.service.passwordResetService.interfaces.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Service
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private final PasswordResetTokenRepository passwordResetTokenRepo;
    private final UserService userService;
    private final PasswordResetMailService mailService;

    @Value("${app.security.password-reset.expiration-ms:900000}")
    private long expiration;


    @Override
    @Transactional
    public void generatePasswordResetToken(Users user) {

        passwordResetTokenRepo.invalidateAllTokensForUser(user.getId());
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .passwordToken(token)
                .used(false)
                .createdAt(Instant.now())
                .expiryDate(Instant.now().plusMillis(expiration))
                .expiration(expiration)
                .user(user)
                .build();
     
        passwordResetTokenRepo.save(resetToken);
        
        CompletableFuture<Boolean> sent = mailService.sendPasswordResetMail(user, resetToken);
        
        if(sent.isDone()){
            log.info("Password reset mail sent");
        }
        else{
            log.error("Could not send password reset mail to {}", user.getEmail());
        }
    }

    @Override
    @Transactional
    public void validatePasswordResetToken(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepo.findByPasswordToken(token)
                .orElseThrow(() -> {
                    log.error("Invalid password reset token: {}", token);
                    return new IllegalArgumentException("Invalid password reset token");
                });

        if (resetToken.getUsed()) {
            log.error("Password reset token already used: {}", token);
            throw new IllegalArgumentException("Password reset token has already been used");
        }

        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            log.error("Password reset token expired: {}", token);
            throw new IllegalArgumentException("Password reset token has expired");
        }

        Users user = resetToken.getUser();
        userService.resetUserPassword(user, newPassword);
        passwordResetTokenRepo.invalidateToken(token);

        log.info("Password successfully reset for user: {}", user.getEmail());
    }
}
