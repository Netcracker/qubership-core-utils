package com.netcracker.cloud.security.core.utils.k8s;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

@Slf4j
public class K8sOidcRestClient {
    private final OkHttpClient client;
    private final K8sTokenInterceptor k8sTokenInterceptor;
    @Getter
    String jwtIssuer;

    public K8sOidcRestClient(TokenSource tokenSource, String jwtIssuer) throws IOException {
        Builder builder = new OkHttpClient.Builder();

        k8sTokenInterceptor = new K8sTokenInterceptor(tokenSource);
        this.jwtIssuer = jwtIssuer;
        builder.addInterceptor(k8sTokenInterceptor);

        client = builder.build();
    }

    public OidcConfig getOidcConfiguration() throws RuntimeException {
        Request request = new Request.Builder()
                .url(jwtIssuer + "/.well-known/openid-configuration")
                .build();

        Call call = client.newCall(request);
        try (Response response = call.execute()) {
            ObjectMapper objectMapper = new ObjectMapper();
            if (response.body() == null) {
                throw new RuntimeException("Response for requesting oidc configuration from Kubernetes IDP does not have response body");
            }
            return objectMapper.readValue(response.body().string(), OidcConfig.class);
        } catch (IOException e) {
            log.error("Failed to retrieve OIDC configuration from Kubernetes IDP", e);
            throw new RuntimeException(e);
        }
    }

    public String getJwks(String jwksEndpoint) throws IOException {
        Request request = new Request.Builder()
                .url(jwksEndpoint)
                .build();

        Call call = client.newCall(request);
        try (Response response = call.execute()) {
            if (response.body() == null) {
                throw new RuntimeException("Response for requesting jwks from Kubernetes IDP does not have response body");
            }
            return response.body().string();
        }
    }
}
