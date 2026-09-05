package com.sellam.store.common.init;

import com.sellam.store.balancesettings.models.BalanceSettingsEntity;
import com.sellam.store.balancesettings.repositories.BalanceSettingsRepository;
import com.sellam.store.identity.models.PersonEntity;
import com.sellam.store.identity.models.ShopMembershipEntity;
import com.sellam.store.identity.repositories.PersonRepository;
import com.sellam.store.identity.repositories.ShopMembershipRepository;
import com.sellam.store.invoices.models.InvoiceEntity;
import com.sellam.store.invoices.models.InvoiceStatusEnum;
import com.sellam.store.invoices.repositories.InvoiceRepository;
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
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
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
@Profile("dev")
public class DataInitializer implements CommandLineRunner {

    private final PersonRepository personRepository;
    private final ShopRepository shopRepository;
    private final ShopMembershipRepository shopMembershipRepository;
    private final ProductsRepository productsRepository;
    private final SalesRepository salesRepository;
    private final InvoiceRepository invoiceRepository;
    private final BalanceSettingsRepository balanceSettingsRepository;
    private final SaleTypeRepository saleTypeRepository;
    private final PasswordEncoder passwordEncoder;

    private final Random random = new Random();

    public DataInitializer(PersonRepository personRepository,
                           ShopRepository shopRepository,
                           ShopMembershipRepository shopMembershipRepository,
                           ProductsRepository productsRepository,
                           SalesRepository salesRepository,
                           InvoiceRepository invoiceRepository,
                           BalanceSettingsRepository balanceSettingsRepository,
                           SaleTypeRepository saleTypeRepository,
                           PasswordEncoder passwordEncoder) {
        this.personRepository = personRepository;
        this.shopRepository = shopRepository;
        this.shopMembershipRepository = shopMembershipRepository;
        this.productsRepository = productsRepository;
        this.salesRepository = salesRepository;
        this.invoiceRepository = invoiceRepository;
        this.balanceSettingsRepository = balanceSettingsRepository;
        this.saleTypeRepository = saleTypeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String @NonNull ... args) {

        if (personRepository.count() > 0) {
            log.info("DataInitializer : donnÃ©es dÃ©jÃ  prÃ©sentes, initialisation ignorÃ©e.");
            return;
        }

        log.info("DataInitializer : crÃ©ation des donnÃ©es de test...");

        // 1. Comptes (gÃ©rants)
        List<PersonEntity> persons = new ArrayList<>();
        String[] personNames = {"Nelson", "Gina", "Paul", "Sarah", "Eric",
                "Marie", "Jean", "Alice", "David", "Fatou"};
        for (int i = 0; i < 10; i++) {
            PersonEntity person = new PersonEntity();
            person.setName(personNames[i]);
            person.setPhoneNumber("69900000" + i);
            person.setPasswordHash(passwordEncoder.encode("password123"));
            persons.add(personRepository.save(person));
        }

        // 2. Boutiques (une par compte)
        List<ShopEntity> shops = new ArrayList<>();
        String[] shopNames = {"Boutique Centrale", "MarchÃ© du Nord", "Ã‰picerie Gina",
                "Superette Paul", "Alimentation Sarah", "Quincaillerie Eric",
                "Boutique Marie", "MarchÃ© Jean", "CosmÃ©tiques Alice", "Provisions David"};
        for (int i = 0; i < 10; i++) {
            ShopEntity shop = new ShopEntity();
            shop.setName(shopNames[i]);
            shop.setAddress("Quartier " + (i + 1) + ", YaoundÃ©");
            shop.setLogoUrl(null);
            shop = shopRepository.save(shop);
            shops.add(shop);

            ShopMembershipEntity membership = new ShopMembershipEntity();
            membership.setPerson(persons.get(i));
            membership.setShop(shop);
            membership.setRole(RoleEnum.MANAGER);
            membership.setActive(true);
            shopMembershipRepository.save(membership);
        }

        // 3. Utilisateurs internes (caissiÃ¨re/secrÃ©taire)
        RoleEnum[] roles = {RoleEnum.CASHIER, RoleEnum.SECRETARY};
        String[] userNames = {"AÃ¯cha", "Bruno", "ChloÃ©", "Didier", "Emma",
                "Franck", "Grace", "Hugo", "InÃ¨s", "Julien"};
        for (int i = 0; i < 10; i++) {
            PersonEntity person = new PersonEntity();
            person.setName(userNames[i]);
            person.setPhoneNumber("68800000" + i);
            person.setPasswordHash(passwordEncoder.encode("password123"));
            person = personRepository.save(person);

            ShopMembershipEntity membership = new ShopMembershipEntity();
            membership.setPerson(person);
            membership.setShop(shops.get(i % shops.size()));
            membership.setRole(roles[i % 2]);
            membership.setActive(true);
            shopMembershipRepository.save(membership);
        }

        // 4. Types de vente et Produits
        SaleTypeEntity unitType = saleTypeRepository.save(SaleTypeEntity.builder().name("UnitÃ©").unitLabel("unitÃ©").isDefault(true).build());
        SaleTypeEntity batchType = saleTypeRepository.save(SaleTypeEntity.builder().name("Tas").unitLabel("tas").isDefault(true).build());
        SaleTypeEntity weightType = saleTypeRepository.save(SaleTypeEntity.builder().name("Poids").unitLabel("kg").isDefault(true).build());
        SaleTypeEntity[] saleTypes = {weightType, unitType, batchType};

        String[] productNames = {"Riz", "Huile", "Savon", "Sucre", "Farine",
                "Lait en poudre", "Sel", "Tomates", "Oignons", "PÃ¢tes"};
        
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

        // 5. Ventes
        List<SaleEntity> sales = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ProductEntity product = products.get(i % products.size());
            BigDecimal quantity = BigDecimal.valueOf(1 + random.nextInt(5));
            PersonEntity seller = persons.get(i % persons.size());
            ShopEntity shop = product.getShop();
            
            InvoiceEntity invoice = new InvoiceEntity();
            invoice.setShop(shop);
            invoice.setInvoiceNumber("TEST-" + (1000 + i));
            invoice.setTotalAmount(product.getSellingPrice().multiply(quantity));
            invoice.setStatus(InvoiceStatusEnum.VALIDATED);
            invoice.setSoldBy(seller.getId().toString());
            invoice = invoiceRepository.save(invoice);

            SaleEntity sale = new SaleEntity();
            sale.setInvoice(invoice);
            sale.setProduct(product);
            sale.setShop(shop);
            sale.setPerson(seller);
            sale.setQuantity(quantity);
            sale.setTotalPrice(product.getSellingPrice().multiply(quantity));
            sale.setMargin(product.getSellingPrice().subtract(product.getPurchasePrice()).multiply(quantity));
            sale.setStatus(SaleStatusEnum.CONFIRMED);
            sales.add(salesRepository.save(sale));
        }

        // 6.b NOUVEAU: Création des caisses par défaut
        // We will just skip creating the default registers here to avoid context issues. 
        // CashSessionService getOrCreateDefaultRegister will handle it dynamically on first sale or request.

        // 7. ParamÃ¨tres de bilan
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

        log.info("DataInitializer : donnÃ©es de test crÃ©Ã©es avec succÃ¨s !");
        log.info("Comptes : {} | Boutiques : {} | Produits : {} | Ventes : {}", persons.size(), shops.size(), products.size(), sales.size());
        log.info("Connecte-toi avec le numÃ©ro 699000000 et le mot de passe 'password123' pour tester.");
    }
}