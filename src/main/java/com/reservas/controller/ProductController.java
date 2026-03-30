// src/main/java/com/reservas/controller/ProductController.java
package com.reservas.controller;

import com.reservas.dto.request.ProductRequest;
import com.reservas.dto.response.ProductResponse;
import com.reservas.enums.ProductCategory;
import com.reservas.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUSINESS', 'ADMIN')")
public class ProductController {

    private final ProductService productService;

    /**
     * Crear producto
     */
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Actualizar producto
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Eliminar (desactivar) producto
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtener todos los productos (incluye no disponibles)
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<ProductResponse> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * Obtener solo productos disponibles
     */
    @GetMapping("/available")
    public ResponseEntity<List<ProductResponse>> getAvailableProducts() {
        List<ProductResponse> products = productService.getAvailableProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * Obtener productos por categoría
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(
            @PathVariable ProductCategory category) {
        List<ProductResponse> products = productService.getProductsByCategory(category);
        return ResponseEntity.ok(products);
    }

    /**
     * Obtener producto por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    /**
     * Toggle disponibilidad
     */
    @PatchMapping("/{id}/toggle-availability")
    public ResponseEntity<ProductResponse> toggleAvailability(@PathVariable Long id) {
        ProductResponse response = productService.toggleAvailability(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/quick")
    public ResponseEntity<List<ProductResponse>> getQuickProducts() {
        List<ProductResponse> products = productService.getQuickProducts();
        return ResponseEntity.ok(products);
    }

    @PatchMapping("/{id}/toggle-quick")
    public ResponseEntity<ProductResponse> toggleQuickProduct(@PathVariable Long id) {
        ProductResponse response = productService.toggleQuickProduct(id);
        return ResponseEntity.ok(response);
    }
}