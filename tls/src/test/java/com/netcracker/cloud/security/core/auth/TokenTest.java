package com.netcracker.cloud.security.core.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class TokenTest {
    @Test
    void testDummyToken() {
        assertNotNull(Token.DUMMY_TOKEN);
    }
}
