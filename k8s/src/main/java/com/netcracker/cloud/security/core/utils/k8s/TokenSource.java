package com.netcracker.cloud.security.core.utils.k8s;

public interface TokenSource extends AutoCloseable {
    String getToken(String audience);
}
