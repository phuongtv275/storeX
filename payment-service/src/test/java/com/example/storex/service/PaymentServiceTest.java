package com.example.storex.service;

import com.example.storex.exception.ResourceNotFoundException;
import com.example.storex.model.dto.PaymentRequestDto;
import com.example.storex.model.dto.PaymentResponseDto;
import com.example.storex.model.entity.Payment;
import com.example.storex.repository.PaymentRepository;
import com.example.storex.service.impl.PaymentServiceImpl;
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
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment samplePayment;
    private PaymentRequestDto sampleRequest;

    @BeforeEach
    void setUp() {
        samplePayment = Payment.builder()
                .id(1L)
                .orderId(100L)
                .amount(new BigDecimal("99.99"))
                .paymentMethod("CREDIT_CARD")
                .status("PENDING")
                .build();

        sampleRequest = PaymentRequestDto.builder()
                .orderId(100L)
                .amount(new BigDecimal("99.99"))
                .paymentMethod("CREDIT_CARD")
                .status("PENDING")
                .build();
    }

    @Test
    @DisplayName("getAllPayments returns all payment DTOs")
    void shouldGetAllPayments() {
        when(paymentRepository.findAll()).thenReturn(List.of(samplePayment));

        List<PaymentResponseDto> result = paymentService.getAllPayments();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderId()).isEqualTo(100L);
        assertThat(result.get(0).getAmount()).isEqualByComparingTo("99.99");
        assertThat(result.get(0).getPaymentMethod()).isEqualTo("CREDIT_CARD");
        verify(paymentRepository).findAll();
    }

    @Test
    @DisplayName("getPaymentById returns payment DTO when found")
    void shouldGetPaymentByIdWhenFound() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(samplePayment));

        PaymentResponseDto result = paymentService.getPaymentById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getOrderId()).isEqualTo(100L);
        verify(paymentRepository).findById(1L);
    }

    @Test
    @DisplayName("getPaymentById throws ResourceNotFoundException when not found")
    void shouldThrowWhenPaymentNotFound() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Payment not found with id: 99");
    }

    @Test
    @DisplayName("createPayment saves entity and returns response DTO")
    void shouldCreatePayment() {
        when(paymentRepository.save(any(Payment.class))).thenReturn(samplePayment);

        PaymentResponseDto result = paymentService.createPayment(sampleRequest);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getOrderId()).isEqualTo(100L);
        assertThat(result.getAmount()).isEqualByComparingTo("99.99");
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("updatePayment modifies existing entity and returns updated DTO")
    void shouldUpdatePayment() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(samplePayment));
        Payment updatedPayment = Payment.builder()
                .id(1L)
                .orderId(100L)
                .amount(new BigDecimal("120.00"))
                .paymentMethod("BANK_TRANSFER")
                .status("COMPLETED")
                .build();
        when(paymentRepository.save(any(Payment.class))).thenReturn(updatedPayment);

        PaymentRequestDto updateRequest = PaymentRequestDto.builder()
                .orderId(100L)
                .amount(new BigDecimal("120.00"))
                .paymentMethod("BANK_TRANSFER")
                .status("COMPLETED")
                .build();

        PaymentResponseDto result = paymentService.updatePayment(1L, updateRequest);

        assertThat(result.getAmount()).isEqualByComparingTo("120.00");
        assertThat(result.getPaymentMethod()).isEqualTo("BANK_TRANSFER");
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        verify(paymentRepository).save(samplePayment);
    }

    @Test
    @DisplayName("updatePayment throws ResourceNotFoundException when payment not found")
    void shouldThrowWhenUpdatingNonExistingPayment() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.updatePayment(99L, sampleRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Payment not found with id: 99");
    }

    @Test
    @DisplayName("deletePayment removes entity when found")
    void shouldDeletePayment() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(samplePayment));
        doNothing().when(paymentRepository).delete(samplePayment);

        paymentService.deletePayment(1L);

        verify(paymentRepository).delete(samplePayment);
    }

    @Test
    @DisplayName("deletePayment throws ResourceNotFoundException when not found")
    void shouldThrowWhenDeletingNonExistingPayment() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.deletePayment(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Payment not found with id: 99");
    }
}
