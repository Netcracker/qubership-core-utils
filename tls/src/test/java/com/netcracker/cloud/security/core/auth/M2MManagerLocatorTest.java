package com.netcracker.cloud.security.core.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class M2MManagerLocatorTest {
    @Test
    void testM2MManagerLocator() {
        assertEquals(DummyM2MManager.class, M2MManagerLocator.getM2MManager().getClass());
    }
}
