package com.netcracker.cloud.security.core.utils.k8s;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KubernetesAudienceTokenTest {
    @Test
    void getToken() {
        assertEquals("test-token", KubernetesAudienceToken.getToken("never-mind"));
    }
}
