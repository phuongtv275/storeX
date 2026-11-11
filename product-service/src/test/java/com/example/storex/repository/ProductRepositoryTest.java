package com.example.storex.repository;

import com.example.storex.model.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("Should save and retrieve a product by ID")
    void shouldSaveAndRetrieveProduct() {
        Product product = Product.builder()
                .name("Wireless Mouse")
                .price(new BigDecimal("29.99"))
                .quantity(50)
                .description("Ergonomic Bluetooth mouse")
                .build();

        Product saved = productRepository.save(product);

        assertThat(saved.getId()).isNotNull();
        Optional<Product> found = productRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Wireless Mouse");
        assertThat(found.get().getPrice()).isEqualByComparingTo("29.99");
        assertThat(found.get().getQuantity()).isEqualTo(50);
        assertThat(found.get().getDescription()).isEqualTo("Ergonomic Bluetooth mouse");
    }
}
