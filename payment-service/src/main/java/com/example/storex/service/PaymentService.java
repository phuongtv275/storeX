package com.example.storex.service;

import com.example.storex.model.dto.PaymentRequestDto;
import com.example.storex.model.dto.PaymentResponseDto;

import java.util.List;

public interface PaymentService {
    List<PaymentResponseDto> getAllPayments();
    PaymentResponseDto getPaymentById(Long id);
    PaymentResponseDto createPayment(PaymentRequestDto requestDto);
    PaymentResponseDto updatePayment(Long id, PaymentRequestDto requestDto);
    void deletePayment(Long id);
}
