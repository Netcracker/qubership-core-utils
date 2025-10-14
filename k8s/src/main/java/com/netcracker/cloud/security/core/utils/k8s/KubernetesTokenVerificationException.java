package com.netcracker.cloud.security.core.utils.k8s;

public class KubernetesTokenVerificationException extends Exception {
    public KubernetesTokenVerificationException(String msg) {
        super(msg);
    }

    public KubernetesTokenVerificationException(String msg, Throwable e) {
        super(msg, e);
    }

    public KubernetesTokenVerificationException(Throwable e) {
        super(e);
    }
}
