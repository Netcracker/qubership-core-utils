package com.netcracker.cloud.security.core.utils.k8s;

import com.netcracker.cloud.security.core.utils.k8s.impl.KubernetesProjectedVolumeWatcher;
import com.netcracker.cloud.security.core.utils.k8s.impl.Try;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.netcracker.cloud.security.core.utils.k8s.impl.WatchingTokenSource.POLLING_INTERVAL_DEFAULT;
import static com.netcracker.cloud.security.core.utils.k8s.impl.WatchingTokenSource.POLLING_INTERVAL_PROP;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KubernetesServiceAccountToken {

    public static final String SERVICE_ACCOUNT_DIR_PROP = "com.netcracker.cloud.security.kubernetes.service.account.token.dir";
    public static final Path SERVICE_ACCOUNT_DIR_DEFAULT = Paths.get("/var/run/secrets/kubernetes.io/serviceaccount");

    private static final Duration interval = Optional.ofNullable(System.getProperty(POLLING_INTERVAL_PROP))
            .map(Duration::parse)
            .orElse(POLLING_INTERVAL_DEFAULT);
    private static final AtomicReference<Try<String>> token = new AtomicReference<>();
    private static final KubernetesProjectedVolumeWatcher watcher = new KubernetesProjectedVolumeWatcher(
            getStorageRoot(),
            interval,
            KubernetesProjectedVolumeWatcher.EXECUTOR,
            KubernetesServiceAccountToken::updateCache);

    private static Path getStorageRoot() {
        return Optional.ofNullable(System.getProperty(SERVICE_ACCOUNT_DIR_PROP))
                .map(Paths::get)
                .orElse(SERVICE_ACCOUNT_DIR_DEFAULT);
    }

    /**
     * getToken is used for getting a token to use in requests to Kubernetes API
     *
     * @return the default Kubernetes service account token string
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
