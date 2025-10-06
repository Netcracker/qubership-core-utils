package com.netcracker.cloud.security.core.utils.k8s;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KubernetesTokenSourceTest {
    @Test
    void getToken() {
        assertEquals("test-token", KubernetesTokenSource.getToken("never-mind"));
    }
}
