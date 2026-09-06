package com.sellam.store.referrals.dto;

import com.sellam.store.referrals.models.ReferralRewardStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ReferralDTO
{
    /**
     * Vue "tableau de bord parrainage" d'une personne : son propre code,
     * le lien complet prêt à partager, et l'état de ses récompenses.
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReferralSummaryResponse
    {
        private String referralCode;
        private String referralLink;
        private int totalReferred;
        private int totalRewarded;
        private List<ReferralRewardResponse> pendingRewards;
        private List<ReferralRewardResponse> appliedRewards;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReferralRewardResponse
    {
        private UUID id;
        private String refereeName;
        private Integer bonusDays;
        private ReferralRewardStatusEnum status;
        private UUID appliedToShopId;
        private String appliedToShopName;
        private LocalDateTime appliedAt;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ApplyRewardRequest
    {
        private UUID shopId;
    }

    /**
     * Boutiques éligibles proposées au parrain quand une récompense est
     * PENDING et qu'il doit choisir (cf. onRefereeFirstPayment).
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EligibleShopOption
    {
        private UUID shopId;
        private String shopName;
    }
}
