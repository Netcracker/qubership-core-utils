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

import java.io.IOException;
import java.util.List;

@Slf4j
public class K8sTokenVerifier {
    private static final String oidcTokenAud = "oidc-token";

    private final Object lock = new Object();
    private String jwksEndpoint;
    private K8sOidcRestClient restClient;
    private JwtConsumer jwtClaimsParser;
    private List<JsonWebKey> jwksCache;

    public K8sTokenVerifier(String jwtAudience) {
        try {
            TokenSource tokenSource = K8sTokenSource.createTokenSource(oidcTokenAud);
            String issuer = this.getIssuerFromJwt(tokenSource.getToken());
            initialize(new K8sOidcRestClient(tokenSource), jwtAudience, issuer);
        } catch (IOException | RuntimeException e) {
            throw new RuntimeException("failed to create k8s token verifier (possibly projected volume token misconfigured in k8s deployment)", e);
        }
    }

    K8sTokenVerifier(K8sOidcRestClient restClient, String audience, String issuer) {
        initialize(restClient, audience, issuer);
    }

    private void initialize(K8sOidcRestClient restClient, String jwtAudience, String issuer) {
        this.restClient = restClient;
        this.jwtClaimsParser = getJwtClaimsParser(issuer, jwtAudience);
        this.jwksEndpoint = restClient.getOidcConfiguration(issuer).getJwks_uri();
        refreshJwksCache();
    }

    public JwtClaims verify(String token) throws K8sTokenVerificationException {
        try {
            JwtContext jwtContext = jwtClaimsParser.process(token);
            verifySignature(token, jwtContext);
            return jwtContext.getJwtClaims();
        } catch (InvalidJwtException e) {
            throw new K8sTokenVerificationException("failed to verify k8s token", e);
        }
    }

    private void verifySignature(String token, JwtContext jwtContext) throws K8sTokenVerificationException {
        String keyId = jwtContext.getJoseObjects().getFirst().getKeyIdHeaderValue();
        JsonWebKey jsonWebKey = getJwk(keyId);
        if (jsonWebKey == null) {
            throw new K8sTokenVerificationException("jwk not found");
        }
        JsonWebSignature jws = new JsonWebSignature();
        try {
            jws.setCompactSerialization(token);
            jws.setKey(jsonWebKey.getKey());

            if (!jws.verifySignature()) {
                throw new K8sTokenVerificationException("jwt token has an invalid signature");
            }
        } catch (JoseException e) {
            throw new K8sTokenVerificationException(e);
        }
    }

    private void refreshJwksCache() {
        try {
            String rawJwks = restClient.getJwks(jwksEndpoint);
            jwksCache = new JsonWebKeySet(rawJwks).getJsonWebKeys();
        } catch (TimeoutExceededException | JoseException e) {
            String msg = "Getting Json web keys from kubernetes jwks endpoint %s failed".formatted(jwksEndpoint);
            log.error(msg, e);
            throw new RuntimeException(msg, e);
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

    private String getIssuerFromJwt(String token) {
        try {
            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                    .setSkipAllValidators()
                    .setDisableRequireSignature()
                    .setSkipSignatureVerification()
                    .build();
            JwtContext jwtContext = null;
            jwtContext = jwtConsumer.process(token);
            return jwtContext.getJwtClaims().getIssuer();
        } catch (InvalidJwtException | MalformedClaimException e) {
            throw new RuntimeException("failed to get issuer from k8s projected volume token", e);
        }
    }

    private JwtConsumer getJwtClaimsParser(String issuer, String audience) {
        return new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setAllowedClockSkewInSeconds(30)
                .setRequireSubject()
                .setExpectedIssuer(issuer)
                .setExpectedAudience(audience)
                .setSkipSignatureVerification()
                .build();
    }
}
