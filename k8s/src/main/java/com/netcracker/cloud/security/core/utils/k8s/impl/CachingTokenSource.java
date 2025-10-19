package com.netcracker.cloud.security.core.utils.k8s.impl;

import com.netcracker.cloud.security.core.utils.k8s.Priority;
import com.netcracker.cloud.security.core.utils.k8s.TokenSource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Priority(0)
public class CachingTokenSource implements TokenSource {
    public static final String TOKENS_DIR_PROP = "com.netcracker.cloud.security.kubernetes.tokens.dir";
    public static final Path TOKENS_DIR_DEFAULT = Paths.get("/var/run/secrets/tokens");

    public static final String POLLING_INTERVAL_PROP = "com.netcracker.cloud.security.kubernetes.tokens.polling.interval";
    // In practice, the refresh happens at about 80% of the expiration time.
    // And taking into account that minimal update interval is 10 min, I choose 1 min
    public static final Duration POLLING_INTERVAL_DEFAULT = Duration.ofMinutes(1);

    private static final Pattern TOKEN_PATH_MATCHER = Pattern.compile("([^./]+)/token");

    private final CacheRefresher<HashMap<String, Try<String>>> cacheRefresher;

    // default constructor for service-loader
    public CachingTokenSource() {
        this(
                Optional.ofNullable(System.getProperty(TOKENS_DIR_PROP))
                        .map(Paths::get)
                        .orElse(TOKENS_DIR_DEFAULT),
                Optional.ofNullable(System.getProperty(POLLING_INTERVAL_PROP))
                        .map(Duration::parse)
                        .orElse(POLLING_INTERVAL_DEFAULT)
        );
    }

    @SneakyThrows
    public CachingTokenSource(Path storageRoot, Duration interval) {
        log.info("Start token source for {}", storageRoot);
        this.cacheRefresher = new CacheRefresher<>(interval, v -> updateCache(v, storageRoot));
    }

    /**
     * getToken method returns the corresponding Kubernetes projected volume token string by audience
     *
     * @param audience is the audience of the token to be returned
     * @return Kubernetes projected volume token string
     */
    @Override
    public String getToken(String audience) {
        return cacheRefresher.getCache().getOrDefault(
                audience,
                Try.failure(new IllegalArgumentException("Unknown token audience: " + audience))
        ).getOrThrow();
    }

    private HashMap<String,Try<String>> updateCache(final HashMap<String,Try<String>> cache, Path storageRoot) {
        final HashMap<String,Try<String>> updatedCache = (cache == null) ? new HashMap<>() : cache;

        try (var stream = Files.walk(storageRoot, FileVisitOption.FOLLOW_LINKS)) {
            updatedCache.clear(); // avoiding additional load to GC during update fairly stable buckets structure
            stream
                    .map(storageRoot::relativize)
                    .map(Path::toString)
                    .map(TOKEN_PATH_MATCHER::matcher)
                    .filter(Matcher::matches)
                    .map(m -> m.group(1))
                    .forEach(audience ->  {
                        log.debug("Update cache for audience: {}", audience);
                        var tokenPath = storageRoot.resolve(audience).resolve("token");
                        var token = Try.of(() -> Files.readString(tokenPath));
                        updatedCache.put(audience, token);
                    });
        } catch (IOException e) {
            log.error("Cannot list folder: {}", storageRoot);
        }

        return updatedCache;
    }

    @Override
    public void close() {
        // nothing to do
    }
}
