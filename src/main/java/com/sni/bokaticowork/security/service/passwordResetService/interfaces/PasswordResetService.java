package com.sni.bokaticowork.security.service.passwordResetService.interfaces;


import com.sni.bokaticowork.security.admin.user.model.Users;

public interface PasswordResetService {

    void generatePasswordResetToken(Users user);
    void validatePasswordResetToken(String token, String newPassword);
}
