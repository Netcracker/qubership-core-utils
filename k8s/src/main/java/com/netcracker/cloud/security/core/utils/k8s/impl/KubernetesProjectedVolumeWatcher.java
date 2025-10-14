package com.netcracker.cloud.security.core.utils.k8s.impl;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Slf4j
public class KubernetesProjectedVolumeWatcher implements AutoCloseable {
    public static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(1, runnable -> {
        Thread thread = Executors.defaultThreadFactory().newThread(runnable);
        thread.setName("KubernetesProjectedVolumeWatcher");
        thread.setDaemon(true);
        return thread;
    });

    // this is link to actual data container. Handle this link update event to update cache
    private static final String UPDATE_MARKER = "..data";

    private final Path storageRoot;
    private final Consumer<Path> cacheUpdate;
    private final WatchService watchService;
    private final ScheduledFuture<?> scheduledEventsPollTask;

    @SneakyThrows
    public KubernetesProjectedVolumeWatcher(Path storageRoot,
                                            Duration interval,
                                            ScheduledExecutorService scheduler,
                                            Consumer<Path> cacheUpdate) {
        this.storageRoot = storageRoot;
        this.cacheUpdate = cacheUpdate;

        watchService = FileSystems.getDefault().newWatchService();
        log.info("Register {} in watch service", storageRoot);
        this.storageRoot.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

        log.info("Start watcher for: {}, with interval {} : initialDelay {} and period {}", storageRoot, interval, interval.toNanos(), interval.toNanos());
        this.scheduledEventsPollTask = scheduler.scheduleAtFixedRate(this::processFilesystemEvents,
                interval.toNanos(),
                interval.toNanos(),
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
                        var entityPath = (Path) event.context();
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
    @SneakyThrows
    public void close() {
        log.info("Watcher closed for: {}", storageRoot);
        scheduledEventsPollTask.cancel(false);
        watchService.close();
    }
}
