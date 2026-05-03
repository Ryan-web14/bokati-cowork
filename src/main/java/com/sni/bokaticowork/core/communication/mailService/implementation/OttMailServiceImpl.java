package com.sni.bokaticowork.core.communication.mailService.implementation;


import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.communication.mailService.interfaces.OttMailService;
import com.sni.bokaticowork.security.admin.user.model.Users;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Service
@Slf4j
public class OttMailServiceImpl implements OttMailService {

    private final DefaultEmailSender emailSender;
    private final SpringTemplateEngine emailTemplteEngine;

    @Override
    @Async
    public CompletableFuture<Boolean> sendOneTimeTokenMail(Users user, String token) {

        String firstname = user.getEmail();

        Context context = new Context();
        context.setVariable("firstname", firstname);
        context.setVariable("token", token);

        String template = emailTemplteEngine.process("ott-login", context);

        try{
            String subject = "Votre code de connexion";
            log.info("OTP sent to {}", user.getEmail());
            return emailSender.sendHtmlEmail(user.getEmail(), subject, template);
        }catch (MessagingException e){
            log.error("Could not send OTP {}", user.getEmail());
            return CompletableFuture.completedFuture(false);
        }
    }
}
