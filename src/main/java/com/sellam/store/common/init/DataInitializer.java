package com.sellam.store.common.init;

import com.sellam.store.accounts.models.AccountEntity;
import com.sellam.store.accounts.repositories.AccountRepository;
import com.sellam.store.dailybalance.models.BalanceStatusEnum;
import com.sellam.store.dailybalance.models.DailyBalanceEntity;
import com.sellam.store.dailybalance.repositories.DailyBalanceRepository;
import com.sellam.store.balancesettings.models.BalanceSettingsEntity;
import com.sellam.store.balancesettings.repositories.BalanceSettingsRepository;
import com.sellam.store.products.models.ProductEntity;
import com.sellam.store.products.repositories.ProductsRepository;
import com.sellam.store.saletypes.models.SaleTypeEntity;
import com.sellam.store.saletypes.repositories.SaleTypeRepository;
import com.sellam.store.sales.models.SaleEntity;
import com.sellam.store.sales.models.SaleStatusEnum;
import com.sellam.store.sales.repositories.SalesRepository;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.shops.repositories.ShopRepository;
import com.sellam.store.users.models.RoleEnum;
import com.sellam.store.users.models.UserEntity;
import com.sellam.store.users.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@Profile("!prod")
public class DataInitializer implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final ProductsRepository productsRepository;
    private final SalesRepository salesRepository;
    private final DailyBalanceRepository dailyBalanceRepository;
    private final BalanceSettingsRepository balanceSettingsRepository;
    private final SaleTypeRepository saleTypeRepository;
    private final PasswordEncoder passwordEncoder;

    private final Random random = new Random();

    public DataInitializer(AccountRepository accountRepository,
                           ShopRepository shopRepository,
                           UserRepository userRepository,
                           ProductsRepository productsRepository,
                           SalesRepository salesRepository,
                           DailyBalanceRepository dailyBalanceRepository,
                           BalanceSettingsRepository balanceSettingsRepository,
                           SaleTypeRepository saleTypeRepository,
                           PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.productsRepository = productsRepository;
        this.salesRepository = salesRepository;
        this.dailyBalanceRepository = dailyBalanceRepository;
        this.balanceSettingsRepository = balanceSettingsRepository;
        this.saleTypeRepository = saleTypeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String @NonNull ... args) {

        // Ne remplit que si la base est vide (évite les doublons à chaque redémarrage)
        if (accountRepository.count() > 0) {
            log.info("DataInitializer : données déjà présentes, initialisation ignorée.");
            return;
        }

        log.info("DataInitializer : création des données de test...");

        // 1. Comptes (gérants)
        List<AccountEntity> accounts = new ArrayList<>();
        String[] accountNames = {"Nelson", "Gina", "Paul", "Sarah", "Eric",
                "Marie", "Jean", "Alice", "David", "Fatou"};
        for (int i = 0; i < 10; i++) {
            AccountEntity account = new AccountEntity();
            account.setName(accountNames[i]);
            account.setPhoneNumber("69900000" + i);
            account.setPasswordHash(passwordEncoder.encode("password123"));
            accounts.add(accountRepository.save(account));
        }

        // 2. Boutiques (une par compte, pour garder ça simple et cohérent)
        List<ShopEntity> shops = new ArrayList<>();
        String[] shopNames = {"Boutique Centrale", "Marché du Nord", "Épicerie Gina",
                "Superette Paul", "Alimentation Sarah", "Quincaillerie Eric",
                "Boutique Marie", "Marché Jean", "Cosmétiques Alice", "Provisions David"};
        for (int i = 0; i < 10; i++) {
            ShopEntity shop = new ShopEntity();
            shop.setName(shopNames[i]);
            shop.setAddress("Quartier " + (i + 1) + ", Yaoundé");
            shop.setLogoUrl(null); // laissera le défaut géré par le service si applicable
            shop.setAccount(accounts.get(i));
            shops.add(shopRepository.save(shop));
        }

        // 3. Utilisateurs internes (caissière/secrétaire), 1-2 par boutique
        RoleEnum[] roles = {RoleEnum.CASHIER, RoleEnum.SECRETARY};
        String[] userNames = {"Aïcha", "Bruno", "Chloé", "Didier", "Emma",
                "Franck", "Grace", "Hugo", "Inès", "Julien"};
        for (int i = 0; i < 10; i++) {
            UserEntity user = new UserEntity();
            user.setName(userNames[i]);
            user.setPhoneNumber("68800000" + i);
            user.setPasswordHash(passwordEncoder.encode("password123"));
            user.setRole(roles[i % 2]);
            user.setActive(true);
            user.setShop(shops.get(i % shops.size()));
            userRepository.save(user);
        }

        // 4. Types de vente et Produits, répartis entre les boutiques (au moins 10 au total)
        SaleTypeEntity unitType = saleTypeRepository.save(SaleTypeEntity.builder().name("Unité").unitLabel("unité").isDefault(true).build());
        SaleTypeEntity batchType = saleTypeRepository.save(SaleTypeEntity.builder().name("Tas").unitLabel("tas").isDefault(true).build());
        SaleTypeEntity weightType = saleTypeRepository.save(SaleTypeEntity.builder().name("Poids").unitLabel("kg").isDefault(true).build());
        SaleTypeEntity[] saleTypes = {weightType, unitType, batchType};

        String[] productNames = {"Riz", "Huile", "Savon", "Sucre", "Farine",
                "Lait en poudre", "Sel", "Tomates", "Oignons", "Pâtes"};
        
        List<ProductEntity> products = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ProductEntity product = new ProductEntity();
            product.setName(productNames[i]);
            product.setSaleType(saleTypes[i % saleTypes.length]);
            BigDecimal purchasePrice = BigDecimal.valueOf(200 + random.nextInt(300));
            BigDecimal margin = BigDecimal.valueOf(50 + random.nextInt(150));
            product.setPurchasePrice(purchasePrice);
            product.setSellingPrice(purchasePrice.add(margin));
            product.setStockQuantity(BigDecimal.valueOf(20 + random.nextInt(80)));
            product.setAlertThreshold(BigDecimal.valueOf(5));
            product.setCategory("Alimentaire");
            product.setShop(shops.get(i % shops.size()));
            products.add(productsRepository.save(product));
        }

        // 5. Ventes, liées à un produit existant (et donc indirectement à sa boutique)
        List<SaleEntity> sales = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ProductEntity product = products.get(i % products.size());
            BigDecimal quantity = BigDecimal.valueOf(1 + random.nextInt(5));

            SaleEntity sale = new SaleEntity();
            sale.setProduct(product);
            sale.setShop(product.getShop());
            sale.setQuantity(quantity);
            sale.setTotalPrice(product.getSellingPrice().multiply(quantity));
            sale.setMargin(product.getSellingPrice().subtract(product.getPurchasePrice()).multiply(quantity));
            sale.setStatus(SaleStatusEnum.CONFIRMED);
            sales.add(salesRepository.save(sale));
        }

        // 6. Bilans journaliers, un par boutique pour aujourd'hui
        for (ShopEntity shop : shops) {
            BigDecimal totalSales = sales.stream()
                    .filter(s -> s.getShop().getId().equals(shop.getId()))
                    .map(SaleEntity::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalMargin = sales.stream()
                    .filter(s -> s.getShop().getId().equals(shop.getId()))
                    .map(SaleEntity::getMargin)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalSales.compareTo(BigDecimal.ZERO) == 0) {
                continue; // pas de vente pour cette boutique aujourd'hui, on saute
            }

            DailyBalanceEntity balance = new DailyBalanceEntity();
            balance.setShop(shop);
            balance.setBalanceDate(LocalDate.now());
            balance.setComputedTotalSales(totalSales);
            balance.setComputedTotalMargin(totalMargin);
            balance.setDeclaredCash(totalSales); // on simule un bilan parfait
            balance.setDiscrepancy(BigDecimal.ZERO);
            balance.setStatus(BalanceStatusEnum.OK);
            dailyBalanceRepository.save(balance);
        }

        // 7. Paramètres de bilan, pour chaque jour de semaine, sur chaque boutique
        for (ShopEntity shop : shops) {
            for (DayOfWeek day : DayOfWeek.values()) {
                BalanceSettingsEntity setting = new BalanceSettingsEntity();
                setting.setShop(shop);
                setting.setDayOfWeek(day);
                setting.setBalanceTime(day == DayOfWeek.SATURDAY ? LocalTime.of(13, 0) : LocalTime.of(18, 0));
                setting.setReminderFrequencyHours(3);
                balanceSettingsRepository.save(setting);
            }
        }

        log.info("DataInitializer : données de test créées avec succès !");
        log.info("Comptes : {} | Boutiques : {} | Produits : {} | Ventes : {}", accounts.size(), shops.size(), products.size(), sales.size());
        log.info("Connecte-toi avec le numéro 699000000 et le mot de passe 'password123' pour tester.");
    }
}