package com.finsafe.gateway.service;

import com.finsafe.gateway.model.PaymentRequest;
import com.finsafe.gateway.model.StoredResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);

    private final long delayMs;
    private final AtomicInteger chargeCount = new AtomicInteger();

    public PaymentProcessor(@Value("${payment.processing-delay-ms}") long delayMs) {
        this.delayMs = delayMs;
    }

    public StoredResponse charge(PaymentRequest request) {
        log.info("Processing payment of {} {}", request.amount(), request.currency());
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Payment processing was interrupted", e);
        }
        chargeCount.incrementAndGet();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "success");
        body.put("message", "Charged " + request.amount().toPlainString() + " " + request.currency());
        body.put("transactionId", UUID.randomUUID().toString());
        body.put("amount", request.amount());
        body.put("currency", request.currency());
        body.put("processedAt", Instant.now().toString());
        return new StoredResponse(201, body);
    }

    public int getChargeCount() {
        return chargeCount.get();
    }
}
