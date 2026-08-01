package com.sellam.store.products.controllers;

import com.sellam.store.products.dto.ProductsDTO;
import com.sellam.store.products.models.exception.ProductsException;
import com.sellam.store.products.services.ProductsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/products")
public class ProductsRestController {

    private final ProductsService productsService;

    public ProductsRestController(ProductsService productsService)
    {
        this.productsService = productsService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ProductsDTO.PostOutput> listProducts(@RequestParam UUID shopId)
    {
        return productsService.listProducts(shopId);
    }

    @PostMapping

    @ResponseStatus(HttpStatus.CREATED)
    public ProductsDTO.PostOutput post(@Valid @RequestBody ProductsDTO.PostInput input) throws ProductsException
    {
        return productsService.createProduct(input);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ProductsDTO.PostOutput getProductById(@PathVariable UUID id) {
        return productsService.getProductById(id);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ProductsDTO.PostOutput updateProduct(@PathVariable UUID id, @Valid @RequestBody ProductsDTO.PostInput input) {
        return productsService.updateProduct(id, input);
    }
}
