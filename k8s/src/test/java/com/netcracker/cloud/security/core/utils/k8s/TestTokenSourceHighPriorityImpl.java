package com.netcracker.cloud.security.core.utils.k8s;

@Priority(10)
public class TestTokenSourceHighPriorityImpl implements TokenSource {
    @Override
    public String getToken(String audience) {
        return "test-token";
    }

    @Override
    public void close() throws Exception {
    }
}

