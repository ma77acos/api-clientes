// src/main/java/com/reservas/service/ProductService.java
package com.reservas.service;

import com.reservas.dto.request.ProductRequest;
import com.reservas.dto.response.ProductResponse;
import com.reservas.entity.Product;
import com.reservas.entity.User;
import com.reservas.enums.ProductCategory;
import com.reservas.enums.Role;
import com.reservas.exception.BadRequestException;
import com.reservas.exception.ResourceNotFoundException;
import com.reservas.exception.UnauthorizedException;
import com.reservas.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    // ==================== CREAR PRODUCTO ====================

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        // Verificar nombre duplicado
        if (productRepository.existsByComplexIdAndNameIgnoreCase(complexId, request.getName())) {
            throw new BadRequestException("Ya existe un producto con ese nombre");
        }

        Product product = Product.builder()
                .complex(currentUser.getComplex())
                .name(request.getName())
                .category(request.getCategory())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .available(request.getAvailable() != null ? request.getAvailable() : true)
                .quickProduct(request.getQuickProduct() != null ? request.getQuickProduct() : false)
                .build();

        product = productRepository.save(product);

        log.info("✅ Producto creado: {} - ${}", product.getName(), product.getPrice());

        return mapToResponse(product);
    }

    // ==================== ACTUALIZAR PRODUCTO ====================

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductRequest request) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", productId));

        // Verificar que pertenece al complejo
        if (!product.getComplex().getId().equals(complexId)) {
            throw new UnauthorizedException("No tenés acceso a este producto");
        }

        // Verificar nombre duplicado (excluyendo el actual)
        if (productRepository.existsByComplexIdAndNameIgnoreCaseAndIdNot(
                complexId, request.getName(), productId)) {
            throw new BadRequestException("Ya existe otro producto con ese nombre");
        }

        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        if (request.getAvailable() != null) {
            product.setAvailable(request.getAvailable());
        }
        if (request.getQuickProduct() != null) {
            product.setQuickProduct(request.getQuickProduct());
        }

        product = productRepository.save(product);

        log.info("✏️ Producto actualizado: {} - ${}", product.getName(), product.getPrice());

        return mapToResponse(product);
    }

    // ==================== ELIMINAR PRODUCTO ====================

    @Transactional
    public void deleteProduct(Long productId) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", productId));

        if (!product.getComplex().getId().equals(complexId)) {
            throw new UnauthorizedException("No tenés acceso a este producto");
        }

        // En lugar de eliminar, desactivamos
        product.setAvailable(false);
        productRepository.save(product);

        log.info("🗑️ Producto desactivado: {}", product.getName());
    }

    // ==================== LISTAR PRODUCTOS ====================

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        return productRepository.findByComplexIdOrderByNameAsc(complexId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAvailableProducts() {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        return productRepository.findByComplexIdAndAvailableTrueOrderByNameAsc(complexId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategory(ProductCategory category) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        return productRepository.findByComplexIdAndCategoryAndAvailableTrueOrderByNameAsc(
                        complexId, category)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // 🆕 NUEVO MÉTODO - Obtener productos rápidos
    @Transactional(readOnly = true)
    public List<ProductResponse> getQuickProducts() {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        return productRepository.findByComplexIdAndAvailableTrueAndQuickProductTrueOrderByNameAsc(complexId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long productId) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", productId));

        if (!product.getComplex().getId().equals(complexId)) {
            throw new UnauthorizedException("No tenés acceso a este producto");
        }

        return mapToResponse(product);
    }

    // ==================== TOGGLE DISPONIBILIDAD ====================

    @Transactional
    public ProductResponse toggleAvailability(Long productId) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", productId));

        if (!product.getComplex().getId().equals(complexId)) {
            throw new UnauthorizedException("No tenés acceso a este producto");
        }

        product.setAvailable(!product.getAvailable());
        product = productRepository.save(product);

        log.info("🔄 Producto {} ahora está {}",
                product.getName(),
                product.getAvailable() ? "disponible" : "no disponible");

        return mapToResponse(product);
    }

    // 🆕 NUEVO MÉTODO - Toggle producto rápido
    @Transactional
    public ProductResponse toggleQuickProduct(Long productId) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", productId));

        if (!product.getComplex().getId().equals(complexId)) {
            throw new UnauthorizedException("No tenés acceso a este producto");
        }

        product.setQuickProduct(!product.getQuickProduct());
        product = productRepository.save(product);

        log.info("⚡ Producto {} ahora {} es producto rápido",
                product.getName(),
                product.getQuickProduct() ? "SÍ" : "NO");

        return mapToResponse(product);
    }

    // ==================== HELPERS ====================

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    private Long getComplexId(User user) {
        if (user.getRole() != Role.BUSINESS && user.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("No tenés permisos");
        }
        if (user.getComplex() == null) {
            throw new BadRequestException("No tenés un complejo asignado");
        }
        return user.getComplex().getId();
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .category(product.getCategory())
                .categoryDisplayName(product.getCategory().getDisplayName())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .available(product.getAvailable())
                .quickProduct(product.getQuickProduct())
                .createdAt(product.getCreatedAt())
                .build();
    }
}