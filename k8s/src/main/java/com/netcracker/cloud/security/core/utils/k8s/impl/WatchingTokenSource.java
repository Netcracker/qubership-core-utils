package com.netcracker.cloud.security.core.utils.k8s.impl;

import com.netcracker.cloud.security.core.utils.k8s.Priority;
import com.netcracker.cloud.security.core.utils.k8s.TokenSource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Priority(0)
public class WatchingTokenSource  implements TokenSource {
    public static final String TOKENS_DIR_PROP = "com.netcracker.cloud.security.kubernetes.tokens.dir";
    public static final Path TOKENS_DIR_DEFAULT = Paths.get("/var/run/secrets/tokens");

    public static final String POLLING_INTERVAL_PROP = "com.netcracker.cloud.security.kubernetes.tokens.polling.interval";
    // In practice, the refresh happens at about 80% of the expiration time.
    // And taking into account that minimal update interval is 10 min, I choose 1 min
    public static final Duration POLLING_INTERVAL_DEFAULT = Duration.ofMinutes(1);

    private static final Pattern TOKEN_PATH_MATCHER = Pattern.compile("([^./]+)/token");

    private final ConcurrentHashMap<String, Try<String>> cache = new ConcurrentHashMap<>();
    private final KubernetesProjectedVolumeWatcher watcher;

    // default constructor for service-loader
    public WatchingTokenSource() {
        this(
                Optional.ofNullable(System.getProperty(TOKENS_DIR_PROP))
                        .map(Paths::get)
                        .orElse(TOKENS_DIR_DEFAULT),
                Optional.ofNullable(System.getProperty(POLLING_INTERVAL_PROP))
                        .map(Duration::parse)
                        .orElse(POLLING_INTERVAL_DEFAULT),
                KubernetesProjectedVolumeWatcher.EXECUTOR
        );
    }

    @SneakyThrows
    public WatchingTokenSource(Path storageRoot, Duration interval, ScheduledExecutorService scheduler) {
        log.info("Start token source for {}", storageRoot);
        this.watcher = new KubernetesProjectedVolumeWatcher(storageRoot, interval, scheduler, this::updateCache);
    }

	/**
     * getToken method returns the corresponding Kubernetes projected volume token string by audience
     */
    @Override
    public String getToken(String audience) {
        return cache.getOrDefault(
                audience,
                Try.failure(new IllegalArgumentException("Unknown token audience: " + audience))
        ).getOrThrow();
    }

    private void updateCache(Path storageRoot) {
        try (var stream = Files.walk(storageRoot, FileVisitOption.FOLLOW_LINKS)) {
            stream
                    .map(storageRoot::relativize)
                    .map(Path::toString)
                    .map(TOKEN_PATH_MATCHER::matcher)
                    .filter(Matcher::matches)
                    .forEach(match -> {
                        var audience = match.group(1);
                        log.debug("Update cache for audience: {}", audience);
                        refreshToken(audience, storageRoot.resolve(audience));
                    });
        } catch (IOException e) {
            log.error("Cannot list folder: {}",  storageRoot);
        }
    }

    private void refreshToken(String audience, Path tokenDir) {
        cache.put(
                audience,
                Try.of(() -> Files.readString(tokenDir.resolve("token")))
        );
    }

    @Override
    public void close() {
        watcher.close();
    }
}
