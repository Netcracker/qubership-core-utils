package com.netcracker.cloud.security.core.utils.k8s;

@Priority(1)
public class TestTokenSourceLowPriorityImpl implements TokenSource {
    @Override
    public String getToken(String audience) {
        throw new IllegalStateException("Unexpected call");
    }
}
