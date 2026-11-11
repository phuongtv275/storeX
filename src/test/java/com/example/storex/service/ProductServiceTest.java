package com.example.storex.service;

import com.example.storex.exception.ResourceNotFoundException;
import com.example.storex.model.dto.ProductRequestDto;
import com.example.storex.model.dto.ProductResponseDto;
import com.example.storex.model.entity.Product;
import com.example.storex.repository.ProductRepository;
import com.example.storex.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product sampleProduct;
    private ProductRequestDto sampleRequest;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder()
                .id(1L)
                .name("Keyboard")
                .price(new BigDecimal("79.99"))
                .quantity(30)
                .description("Mechanical Keyboard")
                .build();

        sampleRequest = ProductRequestDto.builder()
                .name("Keyboard")
                .price(new BigDecimal("79.99"))
                .quantity(30)
                .description("Mechanical Keyboard")
                .build();
    }

    @Test
    @DisplayName("getAllProducts returns all product DTOs")
    void shouldGetAllProducts() {
        when(productRepository.findAll()).thenReturn(List.of(sampleProduct));

        List<ProductResponseDto> result = productService.getAllProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Keyboard");
        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("getProductById returns product DTO when found")
    void shouldGetProductByIdWhenFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

        ProductResponseDto result = productService.getProductById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Keyboard");
        verify(productRepository).findById(1L);
    }

    @Test
    @DisplayName("getProductById throws ResourceNotFoundException when not found")
    void shouldThrowWhenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with id: 99");
    }

    @Test
    @DisplayName("createProduct saves entity and returns response DTO")
    void shouldCreateProduct() {
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductResponseDto result = productService.createProduct(sampleRequest);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Keyboard");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("updateProduct updates existing entity and returns response DTO")
    void shouldUpdateProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductRequestDto updateRequest = ProductRequestDto.builder()
                .name("Wireless Keyboard")
                .price(new BigDecimal("89.99"))
                .quantity(25)
                .description("Updated description")
                .build();

        ProductResponseDto result = productService.updateProduct(1L, updateRequest);

        assertThat(result).isNotNull();
        verify(productRepository).save(sampleProduct);
    }

    @Test
    @DisplayName("updateProduct throws ResourceNotFoundException when ID does not exist")
    void shouldThrowOnUpdateWhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(99L, sampleRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with id: 99");
    }

    @Test
    @DisplayName("deleteProduct deletes entity when found")
    void shouldDeleteProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        doNothing().when(productRepository).delete(sampleProduct);

        productService.deleteProduct(1L);

        verify(productRepository).delete(sampleProduct);
    }

    @Test
    @DisplayName("deleteProduct throws ResourceNotFoundException when ID does not exist")
    void shouldThrowOnDeleteWhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with id: 99");
    }
}
