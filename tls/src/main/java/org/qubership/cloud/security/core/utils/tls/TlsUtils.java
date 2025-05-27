package org.qubership.cloud.security.core.utils.tls;

import org.jetbrains.annotations.NotNull;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.util.Comparator;
import java.util.ServiceLoader;

@SuppressWarnings("unused")
public class TlsUtils {
    private static TlsConfig INSTANCE = getTlsConfig();

    @NotNull
    private static TlsConfig getTlsConfig() {
        ServiceLoader<TlsConfig> loader = ServiceLoader.load(TlsConfig.class);

        TlsConfig tlsConfig = loader
                .stream()
                .map(ServiceLoader.Provider::get)
                .max(Comparator.comparingInt(TlsConfig::priority))
                .orElseThrow(()->new Error("Service loader failed to load TlsConfig service: " + loader.getClass().getName()));

        tlsConfig.load();
        return tlsConfig;
    }

    @Deprecated() //FOR TESTING ONLY
    static void setTlsConfigForTesting(TlsConfig config) {
        INSTANCE = config;
    }

    @Deprecated() //FOR TESTING ONLY
    static void restoreTlsConfigForTesting() {
        INSTANCE = getTlsConfig();
    }

    public static SSLContext createSSLContext(KeyStore trustStore, KeyStore keyStore, String keyPassword) {
        return INSTANCE.createSSLContext(trustStore, keyStore, keyPassword);
    }

    public static boolean isInternalTlsEnabled() {
        return INSTANCE.isInternalTlsEnabled();
    }

    public static String getKeyStoreType() {
        return INSTANCE.getKeyStoreType();
    }

    public static String getTrustStoreType() {
        return INSTANCE.getTrustStoreType();
    }

    public static String getKeyStorePath() {
        return INSTANCE.getKeyStorePath();
    }

    public static String getTrustStorePath() {
        return INSTANCE.getTrustStorePath();
    }

    public static String getCaCertificatePath() {
        return INSTANCE.getCaCertificatePath();
    }

    public static String getCertificateStorePassword() {
        return INSTANCE.getCertificateStorePassword();
    }

    public static KeyStore getKeyStore() {
        return INSTANCE.getKeyStore();
    }

    public static KeyStore getTrustStore() {
        return INSTANCE.getTrustStore();
    }

    public static X509KeyManager getKeyManager() {
        return INSTANCE.getKeyManager();
    }

    public static X509TrustManager getTrustManager() {
        return INSTANCE.getTrustManager();
    }

    public static SSLContext getSslContext() {
        return INSTANCE.getSslContext();
    }

    public static String selectUrl(String httpUrl, String httpsUrl) {
        return INSTANCE.isInternalTlsEnabled() ? httpsUrl : httpUrl;
    }
}
