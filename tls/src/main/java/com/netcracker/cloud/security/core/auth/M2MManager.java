package org.qubership.cloud.security.core.auth;

public interface M2MManager {
    Token getToken();

    default int priority() {
        return 0;
    }
}
