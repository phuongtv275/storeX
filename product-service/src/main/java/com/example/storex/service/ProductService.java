package com.example.storex.service;

import com.example.storex.model.dto.ProductRequestDto;
import com.example.storex.model.dto.ProductResponseDto;

import java.util.List;

public interface ProductService {
    List<ProductResponseDto> getAllProducts();
    ProductResponseDto getProductById(Long id);
    ProductResponseDto createProduct(ProductRequestDto requestDto);
    ProductResponseDto updateProduct(Long id, ProductRequestDto requestDto);
    void deleteProduct(Long id);
}
