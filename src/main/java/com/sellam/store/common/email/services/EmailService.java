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

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Réinitialiser votre mot de passe - Sellam");
        message.setText(
                "Bonjour,\n\n" +
                        "Cliquez sur ce lien pour réinitialiser votre mot de passe :\n" +
                        frontendUrl + "/forgot-password?token=" + resetToken + "\n\n" +
                        "Ce lien expire dans 1 heure.\n\n" +
                        "Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.\n\n" +
                        "L'équipe Sellam"
        );
        mailSender.send(message);
    }

    public void sendBalanceReminderEmail(String toEmail, String shopName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Rappel : Heure de faire le bilan - " + shopName);
        message.setText(
                "Bonjour,\n\n" +
                        "Il est temps de faire le bilan pour votre boutique " + shopName + ".\n\n" +
                        "Accédez à Sellam pour enregistrer vos ventes du jour :\n" +
                        frontendUrl + "/daily-balance\n\n" +
                        "L'équipe Sellam"
        );
        mailSender.send(message);
    }
}