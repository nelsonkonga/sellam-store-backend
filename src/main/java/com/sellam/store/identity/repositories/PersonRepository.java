package com.sellam.store.identity.repositories;

import com.sellam.store.identity.models.PersonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonRepository extends JpaRepository<PersonEntity, UUID>
{
    Optional<PersonEntity> findByPhoneNumber(String phoneNumber);

    Optional<PersonEntity> findByEmail(String email);

    Optional<PersonEntity> findByVerificationToken(String token);

    Optional<PersonEntity> findByResetToken(String token);

    Optional<PersonEntity> findByOauthProviderAndOauthId(String provider, String oauthId);

    Optional<PersonEntity> findByReferralCode(String referralCode);

    Boolean existsByReferralCode(String referralCode);
}
