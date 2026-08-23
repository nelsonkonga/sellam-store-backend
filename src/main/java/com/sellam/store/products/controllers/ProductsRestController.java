package com.sellam.store.products.controllers;

import com.sellam.store.products.dto.ProductsDTO;
import com.sellam.store.products.models.exception.ProductsException;
import com.sellam.store.products.services.ProductsService;
import com.sellam.store.shops.services.SupabaseStorageService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("api/products")
@AllArgsConstructor
public class ProductsRestController
{

    private final ProductsService productsService;
    private final SupabaseStorageService supabaseStorageService;

    @PreAuthorize("@sec.can(authentication, 'VIEW_PRODUCTS')")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ProductsDTO.ProductResponse> listProducts(@RequestParam UUID shopId)
    {
        return productsService.listProducts(shopId);
    }

    @PreAuthorize("@sec.can(authentication, 'EDIT_PRODUCTS')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductsDTO.ProductResponse post(@Valid @RequestBody ProductsDTO.ProductRequest input) throws ProductsException
    {
        return productsService.createProduct(input);
    }

    @PreAuthorize("@sec.can(authentication, 'VIEW_PRODUCTS')")
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ProductsDTO.ProductResponse getProductById(@PathVariable UUID id)
    {
        return productsService.getProductById(id);
    }

    @PreAuthorize("@sec.can(authentication, 'EDIT_PRODUCTS')")
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ProductsDTO.ProductResponse updateProduct(@PathVariable UUID id, @Valid @RequestBody ProductsDTO.ProductRequest input)
    {
        return productsService.updateProduct(id, input);
    }

    @PreAuthorize("@sec.can(authentication, 'EDIT_PRODUCTS')")
    @PostMapping(value = "/{id}/picture", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> uploadProductPicture(@PathVariable UUID id, @RequestParam("picture") MultipartFile file)
    {
        String pictureUrl = supabaseStorageService.uploadProductPicture(id, file);
        productsService.updateProductPictureUrl(id, pictureUrl);
        return Map.of("pictureUrl", pictureUrl);
    }

    @PreAuthorize("@sec.can(authentication, 'VIEW_PRODUCTS')")
    @GetMapping("/top-selling")
    @ResponseStatus(HttpStatus.OK)
    public List<ProductsDTO.ProductSalesResponse> listTopSellingProducts(@RequestParam UUID shopId)
    {
        return productsService.listTopSellingProducts(shopId);
    }

    @PreAuthorize("@sec.can(authentication, 'VIEW_PRODUCTS')")
    @GetMapping("/barcode/{barcode}")
    @ResponseStatus(HttpStatus.OK)
    public ProductsDTO.ProductResponse getProductByBarcode(@PathVariable String barcode, @RequestParam UUID shopId)
    {
        return productsService.getProductByBarcode(shopId, barcode);
    }
}