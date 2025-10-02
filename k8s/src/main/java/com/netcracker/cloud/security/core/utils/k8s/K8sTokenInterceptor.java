package com.netcracker.cloud.security.core.utils.k8s;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

@Slf4j
public class K8sTokenInterceptor implements Interceptor {
    private TokenSource tokenSource;

    public K8sTokenInterceptor(TokenSource tokenSource) {
        this.tokenSource = tokenSource;
    }

    public Response intercept(Interceptor.Chain chain) throws IOException {
        Request requestWithUserAgent = chain.request()
                .newBuilder()
                .header("Authorization", "Bearer " + tokenSource.getToken())
                .build();

        return chain.proceed(requestWithUserAgent);
    }
}

