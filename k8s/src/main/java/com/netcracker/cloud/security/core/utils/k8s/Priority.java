package com.netcracker.cloud.security.core.utils.k8s;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Priority {
    int value();
}
