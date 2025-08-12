package com.netcracker.cloud.security.core.utils.tls;


@SuppressWarnings("unused")
public class TlsInitializationException extends RuntimeException {
    public TlsInitializationException() {
        super();
    }

    public TlsInitializationException(String msg) {
        super(msg);
    }

    public TlsInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public TlsInitializationException(Throwable cause) {
        super(cause);
    }
}
