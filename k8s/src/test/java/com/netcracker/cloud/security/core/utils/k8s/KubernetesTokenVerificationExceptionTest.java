package com.netcracker.cloud.security.core.utils.k8s;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KubernetesTokenVerificationExceptionTest {

    @Test
    void testConstructorWithMessage() {
        String expectedMessage = "Token verification failed";
        KubernetesTokenVerificationException exception = new KubernetesTokenVerificationException(expectedMessage);

        assertEquals(expectedMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        String expectedMessage = "Token verification failed";
        RuntimeException cause = new RuntimeException("Invalid token format");
        KubernetesTokenVerificationException exception = new KubernetesTokenVerificationException(expectedMessage, cause);

        assertEquals(expectedMessage, exception.getMessage());
        assertSame(cause, exception.getCause());
        assertEquals("Invalid token format", exception.getCause().getMessage());
    }

    @Test
    void testConstructorWithCause() {
        RuntimeException cause = new RuntimeException("Network error");
        KubernetesTokenVerificationException exception = new KubernetesTokenVerificationException(cause);

        assertSame(cause, exception.getCause());
        assertEquals("java.lang.RuntimeException: Network error", exception.getMessage());
    }

    @Test
    void testExceptionCanBeThrown() {
        assertThrows(KubernetesTokenVerificationException.class, () -> {
            throw new KubernetesTokenVerificationException("Test exception");
        });
    }

    @Test
    void testExceptionWithNullMessage() {
        KubernetesTokenVerificationException exception = new KubernetesTokenVerificationException((String) null);

        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testExceptionWithNullCause() {
        KubernetesTokenVerificationException exception = new KubernetesTokenVerificationException((Throwable) null);

        assertNull(exception.getCause());
    }

    @Test
    void testExceptionWithMessageAndNullCause() {
        String expectedMessage = "Token expired";
        KubernetesTokenVerificationException exception = new KubernetesTokenVerificationException(expectedMessage, null);

        assertEquals(expectedMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testExceptionInheritanceFromException() {
        KubernetesTokenVerificationException exception = new KubernetesTokenVerificationException("Test");

        assertInstanceOf(Exception.class, exception);
        assertInstanceOf(Throwable.class, exception);
    }

    @Test
    void testExceptionWithNestedCause() {
        Exception rootCause = new IllegalArgumentException("Root cause");
        RuntimeException intermediateCause = new RuntimeException("Intermediate cause", rootCause);
        KubernetesTokenVerificationException exception = new KubernetesTokenVerificationException("Top level message", intermediateCause);

        assertEquals("Top level message", exception.getMessage());
        assertSame(intermediateCause, exception.getCause());
        assertSame(rootCause, exception.getCause().getCause());
    }

    @Test
    void testExceptionStackTrace() {
        KubernetesTokenVerificationException exception = new KubernetesTokenVerificationException("Stack trace test");

        assertNotNull(exception.getStackTrace());
        assertTrue(exception.getStackTrace().length > 0);
    }
}

