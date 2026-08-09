package com.sellam.store.auth.services;

import com.sellam.store.accounts.models.AccountEntity;
import com.sellam.store.accounts.repositories.AccountRepository;
import com.sellam.store.users.models.UserEntity;
import com.sellam.store.users.repositories.UserRepository;
import com.sellam.store.auth.JwtProvider;
import com.sellam.store.auth.dto.AuthDTO;
import com.sellam.store.common.email.services.EmailService;
import com.sellam.store.common.exception.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthService(AccountRepository accountRepository,
                       UserRepository userRepository,
                       EmailService emailService,
                       PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public AuthDTO.AuthOutput register(AuthDTO.RegisterInput request) {

        if (accountRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new IllegalArgumentException("Ce numéro de téléphone est déjà utilisé");
        }

        AccountEntity.AccountEntityBuilder accountBuilder = AccountEntity.builder()
                .name(request.getName())
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()));

        String verificationToken = null;

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (accountRepository.findByEmail(request.getEmail()).isPresent()) {
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

        if (savedAccount.getEmail() != null) {
            emailService.sendVerificationEmail(savedAccount.getEmail(), verificationToken);
        }

        String token = jwtProvider.generateToken(savedAccount.getId(), "ACCOUNT", null, savedAccount.getPhoneNumber());

        return AuthDTO.AuthOutput.builder()
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

    public AuthDTO.AuthOutput login(AuthDTO.LoginInput request) {

        // 1. Chercher d'abord dans les comptes (gérants globaux)
        Optional<AccountEntity> optAccount = accountRepository.findByPhoneNumber(request.getPhoneNumber());
        if (optAccount.isPresent()) {
            AccountEntity account = optAccount.get();
            if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
                throw new IllegalArgumentException("Numéro ou mot de passe incorrect");
            }
            String token = jwtProvider.generateToken(account.getId(), "ACCOUNT", null, account.getPhoneNumber());
            return AuthDTO.AuthOutput.builder()
                    .token(token)
                    .accountId(account.getId().toString()) 
                    .userType("ACCOUNT")
                    .name(account.getName())
                    .phoneNumber(account.getPhoneNumber())
                    .email(account.getEmail())
                    .emailVerified(account.isEmailVerified())
                    .build();
        }

        // 2. Si pas trouvé, chercher dans les employés (Users)
        Optional<UserEntity> optUser = userRepository.findByPhoneNumber(request.getPhoneNumber());
        if (optUser.isPresent()) {
            UserEntity user = optUser.get();
            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                throw new IllegalArgumentException("Numéro ou mot de passe incorrect");
            }
            if (!user.isActive()) {
                throw new IllegalArgumentException("Ce compte employé a été désactivé");
            }
            String token = jwtProvider.generateToken(user.getId(), "USER", user.getShop().getId(), user.getPhoneNumber());
            return AuthDTO.AuthOutput.builder()
                    .token(token)
                    .accountId(user.getId().toString())
                    .userType("USER")
                    .shopId(user.getShop().getId().toString())
                    .name(user.getName())
                    .phoneNumber(user.getPhoneNumber())
                    .build();
        }

        throw new IllegalArgumentException("Numéro ou mot de passe incorrect");
    }
}