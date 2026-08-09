package com.sellam.store.common.email.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Vérifiez votre adresse email - Sellam");
        message.setText(
                "Bonjour,\n\n" +
                        "Cliquez sur ce lien pour vérifier votre adresse email :\n" +
                        frontendUrl + "/verify-email?token=" + token + "\n\n" +
                        "Ce lien expire dans 24 heures.\n\n" +
                        "L'équipe Sellam"
        );
        mailSender.send(message);
    }
}