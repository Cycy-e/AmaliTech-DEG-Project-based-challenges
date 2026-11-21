package com.finsafe.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsafe.gateway.model.PaymentRequest;
import com.finsafe.gateway.service.IdempotencyService;
import com.finsafe.gateway.service.IdempotencyService.Outcome;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class PaymentController {

    static final String KEY_HEADER = "Idempotency-Key";
    static final int MAX_KEY_LENGTH = 255;

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public PaymentController(IdempotencyService idempotencyService, ObjectMapper objectMapper, Validator validator) {
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @PostMapping("/process-payment")
    public ResponseEntity<Map<String, Object>> processPayment(
            @RequestHeader(value = KEY_HEADER, required = false) String key,
            @RequestBody Map<String, Object> body) {

        if (key == null || key.isBlank()) {
            throw new BadRequestException("Missing required header: " + KEY_HEADER);
        }
        if (key.length() > MAX_KEY_LENGTH) {
            throw new BadRequestException(KEY_HEADER + " must be at most " + MAX_KEY_LENGTH + " characters");
        }

        PaymentRequest request = toPaymentRequest(body);
        Outcome outcome = idempotencyService.handle(key.trim(), body, request);

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(outcome.response().status());
        if (outcome.replayed()) {
            builder.header("X-Cache-Hit", "true");
        }
        return builder.body(outcome.response().body());
    }

    private PaymentRequest toPaymentRequest(Map<String, Object> body) {
        PaymentRequest request;
        try {
            request = objectMapper.convertValue(body, PaymentRequest.class);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid request body");
        }
        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .sorted()
                    .collect(Collectors.joining("; "));
            throw new BadRequestException(msg);
        }
        return request;
    }

    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) {
            super(message);
        }
    }
}
