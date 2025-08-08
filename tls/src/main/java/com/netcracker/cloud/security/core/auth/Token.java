package org.qubership.cloud.security.core.auth;


import lombok.Value;

import java.time.Instant;

@Value
public class Token {
    public static final Token DUMMY_TOKEN = new Token(
            "Bearer",
            "dummy",
                Instant.parse("2025-02-13T10:15:30Z"), // use explicit fixed values for better testability
                Instant.parse("2040-02-13T10:15:30Z")
            );

    String tokenType;
    String tokenValue;
    Instant issuedAt;
    Instant expiresAt;
}