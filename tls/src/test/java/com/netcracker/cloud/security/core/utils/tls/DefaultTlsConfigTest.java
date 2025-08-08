package org.qubership.cloud.security.core.utils.tls;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.NoSuchAlgorithmException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultTlsConfigTest {

    @Test
    void testSslContextLoad() {
        assertNotNull(new DefaultTlsConfig().getSslContext());
    }

    @Test
    void testGetTrustManagers() {
        assertNotNull(new DefaultTlsConfig().getTrustManager());
    }

    @Test
    void constructorShouldInitializeSuccessfully() {
        assertDoesNotThrow(DefaultTlsConfig::new);
    }

    @Test
    void constructorShouldThrowTlsInitializationExceptionWhenTrustManagerFactoryFails() {
        try (MockedStatic<TrustManagerFactory> mockedTrustManagerFactory = mockStatic(TrustManagerFactory.class)) {
            mockedTrustManagerFactory.when(() -> TrustManagerFactory.getInstance("PKIX"))
                    .thenThrow(new NoSuchAlgorithmException("Test exception"));

            assertThrows(TlsInitializationException.class, DefaultTlsConfig::new);
        }
    }

    @Test
    void constructorShouldThrowTlsInitializationExceptionWhenSslContextFails() {
        try (MockedStatic<SSLContext> mockedSslContext = mockStatic(SSLContext.class)) {
            mockedSslContext.when(() -> SSLContext.getInstance("TLS"))
                    .thenThrow(new NoSuchAlgorithmException("Test exception"));

            assertThrows(TlsInitializationException.class, DefaultTlsConfig::new);
        }
    }

    @Test
    void isInternalTlsEnabledShouldReturnFalse() {
        DefaultTlsConfig config = new DefaultTlsConfig();
        assertFalse(config.isInternalTlsEnabled());
    }

    @Test
    void getTrustManagerShouldReturnValidTrustManager() {
        DefaultTlsConfig config = new DefaultTlsConfig();
        X509TrustManager trustManager = config.getTrustManager();
        assertNotNull(trustManager);
    }

    @Test
    void getTrustManagerShouldThrowIllegalStateExceptionWhenNoTrustManagersAvailable() {
        try (MockedStatic<TrustManagerFactory> mockedTrustManagerFactory = mockStatic(TrustManagerFactory.class)) {
            TrustManagerFactory mockFactory = mock(TrustManagerFactory.class);
            when(mockFactory.getTrustManagers()).thenReturn(new TrustManager[0]);
            mockedTrustManagerFactory.when(() -> TrustManagerFactory.getInstance("PKIX"))
                    .thenReturn(mockFactory);

            DefaultTlsConfig config = new DefaultTlsConfig();
            assertThrows(IllegalStateException.class, config::getTrustManager);
        }
    }

    @Test
    void getSslContextShouldReturnValidSslContext() {
        DefaultTlsConfig config = new DefaultTlsConfig();
        SSLContext sslContext = config.getSslContext();
        assertNotNull(sslContext);
        assertEquals("TLS", sslContext.getProtocol());
    }

    @Test
    void priorityShouldReturnZero() {
        DefaultTlsConfig config = new DefaultTlsConfig();
        assertEquals(0, config.priority());
    }
}
