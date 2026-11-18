package com.example.storex.controller;

import com.example.storex.exception.GlobalExceptionHandler;
import com.example.storex.exception.ResourceNotFoundException;
import com.example.storex.model.dto.PaymentRequestDto;
import com.example.storex.model.dto.PaymentResponseDto;
import com.example.storex.service.PaymentService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    @DisplayName("GET /api/payments returns 200 OK with payment list")
    void shouldGetAllPayments() throws Exception {
        PaymentResponseDto payment = PaymentResponseDto.builder()
                .id(1L)
                .orderId(101L)
                .amount(new BigDecimal("150.00"))
                .paymentMethod("CREDIT_CARD")
                .status("COMPLETED")
                .build();

        when(paymentService.getAllPayments()).thenReturn(List.of(payment));

        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].orderId").value(101))
                .andExpect(jsonPath("$[0].amount").value(150.00))
                .andExpect(jsonPath("$[0].paymentMethod").value("CREDIT_CARD"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /api/payments/1 returns 200 OK with payment")
    void shouldGetPaymentById() throws Exception {
        PaymentResponseDto payment = PaymentResponseDto.builder()
                .id(1L)
                .orderId(101L)
                .amount(new BigDecimal("150.00"))
                .paymentMethod("CREDIT_CARD")
                .status("COMPLETED")
                .build();

        when(paymentService.getPaymentById(1L)).thenReturn(payment);

        mockMvc.perform(get("/api/payments/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value(101))
                .andExpect(jsonPath("$.amount").value(150.00));
    }

    @Test
    @DisplayName("GET /api/payments/999 returns 404 Not Found when payment not found")
    void shouldReturn404WhenNotFound() throws Exception {
        when(paymentService.getPaymentById(999L))
                .thenThrow(new ResourceNotFoundException("Payment not found with id: 999"));

        mockMvc.perform(get("/api/payments/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Payment not found with id: 999"));
    }

    @Test
    @DisplayName("POST /api/payments returns 201 Created with created payment")
    void shouldCreatePayment() throws Exception {
        PaymentResponseDto responseDto = PaymentResponseDto.builder()
                .id(1L)
                .orderId(101L)
                .amount(new BigDecimal("150.00"))
                .paymentMethod("CREDIT_CARD")
                .status("PENDING")
                .build();

        when(paymentService.createPayment(any(PaymentRequestDto.class))).thenReturn(responseDto);

        String json = """
                {
                    "orderId": 101,
                    "amount": 150.00,
                    "paymentMethod": "CREDIT_CARD",
                    "status": "PENDING"
                }
                """;

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value(101))
                .andExpect(jsonPath("$.amount").value(150.00))
                .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/payments returns 400 Bad Request when validation fails")
    void shouldReturn400WhenValidationFails() throws Exception {
        String invalidJson = """
                {
                    "orderId": null,
                    "amount": -10.00,
                    "paymentMethod": "",
                    "status": ""
                }
                """;

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("PUT /api/payments/1 returns 200 OK with updated payment")
    void shouldUpdatePayment() throws Exception {
        PaymentResponseDto updated = PaymentResponseDto.builder()
                .id(1L)
                .orderId(101L)
                .amount(new BigDecimal("200.00"))
                .paymentMethod("BANK_TRANSFER")
                .status("COMPLETED")
                .build();

        when(paymentService.updatePayment(eq(1L), any(PaymentRequestDto.class))).thenReturn(updated);

        String updateJson = """
                {
                    "orderId": 101,
                    "amount": 200.00,
                    "paymentMethod": "BANK_TRANSFER",
                    "status": "COMPLETED"
                }
                """;

        mockMvc.perform(put("/api/payments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(200.00))
                .andExpect(jsonPath("$.paymentMethod").value("BANK_TRANSFER"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("PUT /api/payments/999 returns 404 when payment not found")
    void shouldReturn404WhenUpdatingNotFound() throws Exception {
        when(paymentService.updatePayment(eq(999L), any(PaymentRequestDto.class)))
                .thenThrow(new ResourceNotFoundException("Payment not found with id: 999"));

        String updateJson = """
                {
                    "orderId": 101,
                    "amount": 200.00,
                    "paymentMethod": "BANK_TRANSFER",
                    "status": "COMPLETED"
                }
                """;

        mockMvc.perform(put("/api/payments/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE /api/payments/1 returns 204 No Content")
    void shouldDeletePayment() throws Exception {
        doNothing().when(paymentService).deletePayment(1L);

        mockMvc.perform(delete("/api/payments/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/payments/999 returns 404 when payment not found")
    void shouldReturn404WhenDeletingNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Payment not found with id: 999"))
                .when(paymentService).deletePayment(999L);

        mockMvc.perform(delete("/api/payments/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
