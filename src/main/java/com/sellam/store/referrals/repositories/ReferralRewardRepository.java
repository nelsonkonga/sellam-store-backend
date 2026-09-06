package com.sellam.store.referrals.repositories;

import com.sellam.store.referrals.models.ReferralRewardEntity;
import com.sellam.store.referrals.models.ReferralRewardStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReferralRewardRepository extends JpaRepository<ReferralRewardEntity, UUID>
{
    List<ReferralRewardEntity> findByRefereeId(UUID refereeId);

    List<ReferralRewardEntity> findByReferrerIdAndStatus(UUID referrerId, ReferralRewardStatusEnum status);
}
