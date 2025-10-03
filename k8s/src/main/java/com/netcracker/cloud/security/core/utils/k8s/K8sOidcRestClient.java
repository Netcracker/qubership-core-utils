package com.netcracker.cloud.security.core.utils.k8s;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Slf4j
public class K8sOidcRestClient {
    private static final int retryPolicyBackoffMaxAttempts = 5;
    private static final Duration retryPolicyBackoffDelay = Duration.ofMillis(500);
    private static final Duration retryPolicyBackoffMaxDelay = Duration.ofSeconds(15);
    private static final Duration retryPolicyJitter = Duration.ofMillis(100);

    private final RetryPolicy<HttpResponse<String>> retryPolicy;
    private final HttpClient client;
    private final TokenSource tokenSource;

    public K8sOidcRestClient(TokenSource tokenSource) {
        this.retryPolicy = new RetryPolicy<HttpResponse<String>>()
                .withMaxRetries(retryPolicyBackoffMaxAttempts)
                .withBackoff(retryPolicyBackoffDelay.toMillis(), retryPolicyBackoffMaxDelay.toMillis(), ChronoUnit.MILLIS)
                .withJitter(retryPolicyJitter)
                .handleResultIf(res-> res.statusCode()/100 == 5);
        this.client = HttpClient.newHttpClient();
        this.tokenSource = tokenSource;
    }

    public OidcConfig getOidcConfiguration(String issuer) {
        try {
            HttpRequest request = newRequestWithAuth()
                    .uri(new URI(issuer + "/.well-known/openid-configuration"))
                    .GET()
                    .build();
            HttpResponse<String> response = Failsafe.with(retryPolicy).get(() -> this.client.send(request, HttpResponse.BodyHandlers.ofString()));
            checkResponse(response);
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(response.body(), OidcConfig.class);
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException("Failed to get OIDC configuration with issuer %s from Kubernetes".formatted(issuer), e);
        }
    }

    public String getJwks(String jwksEndpoint) {
        try {
            HttpRequest request = newRequestWithAuth()
                    .uri(new URI(jwksEndpoint))
                    .build();
            HttpResponse<String> response = Failsafe.with(retryPolicy).get(() -> this.client.send(request, HttpResponse.BodyHandlers.ofString()));
            checkResponse(response);
            return response.body();
        } catch (URISyntaxException | IOException | RuntimeException e) {
            throw new RuntimeException("Failed to get jwks with jwks endpoint %s from Kubernetes with jwks".formatted(jwksEndpoint), e);
        }
    }

    private HttpRequest.Builder newRequestWithAuth() throws IOException {
        return HttpRequest.newBuilder().setHeader("Authorization", tokenSource.getToken());
    }

    private void checkResponse(HttpResponse<String> response) {
        if (response.statusCode() != 200 || StringUtils.isEmpty(response.body())) {
            throw new RuntimeException("empty response body");
        }
    }
}
