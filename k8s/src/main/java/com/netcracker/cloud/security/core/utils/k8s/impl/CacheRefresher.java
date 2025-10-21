package com.netcracker.cloud.security.core.utils.k8s.impl;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@Slf4j
public class CacheRefresher<T> {
    private final long refreshInterval;
    private final Function<T, T> updater;
    private final NanoTimeSupplier timeSupplier;
    private final AtomicLong expirationTime = new AtomicLong(0); // force reread cache on first run
    private final AtomicReference<T> cache = new AtomicReference<>();

    public CacheRefresher(Duration refreshInterval, Function<T, T> updater) {
        this(refreshInterval, updater, System::nanoTime);
    }

    CacheRefresher(Duration refreshInterval, Function<T, T> updater, NanoTimeSupplier timeSupplier) {
        this.refreshInterval = refreshInterval.toNanos();
        this.updater = updater;
        this.timeSupplier = timeSupplier;
    }

    public T getCache() {
        var now = timeSupplier.get();
        if (expirationTime.get() < now) {
            log.debug("Update cache");

            synchronized (expirationTime) {
                if (expirationTime.get() < now) {
                    cache.set(updater.apply(cache.get()));
                    expirationTime.set(now + refreshInterval);
                }
            }
        }

        return cache.get();
    }

    // introduce interface that supplies java primitive to avoid allocating Long wrapper object on heap
    @FunctionalInterface
    public interface NanoTimeSupplier {
        long get();
    }
}
