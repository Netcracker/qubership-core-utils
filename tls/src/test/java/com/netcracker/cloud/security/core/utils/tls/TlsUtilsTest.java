package org.qubership.cloud.security.core.utils.tls;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.net.ssl.SSLContext;
import java.security.KeyStore;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class TlsUtilsTest {

    @Test
    void testServiceLoaderFromCurrentThreadContextClassLoader() {
        ServiceLoader<TlsConfig> serviceLoader = ServiceLoader.load(TlsConfig.class);

        assertNotNull(serviceLoader);

        int count = 0;
        for (TlsConfig service : serviceLoader) {
            assertNotNull(service, "Failed to load service.");
            count++;
        }

        assertEquals(2, count, "Failed to load all services.");

        Assertions.assertNotNull(TlsUtils.getSslContext());
    }

    @Test
    void testIsInternalTlsEnabled() {
        assertTrue(TlsUtils.isInternalTlsEnabled());
    }

    @Test
    void testGetKeyStoreType() {
        assertEquals("JKS", TlsUtils.getKeyStoreType());
    }

    @Test
    void testGetTrustStoreType() {
        assertEquals("JKS", TlsUtils.getTrustStoreType());
    }

    @Test
    void testGetKeyStorePath() {
        assertEquals("/path/to/keystore", TlsUtils.getKeyStorePath());
    }

    @Test
    void testGetTrustStorePath() {
        assertEquals("/path/to/truststore", TlsUtils.getTrustStorePath());
    }

    @Test
    void testGetCaCertificatePath() {
        assertEquals("/path/to/ca", TlsUtils.getCaCertificatePath());
    }

    @Test
    void testGetCertificateStorePassword() {
        assertEquals("password", TlsUtils.getCertificateStorePassword());
    }

    @Test
    void testGetKeyStore() {
        assertNotNull(TlsUtils.getKeyStore());
    }

    @Test
    void testGetTrustStore() {
        assertNotNull(TlsUtils.getTrustStore());
    }

    @Test
    void testGetKeyManager() {
        assertNotNull(TlsUtils.getKeyManager());
    }

    @Test
    void testGetTrustManager() {
        assertNotNull(TlsUtils.getTrustManager());
    }

    @Test
    void testGetSslContext() {
        assertNotNull(TlsUtils.getSslContext());
    }

    @Test
    void testSelectUrl() {
        String httpUrl = "http://example.com";
        String httpsUrl = "https://example.com";

        assertEquals(httpsUrl, TlsUtils.selectUrl(httpUrl, httpsUrl));
    }

    @Test
    void testCreateSSLContext() {
        KeyStore trustStore = mock(KeyStore.class);
        KeyStore keyStore = mock(KeyStore.class);
        String keyPassword = "password";

        SSLContext result = TlsUtils.createSSLContext(trustStore, keyStore, keyPassword);

        assertNotNull(result);
    }
}
