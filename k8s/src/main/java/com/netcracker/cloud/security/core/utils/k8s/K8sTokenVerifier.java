package com.netcracker.cloud.security.core.utils.k8s;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.TimeoutExceededException;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.lang.JoseException;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
public class K8sTokenVerifier {
    private static final RetryPolicy<Object> retryPolicy = new RetryPolicy<>()
            .withMaxRetries(5)
            .withBackoff(500, Duration.ofSeconds(60).toMillis(), ChronoUnit.MILLIS);

    private final Object lock = new Object();
    private final String jwtJwksEndpoint;
    private final JwtConsumer jwtClaimsParser;
    private List<JsonWebKey> jwksCache;

    private K8sOidcRestClient k8sOidcRestClient;

    public K8sTokenVerifier(
            K8sOidcRestClient k8sOidcRestClient,
            String jwtAudience
    ) {
        this.k8sOidcRestClient = k8sOidcRestClient;

        jwtClaimsParser = new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setAllowedClockSkewInSeconds(30)
                .setRequireSubject()
                .setExpectedIssuer(k8sOidcRestClient.getJwtIssuer())
                .setExpectedAudience(jwtAudience)
                .setSkipSignatureVerification()
                .build();

        jwtJwksEndpoint = k8sOidcRestClient.getOidcConfiguration().getJwks_uri();

        refreshJwksCache();

        log.debug("Finished creating K8sJWTCallerPrincipalFactory bean");
    }

    public JwtClaims verify(String token) throws K8sTokenVerificationException {
        try {
            JwtContext jwtContext = jwtClaimsParser.process(token);

            if (!verifySignature(token, jwtContext)) {
                throw new K8sTokenVerificationException("invalid jwt signature");
            }

            return jwtContext.getJwtClaims();
        } catch (InvalidJwtException e) {
            throw new K8sTokenVerificationException(e);
        }
    }

    private boolean verifySignature(String token, JwtContext jwtContext) throws K8sTokenVerificationException {
        String keyId = jwtContext.getJoseObjects().getFirst().getKeyIdHeaderValue();
        JsonWebKey jsonWebKey = getJwk(keyId);

        if (jsonWebKey == null) {
            throw new K8sTokenVerificationException("jwk not found");
        }

        JsonWebSignature jws = new JsonWebSignature();

        try {
            jws.setCompactSerialization(token);
            jws.setKey(jsonWebKey.getKey());

            return jws.verifySignature();
        } catch (JoseException e) {
            throw new K8sTokenVerificationException(e);
        }
    }

    private void refreshJwksCache() {
        try {
            Failsafe.with(retryPolicy).run(() -> {
                String rawJwks = k8sOidcRestClient.getJwks(jwtJwksEndpoint);
                jwksCache = new JsonWebKeySet(rawJwks).getJsonWebKeys();
            });
        } catch (TimeoutExceededException e) {
            log.error("Getting Json web keys from kubernetes jwks endpoint %s failed".formatted(jwtJwksEndpoint), e);
        }
    }

    private JsonWebKey getJwk(String keyId) {
        JsonWebKey jwk = getJwksFromCache(keyId);
        if (jwk != null) {
            return jwk;
        }

        synchronized (lock) {
            JsonWebKey jwksFromCache = getJwksFromCache(keyId);
            if (jwksFromCache != null) {
                return jwksFromCache;
            }

            refreshJwksCache();
        }

        return getJwksFromCache(keyId);
    }

    private JsonWebKey getJwksFromCache(String keyId) {
        List<JsonWebKey> jwks = jwksCache;
        for (JsonWebKey jwk : jwks) {
            if (keyId.equals(jwk.getKeyId())) {
                return jwk;
            }
        }
        return null;
    }
}
