package com.netcracker.cloud.security.core.utils.k8s;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TryTest {

    @Test
    void testSuccessCreation() {
        Try<String> success = Try.success("test value");
        assertEquals("test value", success.getOrThrow());
    }

    @Test
    void testFailureGetOrThrow() {
        RuntimeException exception = new RuntimeException("test error");
        Try<String> failure = Try.failure(exception);

        RuntimeException thrown = assertThrows(RuntimeException.class, failure::getOrThrow);
        assertEquals("test error", thrown.getMessage());
        assertSame(exception, thrown);
    }

    @Test
    void testOfSuccess() {
        assertEquals("test", Try.of(() -> "test").getOrThrow());
    }

    @Test
    void testOfFailure() {
        assertThrows(RuntimeException.class, () ->
                Try.of(() -> { throw new RuntimeException("Oops"); }).getOrThrow()
        );
    }
}
