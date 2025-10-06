package com.netcracker.cloud.security.core.utils.k8s;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.lang3.StringUtils;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

import static java.net.HttpURLConnection.HTTP_OK;

@Slf4j
class K8sOidcRestClient {
    private static final int retryPolicyBackoffMaxAttempts = 5;
    private static final Duration retryPolicyBackoffDelay = Duration.ofMillis(500);
    private static final Duration retryPolicyBackoffMaxDelay = Duration.ofSeconds(15);
    private static final Duration retryPolicyJitter = Duration.ofMillis(100);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RetryPolicy<HttpResponse<String>> retryPolicy = new RetryPolicy<HttpResponse<String>>()
            .withMaxRetries(retryPolicyBackoffMaxAttempts)
            .withBackoff(retryPolicyBackoffDelay.toMillis(), retryPolicyBackoffMaxDelay.toMillis(), ChronoUnit.MILLIS)
            .withJitter(retryPolicyJitter)
            .handleResultIf(res -> res.statusCode() / 100 == 5);
    private final HttpClient client = HttpClient.newHttpClient();
    private final Supplier<String> tokenSupplier;

    public K8sOidcRestClient(Supplier<String> tokenSupplier) {
        this.tokenSupplier = tokenSupplier;
    }

    public OidcConfigResponse getOidcConfiguration(String issuer) {
        var url = issuer + "/.well-known/openid-configuration";
        log.info("Request OIDC configuration for {}", url);
        try {
            return objectMapper.readValue(doRequest(url), OidcConfigResponse.class);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to get OIDC configuration with issuer %s from Kubernetes", issuer), e);
        }
    }

    public String getJwks(String jwksEndpoint) {
        log.info("Request JWKS data from: {}", jwksEndpoint);
        return doRequest(jwksEndpoint);
    }

    private String doRequest(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Can't parse " + url + " to URI form", e);
        }

        log.info("Perform request: GET {}", url);
        var request = HttpRequest.newBuilder()
                .setHeader("Authorization", tokenSupplier.get())
                .uri(uri)
                .GET()
                .build();
        HttpResponse<String> response = Failsafe.with(retryPolicy).get(() -> this.client.send(request, HttpResponse.BodyHandlers.ofString()));
        if (response.statusCode() != HTTP_OK) {
            throw new RuntimeException("Unexpected response code " + response.statusCode() + " for GET " + url);
        }

        var responseBody = response.body();
        if (StringUtils.isEmpty(responseBody)) {
            throw new RuntimeException("Received empty response body for GET " + url);
        }
        return responseBody;
    }
}
