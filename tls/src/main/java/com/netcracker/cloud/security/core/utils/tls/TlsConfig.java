package com.netcracker.cloud.security.core.utils.tls;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;

public interface TlsConfig {

    void load();

    SSLContext createSSLContext(KeyStore trustStore, KeyStore keyStore, String keyPassword);

    boolean isInternalTlsEnabled();

    String getKeyStoreType();

    String getTrustStoreType();

    String getKeyStorePath();

    String getTrustStorePath();

    String getCaCertificatePath();

    String getCertificateStorePassword();

    KeyStore getKeyStore();

    KeyStore getTrustStore();

    X509KeyManager getKeyManager();

    X509TrustManager getTrustManager();

    SSLContext getSslContext();

    int priority();
}
