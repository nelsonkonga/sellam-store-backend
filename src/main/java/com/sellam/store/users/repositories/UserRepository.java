package com.sellam.store.users.repositories;

import com.sellam.store.users.models.RoleEnum;
import com.sellam.store.users.models.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID>
{
    Optional<UserEntity> findByPhoneNumber(String phoneNumber);
    List<UserEntity> findByShop_Id(UUID shopId);

    @Query("SELECT u.shop.id FROM UserEntity u WHERE u.id = :userId")
    Optional<UUID> findShopIdByUserId(@Param("userId") UUID userId);

    // Pour le scheduler de rappels de bilan - trouve le propriétaire de la boutique
    List<UserEntity> findByShop_IdAndRoleOrderByCreatedAtAsc(UUID shopId, RoleEnum role);

}
