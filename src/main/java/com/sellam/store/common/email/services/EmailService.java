package com.sellam.store.common.email.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    private void sendMailSafely(SimpleMailMessage message, String description) {
        try {
            mailSender.send(message);
            log.info("Email sent successfully: {}", description);
        } catch (MailException e) {
            log.warn("Failed to send email ({}): {} - will retry asynchronously if configured", description, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending email ({}): {}", description, e.getMessage(), e);
        }
    }

    @Async
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
        sendMailSafely(message, "verification email for " + toEmail);
    }

    @Async
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
        sendMailSafely(message, "password reset email for " + toEmail);
    }

    @Async
    public void sendBalanceReminderEmail(String toEmail, String shopName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Rappel : Clôturez votre caisse - " + shopName);
        message.setText(
                "Bonjour,\n\n" +
                        "Il est temps de clôturer votre session de caisse pour la boutique " + shopName + ".\n\n" +
                        "Accédez à Sellam pour vérifier et clôturer votre caisse :\n" +
                        frontendUrl + "/cash\n\n" +
                        "L'équipe Sellam"
        );
        sendMailSafely(message, "balance reminder email for " + shopName + " to " + toEmail);
    }

    @Async
    public void sendReportByEmail(String toEmail, String reportName, byte[] pdfContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Rapport Sellam : " + reportName);
            helper.setText(
                    "Bonjour,\n\n" +
                    "Veuillez trouver ci-joint le rapport " + reportName + " au format PDF.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe Sellam",
                    false
            );

            helper.addAttachment(reportName + ".pdf", new ByteArrayDataSource(pdfContent, "application/pdf"));

            mailSender.send(message);
            log.info("Report email sent successfully: {} to {}", reportName, toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send report email ({} to {}): {}", reportName, toEmail, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending report email ({} to {}): {}", reportName, toEmail, e.getMessage(), e);
        }
    }


    @Async
    public void sendSubscriptionTrialEndingEmail(String toEmail, String shopName)
    {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Votre essai gratuit se termine bientôt - " + shopName);
        message.setText(
                "Bonjour,\n\n" +
                        "L'essai gratuit de votre boutique " + shopName + " sur Sellam se termine bientôt.\n\n" +
                        "Abonnez-vous dès maintenant pour continuer à utiliser Sellam sans interruption :\n" +
                        frontendUrl + "/subscription\n\n" +
                        "L'équipe Sellam"
        );
        sendMailSafely(message, "subscription trial ending email for " + shopName + " to " + toEmail);
    }

    @Async
    public void sendSubscriptionPastDueEmail(String toEmail, String shopName)
    {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Abonnement expiré - " + shopName);
        message.setText(
                "Bonjour,\n\n" +
                        "L'abonnement de votre boutique " + shopName + " a expiré.\n\n" +
                        "Renouvelez-le rapidement pour éviter le blocage de votre accès :\n" +
                        frontendUrl + "/subscription\n\n" +
                        "L'équipe Sellam"
        );
        sendMailSafely(message, "subscription past due email for " + shopName + " to " + toEmail);
    }

    @Async
    public void sendSubscriptionExpiredEmail(String toEmail, String shopName)
    {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Accès bloqué - " + shopName);
        message.setText(
                "Bonjour,\n\n" +
                        "L'accès à votre boutique " + shopName + " sur Sellam est maintenant bloqué " +
                        "(essai ou abonnement expiré).\n\n" +
                        "Abonnez-vous pour rétablir l'accès immédiatement :\n" +
                        frontendUrl + "/subscription\n\n" +
                        "L'équipe Sellam"
        );
        sendMailSafely(message, "subscription expired email for " + shopName + " to " + toEmail);
    }

    @Async
    public void sendReferralRewardChoiceEmail(String toEmail, java.util.UUID rewardId)
    {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Récompense de parrainage disponible sur Sellam 🎉");
        message.setText(
                "Bonjour,\n\n" +
                        "Bonne nouvelle : un de vos filleuls vient de s'abonner à Sellam !\n\n" +
                        "Comme vous gérez plusieurs boutiques, choisissez celle qui recevra vos jours " +
                        "bonus en vous connectant à l'application :\n" +
                        frontendUrl + "/referrals\n\n" +
                        "L'équipe Sellam"
        );
        sendMailSafely(message, "referral reward choice email (reward " + rewardId + ") to " + toEmail);
    }
}