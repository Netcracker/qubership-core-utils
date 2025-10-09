package com.netcracker.cloud.security.core.utils.k8s;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class K8sTokenVerificationExceptionTest {

    @Test
    void testConstructorWithMessage() {
        String expectedMessage = "Token verification failed";
        K8sTokenVerificationException exception = new K8sTokenVerificationException(expectedMessage);

        assertEquals(expectedMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        String expectedMessage = "Token verification failed";
        RuntimeException cause = new RuntimeException("Invalid token format");
        K8sTokenVerificationException exception = new K8sTokenVerificationException(expectedMessage, cause);

        assertEquals(expectedMessage, exception.getMessage());
        assertSame(cause, exception.getCause());
        assertEquals("Invalid token format", exception.getCause().getMessage());
    }

    @Test
    void testConstructorWithCause() {
        RuntimeException cause = new RuntimeException("Network error");
        K8sTokenVerificationException exception = new K8sTokenVerificationException(cause);

        assertSame(cause, exception.getCause());
        assertEquals("java.lang.RuntimeException: Network error", exception.getMessage());
    }

    @Test
    void testExceptionCanBeThrown() {
        assertThrows(K8sTokenVerificationException.class, () -> {
            throw new K8sTokenVerificationException("Test exception");
        });
    }

    @Test
    void testExceptionWithNullMessage() {
        K8sTokenVerificationException exception = new K8sTokenVerificationException((String) null);

        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testExceptionWithNullCause() {
        K8sTokenVerificationException exception = new K8sTokenVerificationException((Throwable) null);

        assertNull(exception.getCause());
    }

    @Test
    void testExceptionWithMessageAndNullCause() {
        String expectedMessage = "Token expired";
        K8sTokenVerificationException exception = new K8sTokenVerificationException(expectedMessage, null);

        assertEquals(expectedMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testExceptionInheritanceFromException() {
        K8sTokenVerificationException exception = new K8sTokenVerificationException("Test");

        assertInstanceOf(Exception.class, exception);
        assertInstanceOf(Throwable.class, exception);
    }

    @Test
    void testExceptionWithNestedCause() {
        Exception rootCause = new IllegalArgumentException("Root cause");
        RuntimeException intermediateCause = new RuntimeException("Intermediate cause", rootCause);
        K8sTokenVerificationException exception = new K8sTokenVerificationException("Top level message", intermediateCause);

        assertEquals("Top level message", exception.getMessage());
        assertSame(intermediateCause, exception.getCause());
        assertSame(rootCause, exception.getCause().getCause());
    }

    @Test
    void testExceptionStackTrace() {
        K8sTokenVerificationException exception = new K8sTokenVerificationException("Stack trace test");

        assertNotNull(exception.getStackTrace());
        assertTrue(exception.getStackTrace().length > 0);
    }
}

