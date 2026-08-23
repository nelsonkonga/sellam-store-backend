package com.sellam.store.products.services;
import com.sellam.store.common.exception.ResourceNotFoundException;
import com.sellam.store.products.dto.ProductsDTO;
import com.sellam.store.products.models.ProductEntity;
import com.sellam.store.products.models.exception.ProductsException;
import com.sellam.store.saletypes.models.SaleTypeEntity;
import com.sellam.store.saletypes.repositories.SaleTypeRepository;
import com.sellam.store.shops.models.ShopEntity;
import io.micrometer.common.util.StringUtils;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import com.sellam.store.products.repositories.ProductsRepository;
import com.sellam.store.shops.repositories.ShopRepository;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Validated
public class ProductsService
{

    private final ProductsRepository productsRepository;

    private final ShopRepository shopRepository;

    private final SaleTypeRepository saleTypeRepository;


    public ProductsDTO.ProductResponse createProduct(ProductsDTO.ProductRequest input) throws ProductsException
    {
        if(input.getName()==null || StringUtils.isBlank(input.getName()))
        {
            throw new ProductsException("le nom du produit ne peut pas être vide !");
        }

        // Vérifier que le produit n'existe pas dans CETTE boutique (shopId + name + category)
        ProductEntity existingProduct = productsRepository.findByShop_IdAndNameAndCategory(input.getShopId(), input.getName(), input.getCategory());

        if(existingProduct!=null)
        {
            throw new ProductsException("Le produit existe déjà dans cette boutique !");
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
                .brand(input.getBrand())
                .expirationDate(input.getExpirationDate())
                .shop(shop)
                .build();

        productsRepository.save(newProduct);

        return toPostOutput(newProduct);

    }


    public List<ProductsDTO.ProductResponse> listProducts(UUID shopId)
    {
        return productsRepository.findByShop_Id(shopId)
                .stream()
                .map(this::toPostOutput)
                .collect(Collectors.toList());
    }


    public ProductsDTO.ProductResponse getProductById(UUID id) {
        ProductEntity product = productsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit Introuvable"));
        return toPostOutput(product);
    }

    public ProductsDTO.ProductResponse updateProduct(UUID id, ProductsDTO.ProductRequest input)
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
        product.setBrand(input.getBrand());
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

    public void updateProductPictureUrl(UUID productId, String pictureUrl)
    {
        ProductEntity product = productsRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit Introuvable"));
        product.setPictureUrl(pictureUrl);
        productsRepository.save(product);
    }

    public List<ProductsDTO.ProductSalesResponse> listTopSellingProducts(UUID shopId) {
        return productsRepository.findTopSellingProductsByShopId(shopId)
                .stream()
                .map(row -> {
                    ProductEntity p = (ProductEntity) row[0];
                    java.math.BigDecimal totalSold = (java.math.BigDecimal) row[1];
                    return ProductsDTO.ProductSalesResponse.builder()
                            .product(toPostOutput(p))
                            .totalSold(totalSold)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public ProductsDTO.ProductResponse getProductByBarcode(UUID shopId, String barcode) {
        ProductEntity product = productsRepository.findByShop_IdAndBarcode(shopId, barcode)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable pour ce code-barres"));
        return toPostOutput(product);
    }


    public ProductsDTO.ProductResponse toPostOutput(ProductEntity newProduct)
    {
        return ProductsDTO.ProductResponse.builder()
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
                .brand(newProduct.getBrand())
                .createdAt(newProduct.getCreatedAt())
                .expirationDate(newProduct.getExpirationDate())
                .shopId(newProduct.getShop().getId())
                .build();
    }

}
