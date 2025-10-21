package com.netcracker.cloud.security.core.utils.k8s.impl;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class CacheRefresherTest {
    @Test
    void testCacheRefresher() {
        var timeSupplier = new AtomicLong(100);
        var updateCacheCallCounter = new AtomicInteger(0);

        var cr = new CacheRefresher<String>(
                    Duration.ofNanos(100),
                    v -> "value" + updateCacheCallCounter.incrementAndGet(),
                    timeSupplier::get
            );

        assertEquals("value1", cr.getCache());
        assertEquals("value1", cr.getCache());

        // move time
        timeSupplier.set(300L);

        assertEquals("value2", cr.getCache());
    }

}
