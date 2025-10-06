package com.netcracker.cloud.security.core.utils.k8s;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.TimeoutExceededException;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.lang.JoseException;

import java.security.Key;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class K8sTokenVerifier {
    private static final String oidcTokenAud = "oidc-token";

    private final String jwksEndpoint;
    private final K8sOidcRestClient restClient;
    private final JwtConsumer jwtClaimsParser;
    private final AtomicReference<Map<String, Key>> jwksCache = new AtomicReference<>();

    public K8sTokenVerifier(String jwtAudience) {
        this(new K8sOidcRestClient(
                () -> KubernetesTokenSource.getToken(oidcTokenAud)),
                jwtAudience,
                () -> KubernetesTokenSource.getToken(oidcTokenAud)
        );
    }

    K8sTokenVerifier(K8sOidcRestClient restClient, String audience, Supplier<String> oidcToken) {
         this.restClient = restClient;
        String issuer = this.getIssuerFromJwt(oidcToken.get());
        this.jwtClaimsParser = new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setAllowedClockSkewInSeconds(30)
                .setRequireSubject()
                .setExpectedIssuer(issuer)
                .setExpectedAudience(audience)
                .setSkipSignatureVerification()
                .build();
        this.jwksEndpoint = restClient.getOidcConfiguration(issuer).getJwksUri();
        this.jwksCache.set(fetchKeys());
    }

    public JwtClaims verify(String token) throws K8sTokenVerificationException {
        try {
            JwtContext jwtContext = jwtClaimsParser.process(token);
            verifySignature(token, jwtContext);
            return jwtContext.getJwtClaims();
        } catch (InvalidJwtException e) {
            throw new K8sTokenVerificationException("Failed to verify k8s token", e);
        }
    }

    private void verifySignature(String token, JwtContext jwtContext) throws K8sTokenVerificationException {
        if (jwtContext.getJoseObjects().isEmpty())  {
            throw new K8sTokenVerificationException("jwtContext is empty");
        }

        String keyId = jwtContext.getJoseObjects().get(0).getKeyIdHeaderValue();
        var key = getJwk(keyId);
        if (key == null) {
            throw new K8sTokenVerificationException("jwk not found");
        }
        JsonWebSignature jws = new JsonWebSignature();
        try {
            jws.setCompactSerialization(token);
            jws.setKey(key);

            if (!jws.verifySignature()) {
                throw new K8sTokenVerificationException("jwt token has an invalid signature");
            }
        } catch (JoseException e) {
            throw new K8sTokenVerificationException(e);
        }
    }

    private Map<String, Key> fetchKeys() {
        try {
            var rawJwks = restClient.getJwks(jwksEndpoint);
            return new JsonWebKeySet(rawJwks)
                    .getJsonWebKeys()
                    .stream()
                    .collect(Collectors.toMap(JsonWebKey::getKeyId, JsonWebKey::getKey));
        } catch (TimeoutExceededException | JoseException e) {
            String msg = String.format("Getting Json web keys from kubernetes jwks endpoint %s failed", jwksEndpoint);
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    private Key getJwk(String keyId) {
        var key = jwksCache.get().get(keyId);
        if (key != null) {
            return key;
        }
        synchronized(jwksCache) {
            key = jwksCache.get().get(keyId);
            if (key != null) {
                return key;
            }

            this.jwksCache.set(fetchKeys());
        }
        return jwksCache.get().get(keyId);
    }

    private String getIssuerFromJwt(String token) {
        try {
            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                    .setSkipAllValidators()
                    .setDisableRequireSignature()
                    .setSkipSignatureVerification()
                    .build();
            return jwtConsumer.process(token).getJwtClaims().getIssuer();
        } catch (InvalidJwtException | MalformedClaimException e) {
            throw new RuntimeException("failed to get issuer from k8s projected volume token", e);
        }
    }
}
