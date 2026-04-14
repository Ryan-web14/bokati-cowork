package com.sni.bokaticowork.core.communication.mailService.interfaces;



import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.model.PasswordResetToken;

import java.util.concurrent.CompletableFuture;

public interface PasswordResetMailService {

    CompletableFuture<Boolean> sendPasswordResetMail(Users user, PasswordResetToken resetToken);
}
