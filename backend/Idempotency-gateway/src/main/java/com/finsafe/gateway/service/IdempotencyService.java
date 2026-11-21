package com.finsafe.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.finsafe.gateway.model.PaymentRequest;
import com.finsafe.gateway.model.StoredResponse;
import com.finsafe.gateway.store.IdempotencyRecord;
import com.finsafe.gateway.store.IdempotencyStore;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class IdempotencyService {

    private final IdempotencyStore store;
    private final PaymentProcessor processor;
    private final ObjectMapper canonicalMapper;

    public IdempotencyService(IdempotencyStore store, PaymentProcessor processor, ObjectMapper objectMapper) {
        this.store = store;
        this.processor = processor;
        this.canonicalMapper = objectMapper.copy()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    public Outcome handle(String key, Map<String, Object> rawBody, PaymentRequest request) {
        String hash = hash(rawBody);
        IdempotencyRecord fresh = new IdempotencyRecord(hash);
        IdempotencyRecord existing = store.putIfAbsent(key, fresh);

        if (existing != null) {
            if (!existing.getRequestHash().equals(hash)) {
                throw new KeyReuseException();
            }
            return new Outcome(await(existing), true);
        }

        try {
            StoredResponse response = processor.charge(request);
            fresh.getResult().complete(response);
            return new Outcome(response, false);
        } catch (RuntimeException e) {
            store.remove(key, fresh);
            fresh.getResult().completeExceptionally(e);
            throw e;
        }
    }

    private StoredResponse await(IdempotencyRecord record) {
        try {
            return record.getResult().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for original request", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Original request with this key failed, please retry", e.getCause());
        }
    }

    private String hash(Map<String, Object> body) {
        try {
            byte[] json = canonicalMapper.writeValueAsBytes(body);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(json);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Could not hash request body", e);
        }
    }

    public record Outcome(StoredResponse response, boolean replayed) {
    }

    public static class KeyReuseException extends RuntimeException {
        public KeyReuseException() {
            super("Idempotency key already used for a different request body.");
        }
    }
}
