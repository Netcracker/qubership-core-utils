package com.netcracker.cloud.security.core.utils.k8s.impl;

import com.netcracker.cloud.security.core.utils.k8s.FilesUtils;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import static com.netcracker.cloud.security.core.utils.k8s.FilesUtils.tryCreateSymbolicLink;
import static com.netcracker.cloud.security.core.utils.k8s.SystemPropertiesTestHelper.withProperty;
import static com.netcracker.cloud.security.core.utils.k8s.impl.CachingTokenSource.POLLING_INTERVAL_PROP;
import static com.netcracker.cloud.security.core.utils.k8s.impl.CachingTokenSource.TOKENS_DIR_PROP;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class CachingTokenSourceTest {

    private final RetryPolicy<?> retryPolicy = new RetryPolicy<>()
            .withDelay(Duration.ofMillis(5))
            .withMaxDuration(Duration.ofSeconds(1));

    @Test
    void getToken(@TempDir Path storageRoot) throws Exception {
        var interval = Duration.ofMillis(10);
        updateToken(storageRoot, "dbaas", "token1");

        try (var ts = new CachingTokenSource(storageRoot, interval)) {
            assertEquals("token1", ts.getToken("dbaas"));

            // test update
            updateToken(storageRoot, "dbaas", "token2");
            Failsafe.with(retryPolicy).run(() -> assertEquals("token2", ts.getToken("dbaas")));
        }
    }

    @Test
    void testDefaultConstructor(@TempDir Path storageRoot) throws Exception {
        var props = Map.of(
                TOKENS_DIR_PROP, storageRoot.toString(),
                POLLING_INTERVAL_PROP, "PT0.010S"
        );
        withProperty(props, () -> {
                    updateToken(storageRoot, "dbaas", "token1");

                    try (var ts = new CachingTokenSource()) {
                        assertEquals("token1", ts.getToken("dbaas"));

                        // test update
                        log.info("Update token, wait some time, and recheck token");
                        updateToken(storageRoot, "dbaas", "token2");
                        Thread.sleep(100);
                        Failsafe.with(retryPolicy).run(() -> assertEquals("token2", ts.getToken("dbaas")));
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
        Path timestampDir = FilesUtils.createTimestampDir(storageRoot);

        // Create the audience subdirectory
        var audienceDir = timestampDir.resolve(audience);
        Files.createDirectories(audienceDir);

        // Create the token file with sample token content
        var tokenFile = audienceDir.resolve("token");
        Files.writeString(tokenFile, token);
        // Create the default service account token file with sample token content
        Files.writeString(storageRoot.resolve("token"), token);

        // Create ..data symlink pointing to timestamp directory (fallback on Windows if not permitted)
        var dataLink = storageRoot.resolve("..data");
        Files.deleteIfExists(dataLink);
        boolean dataSymlinkCreated = tryCreateSymbolicLink(dataLink, timestampDir.getFileName());

        // Create audience symlink pointing to ..data/audience
        var audienceLink = storageRoot.resolve(audience);
        if (dataSymlinkCreated && !Files.exists(audienceLink)) {
            if (tryCreateSymbolicLink(audienceLink, dataLink.resolve(audience))) {
                return;
            }
        }

        // Fallback: create real directory under storageRoot/audience with token file.
        Files.createDirectories(audienceLink);
        Files.writeString(audienceLink.resolve("token"), token);
    }
}
