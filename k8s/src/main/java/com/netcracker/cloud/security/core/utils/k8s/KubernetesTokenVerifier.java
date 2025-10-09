package com.netcracker.cloud.security.core.utils.k8s;

import com.netcracker.cloud.security.core.utils.k8s.impl.KubernetesOidcRestClient;
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
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class KubernetesTokenVerifier {
    public static final String JWKS_VALID_INTERVAL_PROP = "com.netcracker.cloud.security.kubernetes.jwks.valid-interval";
    public static final Duration JWKS_VALID_INTERVAL_DEFAULT = Duration.ofDays(1);

    private final String jwksEndpoint;
    private final KubernetesOidcRestClient restClient;
    private final JwtConsumer jwtClaimsParser;
    private final AtomicReference<Map<String, Key>> jwksCache = new AtomicReference<>();
    private final Duration jwksValidInterval;
    private final AtomicReference<Instant> jwksExpiration = new AtomicReference<>(Instant.MIN);

    public KubernetesTokenVerifier(String jwtAudience) {
        this(
                new KubernetesOidcRestClient(KubernetesDefaultToken::getToken),
                jwtAudience,
                KubernetesDefaultToken::getToken,
                Optional.ofNullable(System.getProperty(JWKS_VALID_INTERVAL_PROP))
                        .map(Duration::parse)
                        .orElse(JWKS_VALID_INTERVAL_DEFAULT)
        );
    }

    KubernetesTokenVerifier(KubernetesOidcRestClient restClient, String audience, Supplier<String> oidcToken, Duration jwksValidInterval) {
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
        this.jwksEndpoint = restClient.getOidcConfiguration(issuer);
        this.jwksValidInterval = jwksValidInterval;
        this.jwksCache.set(fetchKeys());
    }

    public JwtClaims verify(String token) throws KubernetesTokenVerificationException {
        try {
            JwtContext jwtContext = jwtClaimsParser.process(token);
            verifySignature(token, jwtContext);
            return jwtContext.getJwtClaims();
        } catch (InvalidJwtException e) {
            throw new KubernetesTokenVerificationException("Failed to verify k8s token", e);
        }
    }

    private void verifySignature(String token, JwtContext jwtContext) throws KubernetesTokenVerificationException {
        if (jwtContext.getJoseObjects().isEmpty())  {
            throw new KubernetesTokenVerificationException("jwtContext is empty");
        }

        String keyId = jwtContext.getJoseObjects().get(0).getKeyIdHeaderValue();
        var key = getJwk(keyId);
        if (key == null) {
            throw new KubernetesTokenVerificationException("jwk not found");
        }
        JsonWebSignature jws = new JsonWebSignature();
        try {
            jws.setCompactSerialization(token);
            jws.setKey(key);

            if (!jws.verifySignature()) {
                throw new KubernetesTokenVerificationException("jwt token has an invalid signature");
            }
        } catch (JoseException e) {
            throw new KubernetesTokenVerificationException(e);
        }
    }

    private Map<String, Key> fetchKeys() {
        try {
            var rawJwks = restClient.getJwks(jwksEndpoint);
            jwksExpiration.set(Instant.now().plus(jwksValidInterval));
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
        var key = getJwkFromCache(keyId);
        if (key != null) {
            return key;
        }
        synchronized(jwksCache) {
            key = getJwkFromCache(keyId);
            if (key != null) {
                return key;
            }
            this.jwksCache.set(fetchKeys());
        }
        return jwksCache.get().get(keyId);
    }

    private Key getJwkFromCache(String keyId) {
        if(jwksExpiration.get().isBefore(Instant.now())) {
            return null;
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
