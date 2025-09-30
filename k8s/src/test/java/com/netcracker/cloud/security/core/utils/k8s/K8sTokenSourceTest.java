package com.netcracker.cloud.security.core.utils.k8s;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class K8sTokenSourceTest {
    private final static RetryPolicy<Object> TOKEN_CACHE_UPDATED_RETRY_POLICY = new RetryPolicy<>()
            .withMaxRetries(-1).withDelay(Duration.ofMillis(50)).withMaxDuration(Duration.ofSeconds(1));

    @Test
    void createTokenSource(@TempDir Path tempTokensDir) throws IOException {
        String audience = "test-audience";
        Path tokenDir = Path.of(tempTokensDir.toString(), audience);
        Files.createDirectory(tokenDir);
        Path tokenFile = Files.createFile(Path.of(tokenDir.toString(), K8sTokenSource.FileTokenSource.tokenFileName));
        Path dataLink = Files.createSymbolicLink(Path.of(tokenDir.toString(), K8sTokenSource.FileTokenSource.tokenFileLinkName), tokenFile);

        String oldToken = "oldToken";
        Files.writeString(tokenFile, oldToken);
        K8sTokenSource.createTokenSource(tempTokensDir.toString(), audience);

        Failsafe.with(TOKEN_CACHE_UPDATED_RETRY_POLICY).run(() -> {
            assertEquals(oldToken, K8sTokenSource.getToken(audience));
        });

        String newToken = "newToken";
        Files.writeString(tokenFile, newToken);

        Files.delete(dataLink);
        Files.createSymbolicLink(dataLink, tokenFile);

        Failsafe.with(TOKEN_CACHE_UPDATED_RETRY_POLICY).run(() -> {
            assertEquals(newToken, K8sTokenSource.getToken(audience));
        });
    }

    @Test
    void createTokenSourceRace() {
    }
}
