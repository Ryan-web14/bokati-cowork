package com.sni.bokaticowork.core.communication.mailService.baseService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultEmailSender {


    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@localhost}")
    private  String fromEmail;


    @Async
    public CompletableFuture<Boolean> sendEmail(String to, String subject, String content){

        try{
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromEmail);
            mail.setTo(to);
            mail.setSubject(subject);
            mail.setText(content);

            mailSender.send(mail);
            log.info("Email sent to {}", to);
            return CompletableFuture.completedFuture(true);
        }catch (MailException ex){
            log.error("Error while sending email to {}", to);
            return CompletableFuture.completedFuture(false);
        }

    }

    @Async
    public CompletableFuture<Boolean> sendHtmlEmail(String to, String subject, String content) throws MessagingException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true); // HTML

            mailSender.send(message);
            log.info("HTML email sent to {}", to);
            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            log.error("Failed to send HTML email to {}", to, e);
            return CompletableFuture.completedFuture(false);
        }
    }
}
