package com.sellam.store.accounts.repositories;

import com.sellam.store.accounts.models.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, UUID>
{
    Optional<AccountEntity> findByPhoneNumber(String phoneNumber);

    Optional<AccountEntity> findByEmail(String email);

    Optional<AccountEntity> findByVerificationToken(String token);
}
