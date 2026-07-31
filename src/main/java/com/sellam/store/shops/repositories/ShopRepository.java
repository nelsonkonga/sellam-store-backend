package com.sellam.store.shops.repositories;

import com.sellam.store.shops.models.ShopEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShopRepository extends JpaRepository<ShopEntity, UUID>
{
    List<ShopEntity> findByAccount_Id(UUID accountId);
}
