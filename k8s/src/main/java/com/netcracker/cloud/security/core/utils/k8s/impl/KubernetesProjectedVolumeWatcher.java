package com.netcracker.cloud.security.core.utils.k8s.impl;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.time.temporal.ChronoUnit.NANOS;

@Slf4j
public class KubernetesProjectedVolumeWatcher implements AutoCloseable {
    // TODO is lazy really needed here or it is overengineering
    public static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(1);

    // this is link to actual data container. Handle this link update event to update cache
    private static final String UPDATE_MARKER = "..data";

    private final Path storageRoot;
    private final ScheduledExecutorService scheduler;
    private final Consumer<Path> cacheUpdate;
    private final WatchService watchService;

    @SneakyThrows
    public KubernetesProjectedVolumeWatcher(Path storageRoot,
                                            Duration interval,
                                            ScheduledExecutorService scheduler,
                                            Consumer<Path> cacheUpdate) {
        this.storageRoot = storageRoot;
        this.scheduler = scheduler;
        this.cacheUpdate = cacheUpdate;

        watchService = FileSystems.getDefault().newWatchService();
        this.storageRoot.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

        scheduler.scheduleAtFixedRate(this::processFilesystemEvents,
                interval.get(ChronoUnit.NANOS),
                interval.get(NANOS),
                TimeUnit.NANOSECONDS);

        this.cacheUpdate.accept(storageRoot);
    }

    private void processFilesystemEvents() {
        try {
            log.trace("Check fs events queue");
            WatchKey key;
            var needUpdate = false;

            while ((key = watchService.poll()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    log.debug("Watch event received: {}", event.context());
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE && (event.context() instanceof Path)) {
                        var entityPath = (Path)event.context();
                        if (entityPath.endsWith(UPDATE_MARKER)) {
                            needUpdate = true;
                            // do not break loop here because we need to process all events from
                            // watchService.poll() to avoid memory leak
                        }
                    }
                }
                key.reset();
            }

            if (needUpdate) {
                cacheUpdate.accept(storageRoot);
            }
        } catch (Exception e) {
            log.error("Error update tokens cache", e);
        }
    }

    @Override
    public void close() throws Exception {
        scheduler.shutdown();
    }
}
