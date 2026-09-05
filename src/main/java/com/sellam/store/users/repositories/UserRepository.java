package com.sellam.store.users.repositories;

import com.sellam.store.identity.models.PersonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Maintained for backward compatibility. 
 * Use PersonRepository and ShopMembershipRepository for future logic.
 */
@Repository
public interface UserRepository extends JpaRepository<PersonEntity, UUID> {
    Optional<PersonEntity> findByPhoneNumber(String phoneNumber);
    Optional<PersonEntity> findByEmail(String email);
}
