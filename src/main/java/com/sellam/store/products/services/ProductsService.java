package com.sellam.store.products.services;
import com.sellam.store.common.exception.ResourceNotFoundException;
import com.sellam.store.products.dto.ProductsDTO;
import com.sellam.store.products.models.ProductEntity;
import com.sellam.store.products.models.exception.ProductsException;
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

    public ProductsService(ProductsRepository productsRepository, ShopRepository shopRepository)
    {
        this.productsRepository = productsRepository;
        this.shopRepository = shopRepository;
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
        ProductEntity newProduct = ProductEntity.builder()
                .name(input.getName())
                .barcode(input.getBarcode())
                .pictureUrl(input.getPictureUrl())
                .saleTypeEnum(input.getSaleTypeEnum())
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


    public ProductsDTO.PostOutput updateProduct(UUID id, ProductsDTO.PostInput input)
    {
       return null;
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
                .saleTypeEnum(newProduct.getSaleTypeEnum())
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
