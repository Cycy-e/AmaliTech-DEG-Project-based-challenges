package com.finsafe.gateway.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IdempotencyStore {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyStore.class);

    private final Map<String, IdempotencyRecord> records = new ConcurrentHashMap<>();
    private final Duration ttl;

    public IdempotencyStore(@Value("${idempotency.key-ttl}") Duration ttl) {
        this.ttl = ttl;
    }

    public IdempotencyRecord putIfAbsent(String key, IdempotencyRecord fresh) {
        IdempotencyRecord[] existing = new IdempotencyRecord[1];
        records.compute(key, (k, current) -> {
            if (current != null && !isExpired(current)) {
                existing[0] = current;
                return current;
            }
            return fresh;
        });
        return existing[0];
    }

    public void remove(String key, IdempotencyRecord record) {
        records.remove(key, record);
    }

    public int size() {
        return records.size();
    }

    @Scheduled(fixedDelayString = "${idempotency.cleanup-interval}")
    public void evictExpired() {
        int before = records.size();
        records.entrySet().removeIf(e -> isExpired(e.getValue()));
        int removed = before - records.size();
        if (removed > 0) {
            log.info("Evicted {} expired idempotency key(s)", removed);
        }
    }

    private boolean isExpired(IdempotencyRecord record) {
        return record.isCompleted()
                && record.getCreatedAt().plus(ttl).isBefore(Instant.now());
    }
}
