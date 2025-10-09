package com.netcracker.cloud.security.core.utils.k8s;

public interface TokenSource {
    String getToken(String audience);
}
