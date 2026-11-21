package com.finsafe.gateway.store;

import com.finsafe.gateway.model.StoredResponse;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class IdempotencyRecord {

    private final String requestHash;
    private final CompletableFuture<StoredResponse> result = new CompletableFuture<>();
    private final Instant createdAt = Instant.now();

    public IdempotencyRecord(String requestHash) {
        this.requestHash = requestHash;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public CompletableFuture<StoredResponse> getResult() {
        return result;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isCompleted() {
        return result.isDone();
    }
}
