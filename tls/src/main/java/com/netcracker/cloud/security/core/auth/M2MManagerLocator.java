package com.netcracker.cloud.security.core.auth;

import java.util.Comparator;
import java.util.ServiceLoader;

public class M2MManagerLocator {

    public static M2MManager getM2MManager() {
        var loader = ServiceLoader.load(M2MManager.class);
        return loader
                .stream()
                .map(ServiceLoader.Provider::get)
                .max(Comparator.comparingInt(M2MManager::priority))
                .orElseThrow(()->new Error("Service loader failed to load M2MManager class: " + loader.getClass().getName()));
    }
}
