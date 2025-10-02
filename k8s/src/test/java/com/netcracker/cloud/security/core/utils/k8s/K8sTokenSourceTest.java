package com.netcracker.cloud.security.core.utils.k8s;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.junit.jupiter.api.BeforeEach;
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

    private Path tempTokensDir;
    private Path tokenDir;
    private Path tokenFile;
    private Path dataLink;
    private String audience;
    private String oldToken;

    @BeforeEach
    void setUp(@TempDir Path tempTokensDir) throws IOException {
        this.tempTokensDir = tempTokensDir;
        audience = "test-audience";
        tokenDir = Path.of(tempTokensDir.toString(), audience);
        Files.createDirectory(tokenDir);

        tokenFile = Files.createFile(Path.of(tokenDir.toString(), "token"));
        dataLink = Files.createSymbolicLink(Path.of(tokenDir.toString(), "..data"), tokenFile);

        oldToken = "oldToken";
        Files.writeString(tokenFile, oldToken);
    }

    @Test
    void createTokenSource() throws IOException {
        K8sTokenSource.createTokenSource(tempTokensDir.toString(), audience);

        Failsafe.with(TOKEN_CACHE_UPDATED_RETRY_POLICY).run(() -> {
            assertEquals(oldToken, K8sTokenSource.getToken(audience));
        });

        String newToken = "newToken";
        updateToken(newToken);

        Failsafe.with(TOKEN_CACHE_UPDATED_RETRY_POLICY).run(() -> {
            assertEquals(newToken, K8sTokenSource.getToken(audience));
        });

        K8sTokenSource.close();
        String tokenAfterClosed = "tokenAfterClosed";
        updateToken(tokenAfterClosed);

        assertEquals(newToken, K8sTokenSource.getToken(audience));
    }

    @Test
    void getTokenThrowsError() throws IOException {
        String unknownAudience = "unknown-audience";

        assertThrows(IOException.class, () -> {
            K8sTokenSource.getToken(unknownAudience);
        });
    }

    private void updateToken(String newToken) throws IOException {
        Files.writeString(tokenFile, newToken);

        Files.delete(dataLink);
        Files.createSymbolicLink(dataLink, tokenFile);
    }
}
