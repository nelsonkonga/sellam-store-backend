package com.sellam.store.identity.repositories;

import com.sellam.store.identity.models.LegacyEntityType;
import com.sellam.store.identity.models.LegacyIdMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LegacyIdMappingRepository extends JpaRepository<LegacyIdMapping, UUID>
{
    Optional<LegacyIdMapping> findByLegacyId(UUID legacyId);

    Optional<LegacyIdMapping> findByLegacyIdAndActiveTrue(UUID legacyId);

    Optional<LegacyIdMapping> findByPersonId(UUID personId);

    Optional<LegacyIdMapping> findByLegacyIdAndLegacyType(UUID legacyId, LegacyEntityType legacyType);
}
