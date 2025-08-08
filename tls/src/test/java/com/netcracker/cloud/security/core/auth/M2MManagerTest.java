package org.qubership.cloud.security.core.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class M2MManagerTest {

    @Test
    void priority() {
        M2MManager m2MManager = () -> Token.DUMMY_TOKEN;
        assertEquals(0, m2MManager.priority());
    }
}
