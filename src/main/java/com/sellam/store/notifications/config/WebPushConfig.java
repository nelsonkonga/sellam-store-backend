package com.sellam.store.notifications.config;

import nl.martijndwars.webpush.PushService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.GeneralSecurityException;
import java.security.Security;

@Configuration
@Profile("!test")
public class WebPushConfig {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Bean
    public PushService pushService(
            @Value("${app.vapid.public-key:}") String publicKey,
            @Value("${app.vapid.private-key:}") String privateKey,
            @Value("${app.vapid.subject:http://localhost:5173}") String subject
    ) throws GeneralSecurityException {
        if (publicKey != null && publicKey.trim().isEmpty()) publicKey = null;
        if (privateKey != null && privateKey.trim().isEmpty()) privateKey = null;
        if (subject != null && subject.trim().isEmpty()) subject = null;
        return new PushService(publicKey, privateKey, subject);
    }
}
