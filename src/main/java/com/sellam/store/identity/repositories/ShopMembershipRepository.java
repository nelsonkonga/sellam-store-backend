package com.sellam.store.identity.repositories;

import com.sellam.store.identity.models.ShopMembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShopMembershipRepository extends JpaRepository<ShopMembershipEntity, UUID>
{
    List<ShopMembershipEntity> findByPersonId(UUID personId);

    List<ShopMembershipEntity> findByShopId(UUID shopId);

    Optional<ShopMembershipEntity> findByPersonIdAndShopId(UUID personId, UUID shopId);

    @Query("SELECT sm.shop.id FROM ShopMembershipEntity sm WHERE sm.person.id = :personId AND sm.active = true")
    List<UUID> findActiveShopIdsByPersonId(@Param("personId") UUID personId);

    @Query("SELECT sm FROM ShopMembershipEntity sm WHERE sm.person.id = :personId AND sm.shop.id = :shopId AND sm.active = true")
    Optional<ShopMembershipEntity> findActiveMembership(@Param("personId") UUID personId, @Param("shopId") UUID shopId);

    @Query("SELECT COUNT(sm) FROM ShopMembershipEntity sm WHERE sm.shop.id = :shopId AND sm.active = true")
    long countActiveMembersByShopId(@Param("shopId") UUID shopId);
}
