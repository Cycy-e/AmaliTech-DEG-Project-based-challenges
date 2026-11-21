package com.finsafe.gateway;

import com.finsafe.gateway.model.StoredResponse;
import com.finsafe.gateway.store.IdempotencyRecord;
import com.finsafe.gateway.store.IdempotencyStore;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyStoreTest {

    @Test
    void completedKeysExpireAfterTtl() throws InterruptedException {
        IdempotencyStore store = new IdempotencyStore(Duration.ofMillis(50));
        IdempotencyRecord rec = new IdempotencyRecord("hash");
        assertThat(store.putIfAbsent("k", rec)).isNull();
        rec.getResult().complete(new StoredResponse(201, Map.of()));

        Thread.sleep(100);
        store.evictExpired();
        assertThat(store.size()).isZero();
    }

    @Test
    void inFlightKeysAreNeverEvicted() throws InterruptedException {
        IdempotencyStore store = new IdempotencyStore(Duration.ofMillis(50));
        store.putIfAbsent("k", new IdempotencyRecord("hash"));

        Thread.sleep(100);
        store.evictExpired();
        assertThat(store.size()).isEqualTo(1);
    }
}
