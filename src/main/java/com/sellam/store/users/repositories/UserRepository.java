package com.sellam.store.users.repositories;

import com.sellam.store.users.models.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID>
{
    Optional<UserEntity> findByPhoneNumber(String phoneNumber);
    List<UserEntity> findByShop_Id(UUID shopId);
}
