package com.netcracker.cloud.security.core.utils.k8s;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class K8sTokenVerifierTest {
    @Mock
    K8sOidcRestClient restClient;
    @Mock
    TokenSource tokenSource;
    K8sTokenVerifier verifier;
    TestJwtUtils jwtUtils;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtils = new TestJwtUtils();

        when(restClient.getOidcConfiguration(jwtUtils.getJwtIssuer())).thenReturn(new OidcConfig(jwtUtils.getJwksEndpoint()));
        when(restClient.getJwks(jwtUtils.getJwksEndpoint())).thenReturn(jwtUtils.getJwks());

        verifier = new K8sTokenVerifier(restClient, jwtUtils.getDbaasJwtAudience(), jwtUtils.getDefaultClaimsJwt("test-namespace"));
    }

    @AfterEach
    void tearDown() {
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

        assertThrows(K8sTokenVerificationException.class, () -> {
            verifier.verify(jwtUtils.getJwt(validClaims, true));
        });

        assertThrows(K8sTokenVerificationException.class, () -> {
            String jwt = jwtUtils.getJwt(validClaims, false);
            jwt += "tamperWithSignature";
            verifier.verify(jwt);
        });

        assertThrows(K8sTokenVerificationException.class, () -> {
            JwtClaims invalidClaims = new JwtClaims();
            validClaims.setIssuer("someOtherIssuer");
            validClaims.setAudience(jwtUtils.getDbaasJwtAudience());
            validClaims.setSubject("some-service");
            validClaims.setExpirationTimeMinutesInTheFuture(10);
            validClaims.setGeneratedJwtId();
            validClaims.setIssuedAtToNow();

            verifier.verify(jwtUtils.getJwt(invalidClaims, false));
        });

        assertThrows(K8sTokenVerificationException.class, () -> {
            JwtClaims invalidClaims = new JwtClaims();
            validClaims.setIssuer(jwtUtils.getJwtIssuer());
            validClaims.setAudience("someOtherAudience");
            validClaims.setSubject("some-service");
            validClaims.setExpirationTimeMinutesInTheFuture(10);
            validClaims.setGeneratedJwtId();
            validClaims.setIssuedAtToNow();

            verifier.verify(jwtUtils.getJwt(invalidClaims, false));
        });

        assertThrows(K8sTokenVerificationException.class, () -> {
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
}
