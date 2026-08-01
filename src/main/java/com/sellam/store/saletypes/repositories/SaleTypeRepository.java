package com.sellam.store.saletypes.repositories;

import com.sellam.store.saletypes.models.SaleTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SaleTypeRepository extends JpaRepository<SaleTypeEntity, UUID> {
    List<SaleTypeEntity> findByIsDefaultTrueOrShop_Id(UUID shopId);
}
