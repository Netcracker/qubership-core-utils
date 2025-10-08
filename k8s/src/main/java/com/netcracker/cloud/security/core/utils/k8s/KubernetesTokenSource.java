package com.netcracker.cloud.security.core.utils.k8s;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.Optional;
import java.util.ServiceLoader;

@Slf4j
public class KubernetesTokenSource {
    private static final TokenSource INSTANCE = getInstance();

    private KubernetesTokenSource() {
    }

    private static TokenSource getInstance() {
        ServiceLoader<TokenSource> loader = ServiceLoader.load(TokenSource.class);

        var instance = loader.stream()
                .map(source ->
                    new TokenImplementation(
                            Optional.of(source.type().getAnnotation(Priority.class)).map(Priority::value).orElse(0),
                            source
                        )
                )
                .peek(s -> log.info("Found TokenSource implementation: {}, priority: {}", s.getProvider().type(), s.getPriority()))
                .max(Comparator.comparingInt(TokenImplementation::getPriority))
                .map(TokenImplementation::getProvider)
                .map(ServiceLoader.Provider::get)
                .orElseThrow(() -> new Error("Unable to locate implementation for: " + loader.getClass().getName()));

        log.info("Use {} as token source", instance.getClass().getName());
        return instance;
    }

    public static String getToken(String audience) {
        return INSTANCE.getToken(audience);
    }

    public static String getDefaultToken() {
        return INSTANCE.getDefaultToken();
    }

    @Value
    private static class TokenImplementation {
        int  priority;
        ServiceLoader.Provider<TokenSource> provider;
    }
}
