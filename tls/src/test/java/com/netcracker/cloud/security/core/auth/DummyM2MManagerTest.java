package org.qubership.cloud.security.core.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DummyM2MManagerTest {
    @Test
    void testDummyImpl() {
        assertEquals(Token.DUMMY_TOKEN, new DummyM2MManager().getToken());
    }
}