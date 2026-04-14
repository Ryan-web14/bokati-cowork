package com.sni.bokaticowork.core.communication.mailService.interfaces;



import com.sni.bokaticowork.security.admin.user.model.Users;

import java.util.concurrent.CompletableFuture;

public interface OttMailService {

    CompletableFuture<Boolean> sendOneTimeTokenMail(Users user, String token);
}
