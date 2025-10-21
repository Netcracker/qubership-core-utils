package com.netcracker.cloud.security.core.utils.k8s;

import java.util.HashMap;
import java.util.Map;

public class SystemPropertiesTestHelper {

    @FunctionalInterface
    public interface OmnivoreRunnable {
        void run() throws Exception;
    }

    public static void withProperty(Map<String, String> properties, OmnivoreRunnable runnable) throws Exception {
        var previousValues = new HashMap<String, String>();
        properties.forEach((name, value) -> {
            previousValues.put(name, System.getProperty(name));
            System.setProperty(name, value);
        });
        try {
            runnable.run();
        } finally {
            previousValues.forEach((name, previousValue) -> {
                if (previousValue != null) {
                    System.setProperty(name, previousValue);
                } else {
                    System.clearProperty(name);
                }
            });
        }
    }
}
