package com.netcracker.cloud.security.core.utils.k8s.impl;

import com.netcracker.cloud.security.core.utils.k8s.KubernetesDefaultToken;
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
import java.util.HashMap;
import java.util.Map;

import static com.netcracker.cloud.security.core.utils.k8s.impl.WatchingTokenSource.*;
import static org.junit.jupiter.api.Assertions.*;

class WatchingTokenSourceTest {

    private final RetryPolicy<?> retryPolicy = new RetryPolicy<>()
            .withDelay(Duration.ofMillis(5))
            .withMaxDuration(Duration.ofSeconds(1));

    @Test
    void getToken(@TempDir Path storageRoot) throws Exception {
        var interval = Duration.ofMillis(10);
        updateToken(storageRoot, "dbaas", "token1");

        try(var ts = new WatchingTokenSource(storageRoot, interval, KubernetesProjectedVolumeWatcher.EXECUTOR)) {
            assertEquals("token1", ts.getToken("dbaas"));

            // test update
            updateToken(storageRoot, "dbaas", "token2");
            Failsafe.with(retryPolicy).run(() -> assertEquals("token2", ts.getToken("dbaas")));
        }
    }

    @Test
    void testDefaultConstructor(@TempDir Path storageRoot) throws Exception {
        var props = new HashMap<String, String>();
        props.put(TOKENS_DIR_PROP, storageRoot.toString());
        props.put("com.netcracker.cloud.security.kubernetes.serviceaccount.dir", storageRoot.toString());
        props.put(POLLING_INTERVAL_PROP, "PT0.010S");
        withProperty(props, () -> {
                    System.out.println("inside with property");
                    System.out.println(KubernetesDefaultToken.getStorageRoot());
                    updateToken(storageRoot, "dbaas", "token1");

                    try(var ts = new WatchingTokenSource()) {
                        assertEquals("token1", ts.getToken("dbaas"));
                        Failsafe.with(retryPolicy).run(() -> assertEquals("token1", KubernetesDefaultToken.getToken()));

                        // test update
                        updateToken(storageRoot, "dbaas", "token2");
                        Failsafe.with(retryPolicy).run(() -> assertEquals("token2", ts.getToken("dbaas")));
                        Failsafe.with(retryPolicy).run(() -> assertEquals("token2", KubernetesDefaultToken.getToken()));
                    }
                }
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
        // Create the default service account token file with sample token content
        Files.writeString(storageRoot.resolve("token"), token);

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

    void withProperty(Map<String, String> properties, OmnivoreRunnable runnable) throws Exception {
        var previousValues = new HashMap<String, String>();
        properties.forEach((name, value) -> {
            previousValues.put(name, System.getProperty(name));
            System.out.println("set property");
            System.out.println(name);
            System.out.println(value);
            System.setProperty(name, value);
        });
        try {
            runnable.run();
        } finally {
            previousValues.forEach((name, previousValue) -> {
                System.out.println("return property");
                System.out.println(name);
                System.out.println(previousValue);
                if (previousValue != null) {
                    System.setProperty(name, previousValue);
                } else {
                    System.clearProperty(name);
                }
            });
        }
    }

    @FunctionalInterface
    interface OmnivoreRunnable  {
        void run() throws Exception;
    }
}
