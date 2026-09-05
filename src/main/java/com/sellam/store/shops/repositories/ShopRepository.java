package com.sellam.store.shops.repositories;

import com.sellam.store.shops.models.ShopEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ShopRepository extends JpaRepository<ShopEntity, UUID>
{
}