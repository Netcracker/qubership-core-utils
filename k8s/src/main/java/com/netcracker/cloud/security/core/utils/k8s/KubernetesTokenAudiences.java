package com.netcracker.cloud.security.core.utils.k8s;

public final class KubernetesTokenAudiences {
	/**
     * NETCRACKER audience is used for getting a token for M2M communication between microservices in the same namespace
     */
    public static final String NETCRACKER = "netcracker";
	/**
     * DBAAS audience is used for getting a token for making requests to Dbaas infra service
     */
    public static final String DBAAS = "dbaas";
	/**
     * MAAS audience is used for getting a token for making requests to Maas infra service
     */
    public static final String MAAS = "maas";
}
