package com.sellam.store.auth.services;

import com.sellam.store.accounts.models.AccountEntity;
import com.sellam.store.accounts.repositories.AccountRepository;
import com.sellam.store.auth.JwtProvider;
import com.sellam.store.auth.dto.AuthDTO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthService(AccountRepository accountRepository,
                       PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public AuthDTO.AuthOutput register(AuthDTO.RegisterInput request) {

        if (accountRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new IllegalArgumentException("Ce numéro de téléphone est déjà utilisé");
        }

        AccountEntity account = AccountEntity.builder()
                .name(request.getName())
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();


        AccountEntity savedAccount = accountRepository.save(account);

        String token = jwtProvider.generateToken(savedAccount.getId(), savedAccount.getPhoneNumber());

        return new AuthDTO.AuthOutput(token, savedAccount.getId().toString(), savedAccount.getName());
    }

    public AuthDTO.AuthOutput login(AuthDTO.LoginInput request) {

        AccountEntity account = accountRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new IllegalArgumentException("Numéro ou mot de passe incorrect"));

        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new IllegalArgumentException("Numéro ou mot de passe incorrect");
        }

        String token = jwtProvider.generateToken(account.getId(), account.getPhoneNumber());

        return new AuthDTO.AuthOutput(token, account.getId().toString(), account.getName());
    }
}