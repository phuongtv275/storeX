package com.example.storex.repository;

import com.example.storex.model.entity.Payment;
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
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("Should save and retrieve a payment by ID")
    void shouldSaveAndRetrievePayment() {
        Payment payment = Payment.builder()
                .orderId(101L)
                .amount(new BigDecimal("150.50"))
                .paymentMethod("CREDIT_CARD")
                .status("PENDING")
                .build();

        Payment saved = paymentRepository.save(payment);

        assertThat(saved.getId()).isNotNull();
        Optional<Payment> found = paymentRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getOrderId()).isEqualTo(101L);
        assertThat(found.get().getAmount()).isEqualByComparingTo("150.50");
        assertThat(found.get().getPaymentMethod()).isEqualTo("CREDIT_CARD");
        assertThat(found.get().getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Should delete payment by ID")
    void shouldDeletePayment() {
        Payment payment = Payment.builder()
                .orderId(102L)
                .amount(new BigDecimal("50.00"))
                .paymentMethod("COD")
                .status("COMPLETED")
                .build();

        Payment saved = paymentRepository.save(payment);
        Long id = saved.getId();

        paymentRepository.deleteById(id);

        Optional<Payment> found = paymentRepository.findById(id);
        assertThat(found).isEmpty();
    }
}
