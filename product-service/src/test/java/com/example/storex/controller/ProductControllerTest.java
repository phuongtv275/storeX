package com.example.storex.controller;

import com.example.storex.exception.GlobalExceptionHandler;
import com.example.storex.exception.ResourceNotFoundException;
import com.example.storex.model.dto.ProductRequestDto;
import com.example.storex.model.dto.ProductResponseDto;
import com.example.storex.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    @DisplayName("GET /api/products returns 200 OK with product list")
    void shouldGetAllProducts() throws Exception {
        ProductResponseDto product = ProductResponseDto.builder()
                .id(1L)
                .name("Monitor")
                .price(new BigDecimal("299.99"))
                .quantity(15)
                .description("4K IPS Display")
                .build();

        when(productService.getAllProducts()).thenReturn(List.of(product));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Monitor"))
                .andExpect(jsonPath("$[0].price").value(299.99))
                .andExpect(jsonPath("$[0].quantity").value(15))
                .andExpect(jsonPath("$[0].description").value("4K IPS Display"));
    }

    @Test
    @DisplayName("GET /api/products/1 returns 200 OK with product")
    void shouldGetProductById() throws Exception {
        ProductResponseDto product = ProductResponseDto.builder()
                .id(1L)
                .name("Monitor")
                .price(new BigDecimal("299.99"))
                .quantity(15)
                .description("4K IPS Display")
                .build();

        when(productService.getProductById(1L)).thenReturn(product);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Monitor"));
    }

    @Test
    @DisplayName("GET /api/products/999 returns 404 Not Found when product not found")
    void shouldReturn404WhenNotFound() throws Exception {
        when(productService.getProductById(999L))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 999"));

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Product not found with id: 999"));
    }

    @Test
    @DisplayName("POST /api/products returns 201 Created with created product")
    void shouldCreateProduct() throws Exception {
        ProductResponseDto responseDto = ProductResponseDto.builder()
                .id(1L)
                .name("Monitor")
                .price(new BigDecimal("299.99"))
                .quantity(15)
                .description("4K IPS Display")
                .build();

        when(productService.createProduct(any(ProductRequestDto.class))).thenReturn(responseDto);

        String jsonPayload = """
                {
                    "name": "Monitor",
                    "price": 299.99,
                    "quantity": 15,
                    "description": "4K IPS Display"
                }
                """;

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Monitor"));
    }

    @Test
    @DisplayName("PUT /api/products/1 returns 200 OK with updated product")
    void shouldUpdateProduct() throws Exception {
        ProductResponseDto responseDto = ProductResponseDto.builder()
                .id(1L)
                .name("Monitor Ultra")
                .price(new BigDecimal("349.99"))
                .quantity(12)
                .description("4K OLED Display")
                .build();

        when(productService.updateProduct(eq(1L), any(ProductRequestDto.class))).thenReturn(responseDto);

        String updatePayload = """
                {
                    "name": "Monitor Ultra",
                    "price": 349.99,
                    "quantity": 12,
                    "description": "4K OLED Display"
                }
                """;

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Monitor Ultra"))
                .andExpect(jsonPath("$.price").value(349.99));
    }

    @Test
    @DisplayName("DELETE /api/products/1 returns 204 No Content")
    void shouldDeleteProduct() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/products returns 400 Bad Request when request body has invalid fields")
    void shouldReturn400WhenCreatingProductWithInvalidFields() throws Exception {
        String invalidPayload = """
                {
                    "name": "",
                    "price": -10.00,
                    "quantity": -5,
                    "description": "Invalid product"
                }
                """;

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("Unhandled exceptions return 500 with generic error message")
    void shouldReturnGenericMessageOnInternalServerError() throws Exception {
        when(productService.getAllProducts()).thenThrow(new RuntimeException("Database secret credentials leaked!"));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An internal error occurred. Please contact support."));
    }
}
