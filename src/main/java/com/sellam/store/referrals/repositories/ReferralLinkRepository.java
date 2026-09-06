package com.sellam.store.referrals.repositories;

import com.sellam.store.referrals.models.ReferralLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReferralLinkRepository extends JpaRepository<ReferralLinkEntity, UUID>
{
    Optional<ReferralLinkEntity> findByRefereeId(UUID refereeId);
}
