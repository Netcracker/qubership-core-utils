package com.netcracker.cloud.security.core.utils.k8s;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static com.netcracker.cloud.security.core.utils.k8s.KubernetesServiceAccountToken.SERVICE_ACCOUNT_DIR_PROP;
import static com.netcracker.cloud.security.core.utils.k8s.SystemPropertiesTestHelper.withProperty;
import static com.netcracker.cloud.security.core.utils.k8s.impl.CachingTokenSource.POLLING_INTERVAL_PROP;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
        Path versionedDir = FilesUtils.createTimestampDir(serviceAccountDir);

        // Create example files
        Files.writeString(versionedDir.resolve("token"), tokenValue);

        // Create symlinks where supported; fallback to plain file on Windows without privileges.
        Path dataLink = serviceAccountDir.resolve("..data");
        Files.deleteIfExists(dataLink);
        boolean symlinkCreated = FilesUtils.tryCreateSymbolicLink(dataLink, versionedDir.getFileName());

        Path tokenLink = serviceAccountDir.resolve("token");
        Files.deleteIfExists(tokenLink);
        if (symlinkCreated && FilesUtils.tryCreateSymbolicLink(tokenLink, Paths.get("..data/token"))) {
            return;
        }

        // Fallback: write directly to token file if symlinks are not available.
        Files.writeString(tokenLink, tokenValue);
    }
}
