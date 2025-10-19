package com.netcracker.cloud.security.core.utils.k8s;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static com.netcracker.cloud.security.core.utils.k8s.KubernetesServiceAccountToken.SERVICE_ACCOUNT_DIR_PROP;
import static com.netcracker.cloud.security.core.utils.k8s.SystemPropertiesTestHelper.withProperty;
import static com.netcracker.cloud.security.core.utils.k8s.impl.CachingTokenSource.POLLING_INTERVAL_PROP;
import static org.junit.jupiter.api.Assertions.*;

class KubernetesServiceAccountTokenTest {
    @Test
    void testGetToken(@TempDir Path storageRoot) throws Exception {
        withProperty(Map.of(
                    SERVICE_ACCOUNT_DIR_PROP, storageRoot.toString(),
                    POLLING_INTERVAL_PROP, "PT0.010S"
                ),
                () -> {
                    createKubernetesSecretsStructure(storageRoot, "token1");
                    assertEquals("token1", KubernetesServiceAccountToken.getToken());

                    createKubernetesSecretsStructure(storageRoot, "token2");
                    Thread.sleep(100);
                    assertEquals("token2", KubernetesServiceAccountToken.getToken());
                }
        );
    }

    @SneakyThrows
    private static void createKubernetesSecretsStructure(Path serviceAccountDir, String tokenValue) {
        // Timestamped subdirectory, like "..2025_10_19_18_14_19.2820761135"
        String timestamp = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss.SSSSSSSSS")
                .format(LocalDateTime.now());
        Path versionedDir = serviceAccountDir.resolve(".." + timestamp);

        // Create directory hierarchy
        Files.createDirectories(versionedDir);

        // Create example files
        Files.writeString(versionedDir.resolve("token"), tokenValue);

        // Create symlink "..data" -> "..<timestamp>"
        Path dataLink = serviceAccountDir.resolve("..data");
        Files.deleteIfExists(dataLink);
        Files.createSymbolicLink(dataLink, Paths.get(".." + timestamp).getFileName());

        Files.deleteIfExists(serviceAccountDir.resolve("token"));
        Files.createSymbolicLink(serviceAccountDir.resolve("token"), Paths.get("..data/token"));
    }
}
