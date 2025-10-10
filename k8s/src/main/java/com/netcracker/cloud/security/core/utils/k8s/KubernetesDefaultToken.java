package com.netcracker.cloud.security.core.utils.k8s;

import com.netcracker.cloud.security.core.utils.k8s.impl.KubernetesProjectedVolumeWatcher;
import com.netcracker.cloud.security.core.utils.k8s.impl.Try;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class KubernetesDefaultToken {
    public static final String SERVICE_ACCOUNT_DIR_PROP = "com.netcracker.cloud.security.kubernetes.serviceaccount.dir";
    public static final Path SERVICE_ACCOUNT_DIR_DEFAULT = Paths.get("/var/run/secrets/kubernetes.io/serviceaccount");

    public static final String POLLING_INTERVAL_PROP = "com.netcracker.cloud.security.kubernetes.tokens.polling.interval";
    public static final Duration POLLING_INTERVAL_DEFAULT = Duration.ofMinutes(1);

    private static final Duration interval = Optional.ofNullable(System.getProperty(POLLING_INTERVAL_PROP))
            .map(Duration::parse)
            .orElse(POLLING_INTERVAL_DEFAULT);

    private static final AtomicReference<Try<String>> token = new AtomicReference<>();

    public static Path getStorageRoot() {
        return Optional.ofNullable(System.getProperty(SERVICE_ACCOUNT_DIR_PROP))
                .map(Paths::get)
                .orElse(SERVICE_ACCOUNT_DIR_DEFAULT);
    }

    private static final KubernetesProjectedVolumeWatcher watcher = new KubernetesProjectedVolumeWatcher(
                getStorageRoot(),
                interval,
                KubernetesProjectedVolumeWatcher.EXECUTOR,
                KubernetesDefaultToken::updateCache);

	/**
     * getToken method returns the default Kubernetes service account token string
     */
    public static String getToken() {
        return token.get().getOrThrow();
    }

    private static void updateCache(Path storageRoot) {
        var tokenPath = storageRoot.resolve("token");
        Try<String> value;
        try {
            value = Try.success(Files.readString(tokenPath));
        } catch (Exception e) {
            value = Try.failure(new RuntimeException("Error read token from: " + tokenPath, e));
        }
        token.set(value);
    }
}
