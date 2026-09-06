package com.sellam.store.shops.services;

import com.sellam.store.common.exception.ResourceNotFoundException;
import com.sellam.store.identity.models.PersonEntity;
import com.sellam.store.identity.models.ShopMembershipEntity;
import com.sellam.store.identity.repositories.PersonRepository;
import com.sellam.store.identity.repositories.ShopMembershipRepository;
import com.sellam.store.invoices.repositories.InvoiceRepository;
import com.sellam.store.shops.dto.ShopDTO;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.shops.repositories.ShopRepository;
import com.sellam.store.users.models.RoleEnum;
import com.sellam.store.subscriptions.services.SubscriptionService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@AllArgsConstructor
public class ShopService
{

    private final ShopRepository shopRepository;
    private final PersonRepository personRepository;
    private final ShopMembershipRepository shopMembershipRepository;
    private final SubscriptionService subscriptionService;
    private final InvoiceRepository invoiceRepository;


    public ShopDTO.ShopResponse createShop(ShopDTO.ShopRequest request, UUID accountId)
    {

        PersonEntity person = personRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable"));

        ShopEntity shop = ShopEntity.builder()
                .name(request.getName())
                .address(request.getAddress())
                .logoUrl(request.getLogoUrl())
                .phoneNumber(request.getPhoneNumber())
                .taxpayerNumber(request.getTaxpayerNumber())
                .build();

        ShopEntity newShop = shopRepository.save(shop);

        subscriptionService.createTrialSubscription(newShop);

        ShopMembershipEntity membership = new ShopMembershipEntity();
        membership.setPerson(person);
        membership.setShop(newShop);
        membership.setRole(RoleEnum.MANAGER);
        membership.setActive(true);
        membership.setJoinedAt(LocalDateTime.now());
        
        shopMembershipRepository.save(membership);

        return toResponse(newShop);
    }

    public List<ShopDTO.ShopResponse> listShops(UUID accountId)
    {
        return shopMembershipRepository.findByPersonId(accountId)
                .stream()
                .filter(ShopMembershipEntity::isActive)
                .map(ShopMembershipEntity::getShop)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ShopDTO.ShopSummaryResponse> listShopsSummaries(UUID accountId)
    {
        List<ShopMembershipEntity> memberships = shopMembershipRepository.findByPersonId(accountId)
                .stream()
                .filter(ShopMembershipEntity::isActive)
                .toList();

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        return memberships.stream().map(membership -> {
            ShopEntity shop = membership.getShop();
            UUID shopId = shop.getId();

            List<Object[]> summary = invoiceRepository.getSummaryReportData(shopId, startOfDay, endOfDay);
            BigDecimal salesToday = BigDecimal.ZERO;
            BigDecimal marginAmount = BigDecimal.ZERO;
            if (summary != null && !summary.isEmpty()) {
                Object[] row = summary.get(0);
                if (row[0] != null) salesToday = new BigDecimal(row[0].toString());
                if (row[1] != null) marginAmount = new BigDecimal(row[1].toString());
            }

            String marginStr = "—";
            if (salesToday.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal pct = marginAmount.divide(salesToday, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(0, RoundingMode.HALF_UP);
                marginStr = pct.toPlainString() + " %";
            }

            long teamCount = shopMembershipRepository.countActiveMembersByShopId(shopId);

            return ShopDTO.ShopSummaryResponse.builder()
                    .id(shop.getId())
                    .name(shop.getName())
                    .address(shop.getAddress())
                    .logoUrl(shop.getLogoUrl())
                    .phoneNumber(shop.getPhoneNumber())
                    .taxpayerNumber(shop.getTaxpayerNumber())
                    .autoPrintInvoices(shop.getAutoPrintInvoices())
                    .salesToday(salesToday)
                    .margin(marginStr)
                    .teamCount(teamCount)
                    .build();
        }).collect(Collectors.toList());
    }

    public ShopDTO.ShopResponse getShopById(UUID shopId) {
        if (shopId == null) {
            throw new ResourceNotFoundException("Aucune boutique associée à ce compte");
        }
        ShopEntity shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique introuvable"));
        return toResponse(shop);
    }

    private ShopDTO.ShopResponse toResponse(ShopEntity shop)
    {
        return ShopDTO.ShopResponse.builder()
                .id(shop.getId())
                .name(shop.getName())
                .address(shop.getAddress())
                .logoUrl(shop.getLogoUrl())
                .phoneNumber(shop.getPhoneNumber())
                .taxpayerNumber(shop.getTaxpayerNumber())
                .autoPrintInvoices(shop.getAutoPrintInvoices())
                .build();
    }


    public ShopDTO.ShopResponse updateSettings(UUID shopId, ShopDTO.ShopSettingsRequest request)
    {
        ShopEntity shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique introuvable"));
        if (request.getAutoPrintInvoices() != null)
        {
            shop.setAutoPrintInvoices(request.getAutoPrintInvoices());
        }
        if (request.getLogoUrl() != null)
        {
            shop.setLogoUrl(request.getLogoUrl());
        }
        if (request.getPhoneNumber() != null)
        {
            shop.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getTaxpayerNumber() != null)
        {
            shop.setTaxpayerNumber(request.getTaxpayerNumber());
        }
        return toResponse(shopRepository.save(shop));
    }
}