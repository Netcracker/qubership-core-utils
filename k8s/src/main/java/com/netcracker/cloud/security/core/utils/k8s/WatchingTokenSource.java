package com.netcracker.cloud.security.core.utils.k8s;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.temporal.ChronoUnit.NANOS;

@Slf4j
@Priority(0)
public class WatchingTokenSource  implements TokenSource {
    // TODO align system property name to all other declared in cloud code
    public static final String TOKENS_DIR_PROP = "com.netcracker.cloud.security.kubernetes.tokens.dir";
    public static final String SERVICE_ACCOUNT_DIR_PROP = "com.netcracker.cloud.security.kubernetes.serviceaccount.dir";
    public static final Path TOKENS_DIR_DEFAULT = Paths.get("/var/run/secrets/tokens");
    public static final Path SERVICE_ACCOUNT_DIR_DEFAULT = Paths.get("/var/run/secrets/kubernetes.io/serviceaccount");

    // TODO align system property name to all other declared in cloud code
    public static final String POLLING_INTERVAL_PROP = "com.netcracker.cloud.security.kubernetes.tokens.polling.interval";
    // In practice, the refresh happens at about 80% of the expiration time.
    // And taking into account that minimal update interval is 10 min, I choose 1 min
    public static final Duration POLLING_INTERVAL_DEFAULT = Duration.ofMinutes(1);

    private static final Pattern TOKEN_PATH_MATCHER = Pattern.compile("([^./]+)/token");
    private static final String DEFAULT_TOKEN_AUDIENCE = "oidc-token";

    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<String, Try<String>> cache = new ConcurrentHashMap<>();
    private final Path storageRoot;
    private final Path serviceAccountDir;
    private final WatchService watchService;

    // default constructor for service-loader
    public WatchingTokenSource() {
        this(
                Optional.ofNullable(System.getProperty(TOKENS_DIR_PROP))
                        .map(Paths::get)
                        .orElse(TOKENS_DIR_DEFAULT),
                Optional.ofNullable(System.getProperty(SERVICE_ACCOUNT_DIR_PROP))
                        .map(Paths::get)
                        .orElse(SERVICE_ACCOUNT_DIR_DEFAULT),
                Optional.ofNullable(System.getProperty(POLLING_INTERVAL_PROP))
                        .map(Duration::parse)
                        .orElse(POLLING_INTERVAL_DEFAULT)
        );
    }

    @SneakyThrows
    public WatchingTokenSource(Path tokensDir, Path serviceAccountDir, Duration interval) {
        log.info("Start token source for {}", tokensDir);
        storageRoot = tokensDir;
        this.serviceAccountDir = serviceAccountDir;

        watchService = FileSystems.getDefault().newWatchService();
        storageRoot.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
        this.serviceAccountDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

        scheduler = getScheduledExecutorService();
        scheduler.scheduleAtFixedRate(this::processFilesystemEvents, interval.get(ChronoUnit.NANOS), interval.get(NANOS), TimeUnit.NANOSECONDS);
        updateCache();
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
                        if (entityPath.endsWith("..data")) {
                            needUpdate = true;
                            // do not break loop here because we need to process all events from
                            // watchService.poll() to avoid memory leak
                        }
                    }
                }
                key.reset();
            }

            if (needUpdate) {
                updateCache();
            }
        } catch (Exception e) {
            log.error("Error update tokens cache", e);
        }
    }

    private void updateCache() {
        try (var stream = Files.walk(storageRoot, FileVisitOption.FOLLOW_LINKS)) {
            stream
                    .map(storageRoot::relativize)
                    .map(Path::toString)
                    .map(TOKEN_PATH_MATCHER::matcher)
                    .filter(Matcher::matches)
                    .forEach(match -> {
                        var audience = match.group(1);
                        log.debug("Update cache for audience: {}", audience);
                        updateCache(audience, storageRoot.resolve(audience));
                    });
            updateCache(DEFAULT_TOKEN_AUDIENCE, serviceAccountDir);
        } catch (IOException e) {
            log.error("Cannot list folder: {}",  storageRoot);
        }
    }
    private void updateCache(String audience, Path tokenDir) {
        cache.put(
                audience,
                Try.of(() -> Files.readString(tokenDir.resolve("token")))
        );
    }

    @Override
    public String getToken(String audience) {
        return cache.getOrDefault(
                audience,
                Try.failure(new IllegalArgumentException("Unknown token audience: " + audience))
            ).getOrThrow();
    }

    @Override
    public String getDefaultToken() {
        return getToken(DEFAULT_TOKEN_AUDIENCE);
    }

    // this method can be used to override thread management
    protected ScheduledExecutorService getScheduledExecutorService() {
        return Executors.newScheduledThreadPool(1);
    }

    @Override
    public void close() throws Exception {
        log.info("Stop scheduler");
        scheduler.shutdown();
        watchService.close();
    }
}
