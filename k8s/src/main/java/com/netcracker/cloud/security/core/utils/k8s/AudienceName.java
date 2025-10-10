package com.netcracker.cloud.security.core.utils.k8s;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AudienceName {
    /**
     * NETCRACKER audience is used for getting a token for M2M communication between microservices in the same namespace
     */
    public static final String NETCRACKER = "netcracker";
    /**
     * DBAAS audience is used for getting a token for making requests to DBaaS infra service
     */
    public static final String DBAAS = "dbaas";
    /**
     * MAAS audience is used for getting a token for making requests to MaaS infra service
     */
    public static final String MAAS = "maas";
}
