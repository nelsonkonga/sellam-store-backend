package com.sellam.store.auth.services;

import com.sellam.store.accounts.models.AccountEntity;
import com.sellam.store.accounts.repositories.AccountRepository;
import com.sellam.store.users.models.UserEntity;
import com.sellam.store.users.repositories.UserRepository;
import com.sellam.store.auth.JwtProvider;
import com.sellam.store.auth.dto.AuthDTO;
import com.sellam.store.common.email.services.EmailService;
import com.sellam.store.common.exception.ResourceNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@Profile("!phase1")
@AllArgsConstructor
public class AuthService
{

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    private List<String> buildPhoneLookupCandidates(String phoneNumber)
    {
        Set<String> candidates = new LinkedHashSet<>();
        if (phoneNumber == null)
        {
            return new ArrayList<>();
        }

        String trimmed = phoneNumber.trim();
        if (trimmed.isBlank())
        {
            return new ArrayList<>();
        }

        candidates.add(trimmed);

        String withoutPlus = trimmed.startsWith("+") ? trimmed.substring(1) : trimmed;
        if (!withoutPlus.equals(trimmed))
        {
            candidates.add(withoutPlus);
        }

        String withoutInternationalPrefix = trimmed.startsWith("00") ? trimmed.substring(2) : trimmed;
        if (!withoutInternationalPrefix.equals(trimmed))
        {
            candidates.add(withoutInternationalPrefix);
        }

        String digitsOnly = trimmed.replaceAll("\\D+", "");
        if (!digitsOnly.isEmpty())
        {
            candidates.add(digitsOnly);
            if (digitsOnly.length() > 3)
            {
                candidates.add(digitsOnly.substring(3));
            }
            if (digitsOnly.length() > 9)
            {
                candidates.add(digitsOnly.substring(digitsOnly.length() - 9));
            }
        }

        return new ArrayList<>(candidates);
    }

    private Optional<AccountEntity> findAccountByPhoneNumber(String phoneNumber)
    {
        for (String candidate : buildPhoneLookupCandidates(phoneNumber))
        {
            Optional<AccountEntity> account = accountRepository.findByPhoneNumber(candidate);
            if (account.isPresent())
            {
                return account;
            }
        }
        return Optional.empty();
    }

    private Optional<UserEntity> findUserByPhoneNumber(String phoneNumber)
    {
        for (String candidate : buildPhoneLookupCandidates(phoneNumber))
        {
            Optional<UserEntity> user = userRepository.findByPhoneNumber(candidate);
            if (user.isPresent())
            {
                return user;
            }
        }
        return Optional.empty();
    }

    private Optional<UserEntity> findUserByEmail(String email)
    {
        if (email == null || email.isBlank())
        {
            return Optional.empty();
        }
        return userRepository.findByEmail(email.trim());
    }

    @Transactional
    public AuthDTO.AuthResponse register(AuthDTO.RegisterRequest request)
    {

        if (findAccountByPhoneNumber(request.getPhoneNumber()).isPresent())
        {
            throw new IllegalArgumentException("Ce numéro de téléphone est déjà utilisé");
        }

        AccountEntity.AccountEntityBuilder accountBuilder = AccountEntity.builder()
                .name(request.getName())
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()));

        String verificationToken = null;

        if (request.getEmail() != null && !request.getEmail().isBlank())
        {
            if (accountRepository.findByEmail(request.getEmail()).isPresent())
            {
                throw new IllegalArgumentException("Cet email est déjà utilisé");
            }

            verificationToken = UUID.randomUUID().toString();

            accountBuilder
                    .email(request.getEmail())
                    .emailVerified(false)
                    .verificationToken(verificationToken)
                    .verificationTokenExpiresAt(LocalDateTime.now().plusHours(24));
        }

        AccountEntity account = accountBuilder.build();
        AccountEntity savedAccount = accountRepository.save(account);

        if (savedAccount.getEmail() != null)
        {
            emailService.sendVerificationEmail(savedAccount.getEmail(), verificationToken);
        }

        String token = jwtProvider.generateToken(savedAccount.getId(), "ACCOUNT", null, savedAccount.getPhoneNumber());

        return AuthDTO.AuthResponse.builder()
                .token(token)
                .accountId(savedAccount.getId().toString())
                .userType("ACCOUNT")
                .name(savedAccount.getName())
                .phoneNumber(savedAccount.getPhoneNumber())
                .email(savedAccount.getEmail())
                .emailVerified(savedAccount.isEmailVerified())
                .build();
    }

    public void verifyEmail(String token)
    {
        AccountEntity account = accountRepository.findByVerificationToken(token)
                                                 .orElseThrow(() -> new IllegalArgumentException("Lien de vérification invalide"));

        if (account.getVerificationTokenExpiresAt().isBefore(LocalDateTime.now()))
        {
            throw new IllegalArgumentException("Ce lien a expiré, demandez-en un nouveau");
        }

        account.setEmailVerified(true);
        account.setVerificationToken(null);
        account.setVerificationTokenExpiresAt(null);
        accountRepository.save(account);
    }

    public void resendVerificationEmail(UUID accountId)
    {
        AccountEntity account = accountRepository.findById(accountId)
                                                 .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable"));

        if (account.getEmail() == null)
        {
            throw new IllegalArgumentException("Aucun email associé à ce compte");
        }
        if (account.isEmailVerified())
        {
            throw new IllegalArgumentException("Cet email est déjà vérifié");
        }

        String newToken = UUID.randomUUID().toString();
        account.setVerificationToken(newToken);
        account.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24));
        accountRepository.save(account);
        emailService.sendVerificationEmail(account.getEmail(), newToken);
    }

    public AuthDTO.AuthResponse login(AuthDTO.LoginRequest request)
    {
        String identifier = request.getIdentifier() == null ? "" : request.getIdentifier().trim();
        String password = request.getPassword() == null ? "" : request.getPassword();
        
        log.debug("Login attempt with identifier: {} (type: {})", 
                identifier.isEmpty() ? "EMPTY" : (identifier.contains("@") ? "EMAIL" : "PHONE"), identifier);
        
        if (identifier.isBlank() || password.isBlank())
        {
            log.warn("Login failed: missing identifier or password");
            throw new IllegalArgumentException("L'identifiant et le mot de passe sont requis");
        }

        // Try Account login
        Optional<AccountEntity> optAccount = identifier.contains("@")
                ? accountRepository.findByEmail(identifier)
                : findAccountByPhoneNumber(identifier);

        if (optAccount.isPresent()) {
            AccountEntity account = optAccount.get();
            
            // Check if password hash exists
            if (account.getPasswordHash() == null || account.getPasswordHash().isBlank()) {
                log.warn("Login failed for account {}: no password hash set", account.getId());
                throw new IllegalArgumentException("Identifiant ou mot de passe incorrect");
            }
            
            if (!passwordEncoder.matches(password, account.getPasswordHash()))
            {
                log.warn("Login failed for account {}: password mismatch", account.getId());
                throw new IllegalArgumentException("Identifiant ou mot de passe incorrect");
            }
            
            log.info("Account login successful: {}", account.getId());
            String token = jwtProvider.generateToken(account.getId(), "ACCOUNT", null, account.getPhoneNumber());
            return AuthDTO.AuthResponse.builder()
                    .token(token)
                    .accountId(account.getId().toString())
                    .userType("ACCOUNT")
                    .name(account.getName())
                    .phoneNumber(account.getPhoneNumber())
                    .email(account.getEmail())
                    .emailVerified(account.isEmailVerified())
                    .build();
        }

        // Try User/Employee login
        Optional<UserEntity> optUser = identifier.contains("@")
                ? findUserByEmail(identifier)
                : findUserByPhoneNumber(identifier);

        if (optUser.isPresent())
        {
            UserEntity user = optUser.get();
            
            // Check if password hash exists
            if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
                log.warn("Login failed for user {}: no password hash set", user.getId());
                throw new IllegalArgumentException("Identifiant ou mot de passe incorrect");
            }
            
            if (!passwordEncoder.matches(password, user.getPasswordHash()))
            {
                log.warn("Login failed for user {}: password mismatch", user.getId());
                throw new IllegalArgumentException("Identifiant ou mot de passe incorrect");
            }
            if (!user.isActive())
            {
                log.warn("Login failed for user {}: account disabled", user.getId());
                throw new IllegalArgumentException("Ce compte employé a été désactivé");
            }
            
            log.info("User login successful: {}", user.getId());
            String token = jwtProvider.generateToken(user.getId(), "USER", user.getShop().getId(), user.getPhoneNumber());
            return AuthDTO.AuthResponse.builder()
                    .token(token)
                    .accountId(user.getId().toString())
                    .userType("USER")
                    .shopId(user.getShop().getId().toString())
                    .name(user.getName())
                    .phoneNumber(user.getPhoneNumber())
                    .email(user.getEmail())
                    .build();
        }

        log.warn("Login failed: no account or user found with identifier: {}", identifier);
        throw new IllegalArgumentException("Identifiant ou mot de passe incorrect");
    }

    @Transactional
    public AuthDTO.ForgotPasswordResponse forgotPassword(AuthDTO.ForgotPasswordRequest request)
    {
        AccountEntity account = findAccountByPhoneNumber(request.getPhoneNumber())
                                                 .orElseThrow(() -> new IllegalArgumentException("Aucun compte trouvé avec ce numéro de téléphone"));

        String resetToken = UUID.randomUUID().toString();
        account.setResetToken(resetToken);
        account.setResetTokenExpiresAt(LocalDateTime.now().plusHours(1)); // Token valide 1 heure
        accountRepository.save(account);

        // Envoyer l'email avec le lien de reset (ou afficher le token directement en dev)
        if (account.getEmail() != null && !account.getEmail().isBlank())
        {
            emailService.sendPasswordResetEmail(account.getEmail(), resetToken);
        }

        return AuthDTO.ForgotPasswordResponse.builder()
                .message("Un lien de réinitialisation a été envoyé à votre email. Si vous n'avez pas d'email, veuillez contacter l'administrateur.")
                .build();
    }

    @Transactional
    public AuthDTO.ResetPasswordResponse resetPassword(AuthDTO.ResetPasswordRequest request)
    {
        AccountEntity account = accountRepository.findByResetToken(request.getResetToken())
                                                 .orElseThrow(() -> new IllegalArgumentException("Lien de réinitialisation invalide"));

        if (account.getResetTokenExpiresAt().isBefore(LocalDateTime.now()))
        {
            throw new IllegalArgumentException("Ce lien a expiré, demandez-en un nouveau");
        }

        account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        account.setResetToken(null);
        account.setResetTokenExpiresAt(null);
        accountRepository.save(account);

        return AuthDTO.ResetPasswordResponse.builder()
                .message("Votre mot de passe a été réinitialisé avec succès. Vous pouvez maintenant vous connecter.")
                .build();
    }
}