package org.qubership.cloud.security.core.utils.tls;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyStore;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TlsUtilsTest {

    @Mock
    private TlsConfig tlsConfigMock;

    @BeforeEach
    void setUp() {
        TlsUtils.setTlsConfigForTesting(tlsConfigMock);
    }

    @AfterEach
    void tearDown() {
        TlsUtils.restoreTlsConfigForTesting();
    }

    @Test
    void testCreateSSLContext() {
        KeyStore trustStore = mock(KeyStore.class);
        KeyStore keyStore = mock(KeyStore.class);
        String keyPassword = "testPassword";

        TlsUtils.createSSLContext(trustStore, keyStore, keyPassword);
        verify(tlsConfigMock, times(1)).createSSLContext(trustStore, keyStore, keyPassword);
    }

    @Test
    void testIsInternalTlsEnabled() {
        TlsUtils.isInternalTlsEnabled();
        verify(tlsConfigMock, times(1)).isInternalTlsEnabled();
    }

    @Test
    void testGetKeyStoreType() {
        TlsUtils.getKeyStoreType();
        verify(tlsConfigMock, times(1)).getKeyStoreType();
    }

    @Test
    void testGetTrustStoreType() {
        TlsUtils.getTrustStoreType();
        verify(tlsConfigMock, times(1)).getTrustStoreType();
    }

    @Test
    void testGetKeyStorePath() {
        TlsUtils.getKeyStorePath();
        verify(tlsConfigMock, times(1)).getKeyStorePath();
    }

    @Test
    void testGetTrustStorePath() {
        TlsUtils.getTrustStorePath();
        verify(tlsConfigMock, times(1)).getTrustStorePath();
    }

    @Test
    void testGetCaCertificatePath() {
        TlsUtils.getCaCertificatePath();
        verify(tlsConfigMock, times(1)).getCaCertificatePath();
    }

    @Test
    void testGetCertificateStorePassword() {
        TlsUtils.getCertificateStorePassword();
        verify(tlsConfigMock, times(1)).getCertificateStorePassword();
    }

    @Test
    void testGetKeyStore() {
        TlsUtils.getKeyStore();
        verify(tlsConfigMock, times(1)).getKeyStore();
    }

    @Test
    void testGetTrustStore() {
        TlsUtils.getTrustStore();
        verify(tlsConfigMock, times(1)).getTrustStore();
    }

    @Test
    void testGetKeyManager() {
        TlsUtils.getKeyManager();
        verify(tlsConfigMock, times(1)).getKeyManager();
    }

    @Test
    void testGetTrustManager() {
        TlsUtils.getTrustManager();
        verify(tlsConfigMock, times(1)).getTrustManager();
    }

    @Test
    void testGetSslContext() {
        TlsUtils.getSslContext();
        verify(tlsConfigMock, times(1)).getSslContext();
    }

    @Test
    void testSelectUrl() {
        String httpUrl = "http://example.com";
        String httpsUrl = "https://example.com";

        when(tlsConfigMock.isInternalTlsEnabled()).thenReturn(true);
        String result = TlsUtils.selectUrl(httpUrl, httpsUrl);
        Assertions.assertEquals(httpsUrl, result);

        when(tlsConfigMock.isInternalTlsEnabled()).thenReturn(false);
        result = TlsUtils.selectUrl(httpUrl, httpsUrl);
        Assertions.assertEquals(httpUrl, result);

        verify(tlsConfigMock, times(2)).isInternalTlsEnabled();

    }
}
