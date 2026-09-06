package com.sellam.store.auth.services;

import com.sellam.store.auth.JwtProvider;
import com.sellam.store.auth.dto.AuthDTO;
import com.sellam.store.common.email.services.EmailService;
import com.sellam.store.common.exception.ResourceNotFoundException;
import com.sellam.store.identity.models.LegacyEntityType;
import com.sellam.store.identity.models.PersonEntity;
import com.sellam.store.identity.repositories.LegacyIdMappingRepository;
import com.sellam.store.identity.repositories.PersonRepository;
import com.sellam.store.identity.repositories.ShopMembershipRepository;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.shops.repositories.ShopRepository;
import com.sellam.store.users.models.RoleEnum;
import com.sellam.store.referrals.services.ReferralService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



import java.util.Set;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service d'authentification adaptÃ© au nouveau modÃ¨le PersonEntity + ShopMembershipEntity.
 * Remplace l'ancien AuthService basÃ© sur AccountEntity/UserEntity.
 */
@Slf4j
@Service

@AllArgsConstructor
public class AuthService
{

    private final PersonRepository personRepository;
    private final ShopMembershipRepository shopMembershipRepository;
    private final ShopRepository shopRepository;
    private final LegacyIdMappingRepository legacyIdMappingRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final ReferralService referralService;

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

    private Optional<PersonEntity> findPersonByPhoneNumber(String phoneNumber)
    {
        for (String candidate : buildPhoneLookupCandidates(phoneNumber))
        {
            Optional<PersonEntity> person = personRepository.findByPhoneNumber(candidate);
            if (person.isPresent())
            {
                return person;
            }
        }
        return Optional.empty();
    }

    private Optional<PersonEntity> findPersonByEmail(String email)
    {
        if (email == null || email.isBlank())
        {
            return Optional.empty();
        }
        return personRepository.findByEmail(email.trim());
    }

    @Transactional
    public AuthDTO.AuthResponse register(AuthDTO.RegisterRequest request)
    {
        // Validation : au moins phone ou email requis
        if ((request.getPhoneNumber() == null || request.getPhoneNumber().isBlank())
                && (request.getEmail() == null || request.getEmail().isBlank()))
        {
            throw new IllegalArgumentException("Au moins un numÃ©ro de tÃ©lÃ©phone ou un email est requis");
        }

        // Validation du mot de passe (8+ caractÃ¨res, majuscule, minuscule, chiffre, spÃ©cial)
        if (!isPasswordValid(request.getPassword()))
        {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 8 caractÃ¨res, une majuscule, une minuscule, un chiffre et un caractÃ¨re spÃ©cial");
        }

        // VÃ©rifier les doublons
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank())
        {
            if (findPersonByPhoneNumber(request.getPhoneNumber()).isPresent())
            {
                throw new IllegalArgumentException("Ce numÃ©ro de tÃ©lÃ©phone est dÃ©jÃ  utilisÃ©");
            }
        }

        if (request.getEmail() != null && !request.getEmail().isBlank())
        {
            if (findPersonByEmail(request.getEmail()).isPresent())
            {
                throw new IllegalArgumentException("Cet email est dÃ©jÃ  utilisÃ©");
            }
        }

        // CrÃ©er la personne
        PersonEntity.PersonEntityBuilder personBuilder = PersonEntity.builder()
                .name(request.getName())
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()));

        String verificationToken = null;

        if (request.getEmail() != null && !request.getEmail().isBlank())
        {
            verificationToken = UUID.randomUUID().toString();

            personBuilder
                    .email(request.getEmail())
                    .emailVerified(false)
                    .verificationToken(verificationToken)
                    .verificationTokenExpiresAt(LocalDateTime.now().plusHours(24));
        }

        PersonEntity person = personBuilder.build();
        PersonEntity savedPerson = personRepository.save(person);

        if (request.getReferralCode() != null && !request.getReferralCode().isBlank())
        {
            referralService.linkReferral(savedPerson.getId(), request.getReferralCode());
        }

        // CrÃ©er le mapping legacy (pour compatibilitÃ©)
        // Comme c'est un nouveau compte, on le mappe comme ACCOUNT par dÃ©faut
        com.sellam.store.identity.models.LegacyIdMapping mapping =
                com.sellam.store.identity.models.LegacyIdMapping.builder()
                        .legacyId(savedPerson.getId())
                        .personId(savedPerson.getId())
                        .legacyType(LegacyEntityType.ACCOUNT)
                        .createdAt(LocalDateTime.now())
                        .build();
        legacyIdMappingRepository.save(mapping);

        if (savedPerson.getEmail() != null)
        {
            emailService.sendVerificationEmail(savedPerson.getEmail(), verificationToken);
        }

        // Génère le token avec userType "ACCOUNT" (et non "PERSON") : c'est ce
        // claim, pas la réponse JSON, que @sec.isAccountOwner lit sur chaque
        // requête ultérieure. Un token "PERSON" faisait échouer createShop
        // (403 Access Denied) pour tout compte fraîchement inscrit, alors que
        // la réponse ci-dessous annonçait "ACCOUNT" au frontend.
        String token = jwtProvider.generateToken(savedPerson.getId(), savedPerson.getName(), "ACCOUNT", null, savedPerson.getPhoneNumber());

        return AuthDTO.AuthResponse.builder()
                .token(token)
                .accountId(savedPerson.getId().toString()) // Compatible frontend
                .userType("ACCOUNT") // Ancien type pour compatibilitÃ©
                .name(savedPerson.getName())
                .phoneNumber(savedPerson.getPhoneNumber())
                .email(savedPerson.getEmail())
                .emailVerified(savedPerson.isEmailVerified())
                .build();
    }

    private boolean isPasswordValid(String password)
    {
        if (password == null || password.length() < 8)
        {
            return false;
        }
        boolean hasUpper = !password.equals(password.toLowerCase());
        boolean hasLower = !password.equals(password.toUpperCase());
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = !password.matches("[A-Za-z0-9]*");
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    public void verifyEmail(String token)
    {
        PersonEntity person = personRepository.findByVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Lien de vÃ©rification invalide"));

        if (person.getVerificationTokenExpiresAt().isBefore(LocalDateTime.now()))
        {
            throw new IllegalArgumentException("Ce lien a expirÃ©, demandez-en un nouveau");
        }

        person.setEmailVerified(true);
        person.setVerificationToken(null);
        person.setVerificationTokenExpiresAt(null);
        personRepository.save(person);
    }

    public void resendVerificationEmail(UUID personId)
    {
        PersonEntity person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Personne introuvable"));

        if (person.getEmail() == null)
        {
            throw new IllegalArgumentException("Aucun email associÃ© Ã  ce compte");
        }
        if (person.isEmailVerified())
        {
            throw new IllegalArgumentException("Cet email est dÃ©jÃ  vÃ©rifiÃ©");
        }

        String newToken = UUID.randomUUID().toString();
        person.setVerificationToken(newToken);
        person.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24));
        personRepository.save(person);
        emailService.sendVerificationEmail(person.getEmail(), newToken);
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

        // Recherche de la personne par phone ou email
        Optional<PersonEntity> optPerson = identifier.contains("@")
                ? findPersonByEmail(identifier)
                : findPersonByPhoneNumber(identifier);

        if (optPerson.isEmpty())
        {
            log.warn("Login failed: no person found with identifier: {}", identifier);
            throw new IllegalArgumentException("Identifiant ou mot de passe incorrect");
        }

        PersonEntity person = optPerson.get();

        // VÃ©rifier le mot de passe
        if (person.getPasswordHash() == null || person.getPasswordHash().isBlank())
        {
            log.warn("Login failed for person {}: no password hash set", person.getId());
            throw new IllegalArgumentException("Identifiant ou mot de passe incorrect");
        }

        if (!passwordEncoder.matches(password, person.getPasswordHash()))
        {
            log.warn("Login failed for person {}: password mismatch", person.getId());
            throw new IllegalArgumentException("Identifiant ou mot de passe incorrect");
        }

        log.info("Login successful: {}", person.getId());

        // Détermine le userType de façon durable via LegacyIdMapping.legacyType
        // quand ce mapping existe (créé par AuthService.register pour un
        // propriétaire ACCOUNT). UserService.createUser (création d'employé)
        // n'écrit pas ce mapping : dans ce cas on retombe sur la présence de
        // memberships pour distinguer un employé (USER) d'un compte hérité
        // sans mapping du tout (ACCOUNT, cas legacy pré-migration).
        //
        // Important : on ne déduit plus "a des memberships" => USER pour un
        // ACCOUNT qui a un mapping explicite. Un propriétaire obtient
        // automatiquement une ShopMembershipEntity (rôle MANAGER) dès qu'il
        // crée sa première boutique ; le déduire uniquement des memberships
        // le reclassait à tort en USER dès sa deuxième connexion, bloquant
        // ensuite createShop (protégé par @sec.isAccountOwner) avec un 403.
        Optional<LegacyEntityType> mappedType = legacyIdMappingRepository.findByPersonId(person.getId())
                .map(com.sellam.store.identity.models.LegacyIdMapping::getLegacyType);

        var memberships = shopMembershipRepository.findByPersonId(person.getId());

        LegacyEntityType legacyType = mappedType.orElseGet(() ->
                memberships.isEmpty() ? LegacyEntityType.ACCOUNT : LegacyEntityType.USER
        );

        String userType = legacyType.name(); // "ACCOUNT" ou "USER"
        UUID shopId = null;

        if (legacyType == LegacyEntityType.USER)
        {
            // Pour un employé, on résout un shopId par défaut pour compatibilité
            // avec le frontend actuel (ex: pré-remplissage), mais ce shopId
            // n'est plus la source de vérité pour l'accès boutique côté backend
            // (voir ShopAccessGuard / ShopMembershipRepository.findActiveMembership).
            shopId = memberships.stream()
                    .filter(m -> m.isActive())
                    .findFirst()
                    .map(m -> m.getShop().getId())
                    .orElse(null);
        }

        // Génère le token
        String token = jwtProvider.generateToken(person.getId(), person.getName(), userType, shopId, person.getPhoneNumber());

        return AuthDTO.AuthResponse.builder()
                .token(token)
                .accountId(person.getId().toString())
                .userType(userType)
                .shopId(shopId != null ? shopId.toString() : null)
                .name(person.getName())
                .phoneNumber(person.getPhoneNumber())
                .email(person.getEmail())
                .emailVerified(person.isEmailVerified())
                .build();
    }

    @Transactional
    public AuthDTO.ForgotPasswordResponse forgotPassword(AuthDTO.ForgotPasswordRequest request)
    {
        PersonEntity person = findPersonByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new IllegalArgumentException("Aucun compte trouvÃ© avec ce numÃ©ro de tÃ©lÃ©phone"));

        String resetToken = UUID.randomUUID().toString();
        person.setResetToken(resetToken);
        person.setResetTokenExpiresAt(LocalDateTime.now().plusHours(1));
        personRepository.save(person);

        if (person.getEmail() != null && !person.getEmail().isBlank())
        {
            emailService.sendPasswordResetEmail(person.getEmail(), resetToken);
        }

        return AuthDTO.ForgotPasswordResponse.builder()
                .message("Un lien de rÃ©initialisation a Ã©tÃ© envoyÃ© Ã  votre email. Si vous n'avez pas d'email, veuillez contacter l'administrateur.")
                .build();
    }

    @Transactional
    public AuthDTO.ResetPasswordResponse resetPassword(AuthDTO.ResetPasswordRequest request)
    {
        PersonEntity person = personRepository.findByResetToken(request.getResetToken())
                .orElseThrow(() -> new IllegalArgumentException("Lien de rÃ©initialisation invalide"));

        if (person.getResetTokenExpiresAt().isBefore(LocalDateTime.now()))
        {
            throw new IllegalArgumentException("Ce lien a expirÃ©, demandez-en un nouveau");
        }

        person.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        person.setResetToken(null);
        person.setResetTokenExpiresAt(null);
        personRepository.save(person);

        return AuthDTO.ResetPasswordResponse.builder()
                .message("Votre mot de passe a Ã©tÃ© rÃ©initialisÃ© avec succÃ¨s. Vous pouvez maintenant vous connecter.")
                .build();
    }
}