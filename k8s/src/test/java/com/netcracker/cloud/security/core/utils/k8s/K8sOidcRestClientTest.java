package com.netcracker.cloud.security.core.utils.k8s;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.jose4j.lang.JoseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

@ExtendWith(MockitoExtension.class)
class K8sOidcRestClientTest {
    private static final String mockOidcConfig = "{\"issuer\":\"https://kubernetes.default.svc.cluster.local\","
            + "\"jwks_uri\":\"https://192.168.49.2:8443/openid/v1/jwks\","
            + "\"response_types_supported\":[\"id_token\"],"
            + "\"subject_types_supported\":[\"public\"],"
            + "\"id_token_signing_alg_values_supported\":[\"RS256\"]}";

    private String mockJwks;
    private K8sOidcRestClient restClient;
    @Mock
    TokenSource tokenSource;

    @BeforeEach
    void setUp() throws IOException, JoseException {
        TestJwtUtils jwtUtils = new TestJwtUtils();
        mockJwks = jwtUtils.getJwks();
        String validToken = jwtUtils.getDefaultClaimsJwt("test-namespace");
        restClient = new K8sOidcRestClient(tokenSource);
        Mockito.when(tokenSource.getToken()).thenReturn(validToken);
    }


    @Test
    void getOidcConfiguration() throws IOException {
        MockWebServer server = new MockWebServer();

        MockResponse response = new MockResponse();
        response.setBody(mockOidcConfig);
        server.enqueue(response);

        server.start();

        HttpUrl baseUrl = server.url("");

        Assertions.assertEquals("https://192.168.49.2:8443/openid/v1/jwks", restClient.getOidcConfiguration(baseUrl.toString()).getJwks_uri());

        server.close();
    }

    @Test
    void getJwks() throws IOException {
        MockWebServer server = new MockWebServer();

        MockResponse response = new MockResponse();
        response.setBody(mockJwks);
        server.enqueue(response);

        server.start();

        HttpUrl baseUrl = server.url("");

        Assertions.assertEquals(mockJwks, restClient.getJwks(baseUrl.toString()));

        server.close();
    }
}
