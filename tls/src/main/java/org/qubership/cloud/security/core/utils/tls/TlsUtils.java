package org.qubership.cloud.security.core.utils.tls;

import org.jetbrains.annotations.NotNull;
import org.qubership.cloud.test.ExcludeFromJacocoGeneratedReport;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.util.Comparator;
import java.util.ServiceLoader;

@SuppressWarnings("unused")
public class TlsUtils {
    private final static TlsConfig INSTANCE = getTlsConfig();

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

    @ExcludeFromJacocoGeneratedReport
    public static SSLContext createSSLContext(KeyStore trustStore, KeyStore keyStore, String keyPassword) {
        return INSTANCE.createSSLContext(trustStore, keyStore, keyPassword);
    }

    @ExcludeFromJacocoGeneratedReport
    public static boolean isInternalTlsEnabled() {
        return INSTANCE.isInternalTlsEnabled();
    }

    @ExcludeFromJacocoGeneratedReport
    public static String getKeyStoreType() {
        return INSTANCE.getKeyStoreType();
    }

    @ExcludeFromJacocoGeneratedReport
    public static String getTrustStoreType() {
        return INSTANCE.getTrustStoreType();
    }

    @ExcludeFromJacocoGeneratedReport
    public static String getKeyStorePath() {
        return INSTANCE.getKeyStorePath();
    }

    @ExcludeFromJacocoGeneratedReport
    public static String getTrustStorePath() {
        return INSTANCE.getTrustStorePath();
    }

    @ExcludeFromJacocoGeneratedReport
    public static String getCaCertificatePath() {
        return INSTANCE.getCaCertificatePath();
    }

    @ExcludeFromJacocoGeneratedReport
    public static String getCertificateStorePassword() {
        return INSTANCE.getCertificateStorePassword();
    }

    @ExcludeFromJacocoGeneratedReport
    public static KeyStore getKeyStore() {
        return INSTANCE.getKeyStore();
    }

    @ExcludeFromJacocoGeneratedReport
    public static KeyStore getTrustStore() {
        return INSTANCE.getTrustStore();
    }

    @ExcludeFromJacocoGeneratedReport
    public static X509KeyManager getKeyManager() {
        return INSTANCE.getKeyManager();
    }

    @ExcludeFromJacocoGeneratedReport
    public static X509TrustManager getTrustManager() {
        return INSTANCE.getTrustManager();
    }

    @ExcludeFromJacocoGeneratedReport
    public static SSLContext getSslContext() {
        return INSTANCE.getSslContext();
    }

    @ExcludeFromJacocoGeneratedReport
    public static String selectUrl(String httpUrl, String httpsUrl) {
        return INSTANCE.isInternalTlsEnabled() ? httpsUrl : httpUrl;
    }
}
