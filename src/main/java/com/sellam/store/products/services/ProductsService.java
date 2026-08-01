package com.sellam.store.products.services;
import com.sellam.store.common.exception.ResourceNotFoundException;
import com.sellam.store.products.dto.ProductsDTO;
import com.sellam.store.products.models.ProductEntity;
import com.sellam.store.products.models.exception.ProductsException;
import com.sellam.store.saletypes.models.SaleTypeEntity;
import com.sellam.store.saletypes.repositories.SaleTypeRepository;
import com.sellam.store.shops.models.ShopEntity;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Service;
import com.sellam.store.products.repositories.ProductsRepository;
import com.sellam.store.shops.repositories.ShopRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductsService
{

    private final ProductsRepository productsRepository;

    private final ShopRepository shopRepository;

    private final SaleTypeRepository saleTypeRepository;

    public ProductsService(ProductsRepository productsRepository, ShopRepository shopRepository, SaleTypeRepository saleTypeRepository)
    {
        this.productsRepository = productsRepository;
        this.shopRepository = shopRepository;
        this.saleTypeRepository = saleTypeRepository;
    }


    public ProductsDTO.PostOutput createProduct(ProductsDTO.PostInput input) throws ProductsException
    {
        if(input.getName()==null || StringUtils.isBlank(input.getName()))
        {
            throw new ProductsException("le nom du produit ne peut pas être vide !");
        }

        ProductEntity existingProduct = productsRepository.findByNameAndCategory(input.getName(),input.getCategory());


        if(existingProduct!=null)
        {
            throw new ProductsException("Le produit existe déjà !");
        }

        ShopEntity shop = shopRepository.findById(input.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Boutique Introuvable"));

        SaleTypeEntity saleType = saleTypeRepository.findById(input.getSaleTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Type de vente introuvable"));

        ProductEntity newProduct = ProductEntity.builder()
                .name(input.getName())
                .barcode(input.getBarcode())
                .pictureUrl(input.getPictureUrl())
                .saleType(saleType)
                .purchasePrice(input.getPurchasePrice())
                .sellingPrice(input.getSellingPrice())
                .stockQuantity(input.getStockQuantity())
                .alertThreshold(input.getAlertThreshold())
                .category(input.getCategory())
                .expirationDate(input.getExpirationDate())
                .shop(shop)
                .build();

        productsRepository.save(newProduct);

        return toPostOutput(newProduct);

    }


    public List<ProductsDTO.PostOutput> listProducts(UUID shopId)
    {
        return productsRepository.findByShop_Id(shopId)
                .stream()
                .map(this::toPostOutput)
                .collect(Collectors.toList());
    }


    public ProductsDTO.PostOutput getProductById(UUID id) {
        ProductEntity product = productsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit Introuvable"));
        return toPostOutput(product);
    }

    public ProductsDTO.PostOutput updateProduct(UUID id, ProductsDTO.PostInput input)
    {
        ProductEntity product = productsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit Introuvable"));
        
        SaleTypeEntity saleType = saleTypeRepository.findById(input.getSaleTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Type de vente introuvable"));

        product.setName(input.getName());
        product.setBarcode(input.getBarcode());
        product.setPictureUrl(input.getPictureUrl());
        product.setSaleType(saleType);
        product.setPurchasePrice(input.getPurchasePrice());
        product.setSellingPrice(input.getSellingPrice());
        product.setStockQuantity(input.getStockQuantity());
        product.setAlertThreshold(input.getAlertThreshold());
        product.setCategory(input.getCategory());
        product.setExpirationDate(input.getExpirationDate());
        
        productsRepository.save(product);
        return toPostOutput(product);
    }


    public boolean checkAlertThreshold(UUID productId)
    {
        ProductEntity product = productsRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit Introuvable"));
        return product.getStockQuantity().compareTo(product.getAlertThreshold()) <= 0;
    }


    public ProductsDTO.PostOutput toPostOutput(ProductEntity newProduct)
    {
        return ProductsDTO.PostOutput.builder()
                .id(newProduct.getId())
                .name(newProduct.getName())
                .barcode(newProduct.getBarcode())
                .pictureUrl(newProduct.getPictureUrl())
                .saleTypeId(newProduct.getSaleType().getId())
                .saleTypeName(newProduct.getSaleType().getName())
                .saleTypeUnitLabel(newProduct.getSaleType().getUnitLabel())
                .purchasePrice(newProduct.getPurchasePrice())
                .sellingPrice(newProduct.getSellingPrice())
                .stockQuantity(newProduct.getStockQuantity())
                .alertThreshold(newProduct.getAlertThreshold())
                .category(newProduct.getCategory())
                .createdAt(newProduct.getCreatedAt())
                .expirationDate(newProduct.getExpirationDate())
                .shopId(newProduct.getShop().getId())
                .build();
    }

}
