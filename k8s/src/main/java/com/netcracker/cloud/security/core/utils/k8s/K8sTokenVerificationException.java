package com.netcracker.cloud.security.core.utils.k8s;

public class K8sTokenVerificationException extends Exception {
    public K8sTokenVerificationException(String msg) {
        super(msg);
    }

    public K8sTokenVerificationException(String msg, Throwable e) {
        super(msg, e);
    }

    public K8sTokenVerificationException(Throwable e) {
        super(e);
    }
}
