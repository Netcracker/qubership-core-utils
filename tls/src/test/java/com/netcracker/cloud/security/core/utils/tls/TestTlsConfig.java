package com.netcracker.cloud.security.core.utils.tls;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;

import static org.mockito.Mockito.mock;

public class TestTlsConfig implements TlsConfig {
    private final KeyStore keyStore;
    private final KeyStore trustStore;
    private final X509KeyManager keyManager;
    private final X509TrustManager trustManager;
    private final SSLContext sslContext;
    private final boolean internalTlsEnabled;
    private final String keyStoreType;
    private final String trustStoreType;
    private final String keyStorePath;
    private final String trustStorePath;
    private final String caCertificatePath;
    private final String certificateStorePassword;

    public TestTlsConfig() {
        this.keyStore = mock(KeyStore.class);
        this.trustStore = mock(KeyStore.class);
        this.keyManager = mock(X509KeyManager.class);
        this.trustManager = mock(X509TrustManager.class);
        this.sslContext = mock(SSLContext.class);
        this.internalTlsEnabled = true;
        this.keyStoreType = "JKS";
        this.trustStoreType = "JKS";
        this.keyStorePath = "/path/to/keystore";
        this.trustStorePath = "/path/to/truststore";
        this.caCertificatePath = "/path/to/ca";
        this.certificateStorePassword = "password";
    }

    @Override
    public void load() {
    }

    @Override
    public int priority() {
        return 100; // High priority to ensure it's selected
    }

    @Override
    public SSLContext createSSLContext(KeyStore trustStore, KeyStore keyStore, String keyPassword) {
        return sslContext;
    }

    @Override
    public boolean isInternalTlsEnabled() {
        return internalTlsEnabled;
    }

    @Override
    public String getKeyStoreType() {
        return keyStoreType;
    }

    @Override
    public String getTrustStoreType() {
        return trustStoreType;
    }

    @Override
    public String getKeyStorePath() {
        return keyStorePath;
    }

    @Override
    public String getTrustStorePath() {
        return trustStorePath;
    }

    @Override
    public String getCaCertificatePath() {
        return caCertificatePath;
    }

    @Override
    public String getCertificateStorePassword() {
        return certificateStorePassword;
    }

    @Override
    public KeyStore getKeyStore() {
        return keyStore;
    }

    @Override
    public KeyStore getTrustStore() {
        return trustStore;
    }

    @Override
    public X509KeyManager getKeyManager() {
        return keyManager;
    }

    @Override
    public X509TrustManager getTrustManager() {
        return trustManager;
    }

    @Override
    public SSLContext getSslContext() {
        return sslContext;
    }
}
