package com.netcracker.cloud.security.core.utils.k8s;

import java.io.IOException;

public interface TokenSource {
    String getToken() throws IOException;
}
