package com.netcracker.cloud.security.core.utils.tls;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;

public class DefaultTlsConfig implements TlsConfig {
    private final TrustManagerFactory trustManagerFactory;
    private final SSLContext sslContext;

    public DefaultTlsConfig() {
        try {
            trustManagerFactory = TrustManagerFactory.getInstance("PKIX");
            trustManagerFactory.init((KeyStore)null);

            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
        } catch (Exception e) {
            throw new TlsInitializationException("Failed to initialize tls objects", e);
        }
    }

    @Override
    public void load() {
        // all were loaded in constructor
    }

    @Override
    public SSLContext createSSLContext(KeyStore trustStore, KeyStore keyStore, String keyPassword) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    public boolean isInternalTlsEnabled() {
        return false;
    }

    @Override
    public String getKeyStoreType() {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    public String getTrustStoreType() {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    public String getKeyStorePath() {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    public String getTrustStorePath() {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    public String getCaCertificatePath() {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    public String getCertificateStorePassword() {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    public KeyStore getKeyStore() {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    public KeyStore getTrustStore() {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    public X509KeyManager getKeyManager() {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    public X509TrustManager getTrustManager() {
        if (trustManagerFactory.getTrustManagers().length == 0) {
            throw new IllegalStateException("No trust manager available");
        }

        return (X509TrustManager)trustManagerFactory.getTrustManagers()[0];
    }

    @Override
    public SSLContext getSslContext() {
        return sslContext;
    }

    @Override
    public int priority() {
        return 0;
    }
}
