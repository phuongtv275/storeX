package com.example.storex.service.impl;

import com.example.storex.exception.ResourceNotFoundException;
import com.example.storex.model.dto.ProductRequestDto;
import com.example.storex.model.dto.ProductResponseDto;
import com.example.storex.model.entity.Product;
import com.example.storex.repository.ProductRepository;
import com.example.storex.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'all'")
    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "product", key = "#id")
    public ProductResponseDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToResponseDto(product);
    }

    @Override
    @CacheEvict(value = "products", key = "'all'")
    public ProductResponseDto createProduct(ProductRequestDto requestDto) {
        Product product = Product.builder()
                .name(requestDto.getName())
                .price(requestDto.getPrice())
                .quantity(requestDto.getQuantity())
                .description(requestDto.getDescription())
                .build();
        Product saved = productRepository.save(product);
        return mapToResponseDto(saved);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "products", key = "'all'"),
            @CacheEvict(value = "product", key = "#id")
    })
    public ProductResponseDto updateProduct(Long id, ProductRequestDto requestDto) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        existing.setName(requestDto.getName());
        existing.setPrice(requestDto.getPrice());
        existing.setQuantity(requestDto.getQuantity());
        existing.setDescription(requestDto.getDescription());

        Product updated = productRepository.save(existing);
        return mapToResponseDto(updated);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "products", key = "'all'"),
            @CacheEvict(value = "product", key = "#id")
    })
    public void deleteProduct(Long id) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        productRepository.delete(existing);
    }

    private ProductResponseDto mapToResponseDto(Product product) {
        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .description(product.getDescription())
                .build();
    }
}
