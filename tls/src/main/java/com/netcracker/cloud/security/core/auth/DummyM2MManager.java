package org.qubership.cloud.security.core.auth;

public class DummyM2MManager implements M2MManager {
    @Override
    public Token getToken() {
        return Token.DUMMY_TOKEN;
    }
}
