package com.netcracker.cloud.security.core.utils.k8s;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.netcracker.cloud.security.core.utils.k8s.WatchingTokenSource.POLLING_INTERVAL_PROP;
import static com.netcracker.cloud.security.core.utils.k8s.WatchingTokenSource.TOKENS_DIR_PROP;
import static org.junit.jupiter.api.Assertions.*;

class WatchingTokenSourceTest {

    private final RetryPolicy<?> retryPolicy = new RetryPolicy<>()
            .withDelay(Duration.ofMillis(5))
            .withMaxDuration(Duration.ofSeconds(1));

    @Test
    void getToken(@TempDir Path storageRoot) throws Exception {
        var interval = Duration.ofMillis(10);
        updateToken(storageRoot, "dbaas", "token1");

        try (var ts = new WatchingTokenSource(storageRoot, interval)) {
            assertEquals("token1", ts.getToken("dbaas"));

            // test update
            updateToken(storageRoot, "dbaas", "token2");
            Failsafe.with(retryPolicy).run(() -> assertEquals("token2", ts.getToken("dbaas")));
        }
    }

    @Test
    void testDefaultConstructor(@TempDir Path storageRoot) throws Exception {
        withProperty(TOKENS_DIR_PROP, storageRoot.toString(), () ->
            withProperty(POLLING_INTERVAL_PROP, "PT0.010S", () -> {
                updateToken(storageRoot, "dbaas", "token1");

                try (var ts = new WatchingTokenSource()) {
                    assertEquals("token1", ts.getToken("dbaas"));

                    // test update
                    updateToken(storageRoot, "dbaas", "token2");
                    Failsafe.with(retryPolicy).run(() -> assertEquals("token2", ts.getToken("dbaas")));
                }
            })
        );
    }

    /**
     * Creates a Kubernetes-style token storage structure with symlinks.
     * Structure:
     * <pre>
     * - storageRoot/
     *   - ..{timestamp}/
     *     - {audience}/
     *       - token (file with token content)
     *   - ..data -> ..{timestamp} (symlink)
     *   - {audience} -> ..data/{audience} (symlink)
     * </pre>
     *
     * @param storageRoot the root directory for token storage
     * @param audience    the audience name (e.g., "dbaas", "maas")
     * @throws IOException if file operations fail
     */
    private void updateToken(Path storageRoot, String audience, String token) throws Exception {
        var timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss"));
        var timestampDir = storageRoot.resolve(".." + timestamp + "." + System.nanoTime());

        // Create the audience subdirectory
        var audienceDir = timestampDir.resolve(audience);
        Files.createDirectories(audienceDir);

        // Create the token file with sample token content
        var tokenFile = audienceDir.resolve("token");
        Files.writeString(tokenFile, token);

        // Create ..data symlink pointing to timestamp directory
        var dataLink = storageRoot.resolve("..data");
        Files.deleteIfExists(dataLink);
        Files.createSymbolicLink(dataLink, timestampDir.getFileName());

        // Create audience symlink pointing to ..data/audience
        var audienceLink = storageRoot.resolve(audience);

        if (!Files.exists(audienceLink)) {
            Files.createSymbolicLink(audienceLink, dataLink.resolve(audience));
        }
    }

    void withProperty(String name, String value, OmnivoreRunnable runnable) throws Exception {
        var previousValue = System.getProperty(name);
        System.setProperty(name, value);
        try {
            runnable.run();
        } finally {
            if (previousValue != null) {
                System.setProperty(name, previousValue);
            } else {
                System.clearProperty(name);
            }
        }
    }

    @FunctionalInterface
    interface OmnivoreRunnable  {
        void run() throws Exception;
    }
}
