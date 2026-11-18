package com.example.storex.service.impl;

import com.example.storex.exception.ResourceNotFoundException;
import com.example.storex.model.dto.PaymentRequestDto;
import com.example.storex.model.dto.PaymentResponseDto;
import com.example.storex.model.entity.Payment;
import com.example.storex.repository.PaymentRepository;
import com.example.storex.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "payments", key = "'all'")
    public List<PaymentResponseDto> getAllPayments() {
        return paymentRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "payment", key = "#id")
    public PaymentResponseDto getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        return mapToResponseDto(payment);
    }

    @Override
    @CacheEvict(value = "payments", key = "'all'")
    public PaymentResponseDto createPayment(PaymentRequestDto requestDto) {
        Payment payment = Payment.builder()
                .orderId(requestDto.getOrderId())
                .amount(requestDto.getAmount())
                .paymentMethod(requestDto.getPaymentMethod())
                .status(requestDto.getStatus())
                .build();
        Payment saved = paymentRepository.save(payment);
        return mapToResponseDto(saved);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "payments", key = "'all'"),
            @CacheEvict(value = "payment", key = "#id")
    })
    public PaymentResponseDto updatePayment(Long id, PaymentRequestDto requestDto) {
        Payment existing = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));

        existing.setOrderId(requestDto.getOrderId());
        existing.setAmount(requestDto.getAmount());
        existing.setPaymentMethod(requestDto.getPaymentMethod());
        existing.setStatus(requestDto.getStatus());

        Payment updated = paymentRepository.save(existing);
        return mapToResponseDto(updated);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "payments", key = "'all'"),
            @CacheEvict(value = "payment", key = "#id")
    })
    public void deletePayment(Long id) {
        Payment existing = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        paymentRepository.delete(existing);
    }

    private PaymentResponseDto mapToResponseDto(Payment payment) {
        return PaymentResponseDto.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .build();
    }
}
