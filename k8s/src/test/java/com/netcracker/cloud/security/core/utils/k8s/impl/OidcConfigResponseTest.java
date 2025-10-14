package com.netcracker.cloud.security.core.utils.k8s.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

class OidcConfigResponseTest {
    @Test
    void testDeserialize() throws Exception {
        var mapper = new ObjectMapper();
        var json = "{\"issuer\":\"https://kubernetes.default.svc.cluster.local\","
                + "\"jwks_uri\":\"https://192.168.49.2:8443/openid/v1/jwks\","
                + "\"response_types_supported\":[\"id_token\"],"
                + "\"subject_types_supported\":[\"public\"],"
                + "\"id_token_signing_alg_values_supported\":[\"RS256\"]}";

        Assertions.assertEquals("https://192.168.49.2:8443/openid/v1/jwks", mapper.readValue(json, OidcConfigResponse.class).getJwksUri());
    }
}
