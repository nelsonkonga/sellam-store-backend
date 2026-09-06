package com.sellam.store.referrals.controllers;

import com.sellam.store.common.security.AuthPrincipal;
import com.sellam.store.referrals.dto.ReferralDTO;
import com.sellam.store.referrals.services.ReferralService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/referrals")
public class ReferralRestController
{
    private final ReferralService referralService;

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public ReferralDTO.ReferralSummaryResponse getMySummary(Authentication authentication)
    {
        UUID personId = requirePersonId(authentication);
        return referralService.getSummary(personId);
    }

    @GetMapping("/rewards/{rewardId}/eligible-shops")
    @ResponseStatus(HttpStatus.OK)
    public List<ReferralDTO.EligibleShopOption> getEligibleShops(Authentication authentication)
    {
        UUID personId = requirePersonId(authentication);
        return referralService.getEligibleShopsForReward(personId);
    }

    @PostMapping("/rewards/{rewardId}/apply")
    @ResponseStatus(HttpStatus.OK)
    public void applyReward(
            @PathVariable UUID rewardId,
            @Valid @RequestBody ReferralDTO.ApplyRewardRequest request,
            Authentication authentication)
    {
        UUID personId = requirePersonId(authentication);
        referralService.applyPendingReward(personId, rewardId, request.getShopId());
    }

    private UUID requirePersonId(Authentication authentication)
    {
        if (authentication == null || authentication.getPrincipal() == null)
        {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié");
        }
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        return principal.getId();
    }
}
