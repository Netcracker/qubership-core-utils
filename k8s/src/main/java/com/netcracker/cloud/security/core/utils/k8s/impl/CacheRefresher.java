package com.netcracker.cloud.security.core.utils.k8s.impl;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

@Slf4j
public class CacheRefresher<T> {
    private final long refreshInterval;
    private final Function<T, T> updater;
    private final NanoTimeSupplier timeSupplier;
    private final AtomicLong expired = new AtomicLong(0); // force reread cache on first run
    private volatile T cache;

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
        if (expired.get() < now) {
            log.debug("Update cache");

            synchronized (expired) {
                if (expired.get() < now) {
                    cache = updater.apply(cache);
                    expired.set(now +  refreshInterval);
                }
            }
        }

        return cache;
    }

    // introduce interface that supplies java primitive to avoid allocating Long wrapper object on heap
    @FunctionalInterface
    public interface NanoTimeSupplier {
        long get();
    }
}
