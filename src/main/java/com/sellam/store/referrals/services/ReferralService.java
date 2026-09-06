package com.sellam.store.referrals.services;

import com.sellam.store.common.exception.ResourceNotFoundException;
import com.sellam.store.identity.models.PersonEntity;
import com.sellam.store.identity.models.ShopMembershipEntity;
import com.sellam.store.identity.repositories.PersonRepository;
import com.sellam.store.identity.repositories.ShopMembershipRepository;
import com.sellam.store.common.email.services.EmailService;
import com.sellam.store.notifications.services.PushNotificationService;
import com.sellam.store.referrals.dto.ReferralDTO;
import com.sellam.store.referrals.models.ReferralLinkEntity;
import com.sellam.store.referrals.models.ReferralRewardEntity;
import com.sellam.store.referrals.models.ReferralRewardStatusEnum;
import com.sellam.store.referrals.repositories.ReferralLinkRepository;
import com.sellam.store.referrals.repositories.ReferralRewardRepository;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.shops.repositories.ShopRepository;
import com.sellam.store.subscriptions.services.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Centralise toute la logique de parrainage :
 *
 * - Génération du code unique par personne (généré paresseusement, au
 *   premier besoin, plutôt qu'obligatoirement à l'inscription : évite
 *   d'imposer une contrainte de génération/retry dans le chemin critique
 *   de register()).
 * - Rattachement d'un filleul à un parrain à l'inscription (register()) :
 *   priorité au code capturé via ?ref=CODE, sinon celui saisi manuellement
 *   (cf. décision produit "les deux, priorité à l'URL").
 * - Déclenchement de la récompense au premier paiement du filleul.
 * - Choix de la boutique bénéficiaire : automatique si le parrain n'a
 *   qu'une boutique, sinon récompense laissée PENDING + notification.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralService
{
    private final PersonRepository personRepository;
    private final ShopMembershipRepository shopMembershipRepository;
    private final ShopRepository shopRepository;
    private final ReferralLinkRepository referralLinkRepository;
    private final ReferralRewardRepository referralRewardRepository;
    private final SubscriptionService subscriptionService;
    private final PushNotificationService pushNotificationService;
    private final EmailService emailService;

    @Value("${app.referral.bonus-days-referrer:15}")
    private int bonusDaysReferrer;

    @Value("${app.referral.bonus-days-referee:15}")
    private int bonusDaysReferee;

    @Value("${app.frontend-url:https://app.sellam.store}")
    private String frontendUrl;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sans caractères ambigus (0/O, 1/I/L)

    /**
     * Génère (si absent) et renvoie le code de parrainage d'une personne.
     * Idempotent : appeler plusieurs fois renvoie toujours le même code
     * une fois généré.
     */
    @Transactional
    public String getOrCreateReferralCode(UUID personId)
    {
        PersonEntity person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Personne introuvable"));

        if (person.getReferralCode() != null && !person.getReferralCode().isBlank())
        {
            return person.getReferralCode();
        }

        String code;
        int attempts = 0;
        do
        {
            code = generateCode();
            attempts++;
            if (attempts > 10)
            {
                // Extrêmement improbable avec 8 caractères sur un alphabet
                // de 32 (32^8 combinaisons), gardé par robustesse défensive.
                throw new IllegalStateException("Impossible de générer un code de parrainage unique");
            }
        }
        while (personRepository.existsByReferralCode(code));

        person.setReferralCode(code);
        personRepository.save(person);
        return code;
    }

    private String generateCode()
    {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++)
        {
            sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    /**
     * À appeler depuis AuthService.register() juste après la création du
     * PersonEntity du filleul, si un code de parrainage a été fourni
     * (query param ?ref=CODE en priorité, sinon champ saisi manuellement
     * — la résolution de priorité est faite par l'appelant, ce service ne
     * reçoit que le code final retenu).
     *
     * Ne lève jamais d'exception bloquante : un code invalide/inconnu, ou
     * une tentative d'auto-parrainage, sont silencieusement ignorés pour
     * ne jamais faire échouer une inscription à cause du parrainage.
     */
    @Transactional
    public void linkReferral(UUID refereeId, String referralCode)
    {
        if (referralCode == null || referralCode.isBlank())
        {
            return;
        }

        Optional<PersonEntity> referrerOpt = personRepository.findByReferralCode(referralCode.trim().toUpperCase());
        if (referrerOpt.isEmpty())
        {
            log.info("Code de parrainage inconnu utilisé à l'inscription : {}", referralCode);
            return;
        }

        PersonEntity referrer = referrerOpt.get();
        if (referrer.getId().equals(refereeId))
        {
            log.warn("Tentative d'auto-parrainage détectée pour la personne {}", refereeId);
            return;
        }

        if (referralLinkRepository.findByRefereeId(refereeId).isPresent())
        {
            // Ne devrait pas arriver (un seul appel à l'inscription), mais
            // protège contre un double appel accidentel.
            return;
        }

        PersonEntity referee = personRepository.findById(refereeId)
                .orElseThrow(() -> new ResourceNotFoundException("Personne introuvable"));

        ReferralLinkEntity link = ReferralLinkEntity.builder()
                .referrer(referrer)
                .referee(referee)
                .build();
        referralLinkRepository.save(link);

        log.info("Filleul {} rattaché au parrain {} via le code {}", refereeId, referrer.getId(), referralCode);
    }

    /**
     * Appelé par PaymentService quand un filleul effectue son tout premier
     * paiement confirmé (isFirstPayment == true). Ne fait rien si le
     * payeur n'a pas de parrain enregistré.
     */
    @Transactional
    public void onRefereeFirstPayment(UUID shopIdJustPaid)
    {
        // Le payeur est identifié via la boutique qui vient de payer : on
        // remonte à son (ses) membre(s), puis à leur éventuel parrainage.
        List<ShopMembershipEntity> memberships = shopMembershipRepository.findByShopId(shopIdJustPaid);
        for (ShopMembershipEntity membership : memberships)
        {
            UUID refereeId = membership.getPerson().getId();
            Optional<ReferralLinkEntity> linkOpt = referralLinkRepository.findByRefereeId(refereeId);
            if (linkOpt.isEmpty())
            {
                continue;
            }

            ReferralLinkEntity link = linkOpt.get();

            // Idempotence : si une récompense existe déjà pour ce filleul,
            // ne pas en recréer une seconde au paiement suivant.
            if (!referralRewardRepository.findByRefereeId(refereeId).isEmpty())
            {
                continue;
            }

            // Récompense du filleul : appliquée immédiatement à la boutique
            // qui vient de payer, aucune ambiguïté possible ici puisqu'on
            // sait exactement quelle boutique est concernée.
            subscriptionService.grantBonusDays(shopIdJustPaid, bonusDaysReferee);

            // Récompense du parrain : à choisir/appliquer selon son nombre
            // de boutiques (cf. règle produit).
            ReferralRewardEntity reward = ReferralRewardEntity.builder()
                    .referrer(link.getReferrer())
                    .referee(link.getReferee())
                    .bonusDays(bonusDaysReferrer)
                    .status(ReferralRewardStatusEnum.PENDING)
                    .build();
            referralRewardRepository.save(reward);

            resolveRewardApplication(reward);
        }
    }

    /**
     * Applique automatiquement la récompense si le parrain n'a qu'une
     * seule boutique active ; sinon la laisse PENDING et notifie le
     * parrain qu'il doit choisir.
     */
    private void resolveRewardApplication(ReferralRewardEntity reward)
    {
        UUID referrerId = reward.getReferrer().getId();
        List<ShopMembershipEntity> referrerMemberships = shopMembershipRepository.findByPersonId(referrerId)
                .stream()
                .filter(ShopMembershipEntity::isActive)
                .collect(Collectors.toList());

        if (referrerMemberships.size() == 1)
        {
            ShopEntity onlyShop = referrerMemberships.get(0).getShop();
            applyReward(reward, onlyShop.getId());
            return;
        }

        // Zéro boutique (cas limite improbable, ex. compte désactivé) ou
        // plusieurs boutiques : dans les deux cas on laisse PENDING et on
        // notifie, le parrain choisira depuis l'appli (ou contactera le
        // support si zéro boutique, cas qui ne devrait pas se produire en
        // pratique puisqu'il faut une boutique pour avoir pu parrainer).
        //
        // Notification : PushNotificationService n'a qu'un sendToShop(shopId,...)
        // (pas de notion de "par personne"), et n'est actif que sous le
        // profil !dev & !postgres (donc probablement inactif tel quel avec
        // Supabase/Postgres en prod, cf. @Profile sur la classe) — on
        // notifie donc chaque boutique du parrain par push ET on envoie un
        // email, qui reste le canal fiable garanti actif.
        for (ShopMembershipEntity membership : referrerMemberships)
        {
            try
            {
                pushNotificationService.sendToShop(
                        membership.getShop().getId(),
                        "Récompense de parrainage disponible 🎉",
                        "Un de vos filleuls s'est abonné ! Choisissez la boutique qui recevra vos jours bonus."
                );
            }
            catch (Exception e)
            {
                log.error("Erreur lors de la notification push de récompense de parrainage (boutique {})",
                        membership.getShop().getId(), e);
            }
        }

        String referrerEmail = reward.getReferrer().getEmail();
        if (referrerEmail != null && !referrerEmail.isBlank())
        {
            try
            {
                emailService.sendReferralRewardChoiceEmail(referrerEmail, reward.getId());
            }
            catch (Exception e)
            {
                log.error("Erreur lors de l'envoi de l'email de récompense de parrainage à {}", referrerEmail, e);
            }
        }
    }

    /**
     * Applique explicitement une récompense PENDING à une boutique donnée.
     * Utilisé automatiquement (boutique unique) ou via l'endpoint de choix
     * manuel du parrain.
     */
    @Transactional
    public void applyReward(ReferralRewardEntity reward, UUID shopId)
    {
        ShopEntity shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique introuvable"));

        subscriptionService.grantBonusDays(shopId, reward.getBonusDays());

        reward.setStatus(ReferralRewardStatusEnum.APPLIED);
        reward.setAppliedAt(LocalDateTime.now());
        reward.setAppliedToShop(shop);
        referralRewardRepository.save(reward);
    }

    /**
     * Endpoint public : le parrain choisit explicitement la boutique
     * bénéficiaire d'une récompense PENDING.
     */
    @Transactional
    public void applyPendingReward(UUID referrerId, UUID rewardId, UUID chosenShopId)
    {
        ReferralRewardEntity reward = referralRewardRepository.findById(rewardId)
                .orElseThrow(() -> new ResourceNotFoundException("Récompense introuvable"));

        if (!reward.getReferrer().getId().equals(referrerId))
        {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette récompense ne vous appartient pas");
        }

        if (reward.getStatus() != ReferralRewardStatusEnum.PENDING)
        {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette récompense a déjà été appliquée");
        }

        // Vérifie que la boutique choisie appartient bien au parrain, pour
        // empêcher de créditer une boutique tierce.
        boolean ownsShop = shopMembershipRepository.findByPersonId(referrerId).stream()
                .anyMatch(m -> m.isActive() && m.getShop().getId().equals(chosenShopId));
        if (!ownsShop)
        {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette boutique ne vous appartient pas");
        }

        applyReward(reward, chosenShopId);
    }

    public ReferralDTO.ReferralSummaryResponse getSummary(UUID personId)
    {
        String code = getOrCreateReferralCode(personId);

        List<ReferralRewardEntity> allRewards = referralRewardRepository.findByReferrerIdAndStatus(
                        personId, ReferralRewardStatusEnum.PENDING).stream()
                .collect(Collectors.toList());
        List<ReferralRewardEntity> applied = referralRewardRepository.findByReferrerIdAndStatus(
                personId, ReferralRewardStatusEnum.APPLIED);

        return ReferralDTO.ReferralSummaryResponse.builder()
                .referralCode(code)
                .referralLink(frontendUrl + "/register?ref=" + code)
                .totalReferred(allRewards.size() + applied.size())
                .totalRewarded(applied.size())
                .pendingRewards(allRewards.stream().map(this::toRewardResponse).collect(Collectors.toList()))
                .appliedRewards(applied.stream().map(this::toRewardResponse).collect(Collectors.toList()))
                .build();
    }

    private ReferralDTO.ReferralRewardResponse toRewardResponse(ReferralRewardEntity reward)
    {
        return ReferralDTO.ReferralRewardResponse.builder()
                .id(reward.getId())
                .refereeName(reward.getReferee().getName())
                .bonusDays(reward.getBonusDays())
                .status(reward.getStatus())
                .appliedToShopId(reward.getAppliedToShop() != null ? reward.getAppliedToShop().getId() : null)
                .appliedToShopName(reward.getAppliedToShop() != null ? reward.getAppliedToShop().getName() : null)
                .appliedAt(reward.getAppliedAt())
                .createdAt(reward.getCreatedAt())
                .build();
    }

    public List<ReferralDTO.EligibleShopOption> getEligibleShopsForReward(UUID referrerId)
    {
        return shopMembershipRepository.findByPersonId(referrerId).stream()
                .filter(ShopMembershipEntity::isActive)
                .map(m -> ReferralDTO.EligibleShopOption.builder()
                        .shopId(m.getShop().getId())
                        .shopName(m.getShop().getName())
                        .build())
                .collect(Collectors.toList());
    }
}
