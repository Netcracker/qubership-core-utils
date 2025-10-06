package com.netcracker.cloud.security.core.utils.k8s;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import java.net.URI;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
class OidcConfigResponse {
    String jwksUri;

    @JsonCreator
    public OidcConfigResponse(@JsonProperty("jwks_uri") String jwksUri) {
        this.jwksUri = jwksUri;
    }
}
