package com.netcracker.cloud.security.core.utils.k8s;

import com.netcracker.cloud.security.core.utils.k8s.impl.KubernetesOidcRestClient;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class KubernetesTokenVerifierTest {
    @Mock
    KubernetesOidcRestClient restClient;
    @Mock
    TokenSource tokenSource;
    KubernetesTokenVerifier verifier;
    TestJwtUtils jwtUtils;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtils = new TestJwtUtils();

        when(restClient.getOidcConfiguration(jwtUtils.getJwtIssuer())).thenReturn(jwtUtils.getJwksEndpoint());
        when(restClient.getJwks(jwtUtils.getJwksEndpoint())).thenReturn(jwtUtils.getJwks());

        verifier = new KubernetesTokenVerifier(restClient, jwtUtils.getDbaasJwtAudience(), () -> jwtUtils.getDefaultClaimsJwt("test-namespace"), KubernetesTokenVerifier.JWKS_VALID_INTERVAL_DEFAULT);
    }

    @Test
    void parse() {
        JwtClaims validClaims = new JwtClaims();
        validClaims.setIssuer(jwtUtils.getJwtIssuer());
        validClaims.setAudience(jwtUtils.getDbaasJwtAudience());
        validClaims.setSubject("some-service");
        validClaims.setExpirationTimeMinutesInTheFuture(10);
        validClaims.setGeneratedJwtId();
        validClaims.setIssuedAtToNow();

        assertDoesNotThrow(() -> {
            verifier.verify(jwtUtils.getJwt(validClaims, false));
        });

        assertThrows(KubernetesTokenVerificationException.class, () -> {
            verifier.verify(jwtUtils.getJwt(validClaims, true));
        });

        assertThrows(KubernetesTokenVerificationException.class, () -> {
            String jwt = jwtUtils.getJwt(validClaims, false);
            jwt += "tamperWithSignature";
            verifier.verify(jwt);
        });

        assertThrows(KubernetesTokenVerificationException.class, () -> {
            JwtClaims invalidClaims = new JwtClaims();
            validClaims.setIssuer("someOtherIssuer");
            validClaims.setAudience(jwtUtils.getDbaasJwtAudience());
            validClaims.setSubject("some-service");
            validClaims.setExpirationTimeMinutesInTheFuture(10);
            validClaims.setGeneratedJwtId();
            validClaims.setIssuedAtToNow();

            verifier.verify(jwtUtils.getJwt(invalidClaims, false));
        });

        assertThrows(KubernetesTokenVerificationException.class, () -> {
            JwtClaims invalidClaims = new JwtClaims();
            validClaims.setIssuer(jwtUtils.getJwtIssuer());
            validClaims.setAudience("someOtherAudience");
            validClaims.setSubject("some-service");
            validClaims.setExpirationTimeMinutesInTheFuture(10);
            validClaims.setGeneratedJwtId();
            validClaims.setIssuedAtToNow();

            verifier.verify(jwtUtils.getJwt(invalidClaims, false));
        });

        assertThrows(KubernetesTokenVerificationException.class, () -> {
            NumericDate invalidExpirationDate = NumericDate.now();
            invalidExpirationDate.addSeconds(-100);

            JwtClaims invalidClaims = new JwtClaims();
            validClaims.setIssuer(jwtUtils.getJwtIssuer());
            validClaims.setAudience(jwtUtils.getDbaasJwtAudience());
            validClaims.setSubject("some-service");
            validClaims.setExpirationTime(invalidExpirationDate);
            validClaims.setGeneratedJwtId();
            validClaims.setIssuedAtToNow();

            verifier.verify(jwtUtils.getJwt(invalidClaims, false));
        });
    }

    @Test
    void defaultConstructor() {
        assertThrows(RuntimeException.class, () -> new KubernetesTokenVerifier("test-audience"));
    }
}
