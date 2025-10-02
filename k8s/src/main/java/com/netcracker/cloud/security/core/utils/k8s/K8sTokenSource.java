package com.netcracker.cloud.security.core.utils.k8s;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class K8sTokenSource {
    private static final String defaultTokenDir = "/var/run/secrets/tokens";
    private static final Object lock = new Object();

    static final Map<String, FileTokenSource> tokenSources = new ConcurrentHashMap<>();

    public static String getToken(String audience) throws IOException {
        FileTokenSource tokenSource = tokenSources.get(audience);
        if (tokenSource != null) {
            return tokenSource.getToken();
        }
        return createTokenSource(defaultTokenDir, audience).getToken();
    }

    public static TokenSource getTokenSource(String audience) throws IOException {
        FileTokenSource tokenSource = tokenSources.get(audience);
        if (tokenSource != null) {
            return tokenSource;
        }
        return createTokenSource(defaultTokenDir, audience);
    }

    static FileTokenSource createTokenSource(String tokenDir, String audience) throws IOException {
        synchronized (lock) {
            FileTokenSource tokenSource = tokenSources.get(audience);
            if (tokenSource != null) {
                return tokenSource;
            }
            tokenSource = new FileTokenSource(Path.of(tokenDir, audience));
            tokenSources.put(audience, tokenSource);
            return tokenSource;
        }
    }

    public static void close() {
        for (Map.Entry<String, FileTokenSource> entry : tokenSources.entrySet()) {
            entry.getValue().close();
        }
    }

    private static class FileTokenSource implements TokenSource {
        static final String tokenFileLinkName = "..data";
        static final String tokenFileName = "token";
        private final Object lock = new Object();
        private final Path tokenDir;

        private final WatchService watchService;
        Thread watcherThread;
        private IOException tokenRefreshException;
        private String token;

        private FileTokenSource(Path tokenDir) throws IOException {
            this.tokenDir = tokenDir;
            refreshToken();
            watchService = FileSystems.getDefault().newWatchService();
            tokenDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
            watcherThread = Thread.startVirtualThread(this::listenFs);
        }

        private void listenFs() {
            try {
                WatchKey key;
                while ((key = watchService.take()) != null) {
                    handleEvent(key);
                }
            } catch (IOException e) {
                synchronized (lock) {
                    tokenRefreshException = e;
                }
            } catch (InterruptedException e) {
                log.error("K8sTokenWatcher listening thread interrupted. Stopping FileTokenSource", e);
            }
        }

        public String getToken() throws IOException {
            synchronized (lock) {
                if (tokenRefreshException != null) {
                    throw tokenRefreshException;
                }
                return token;
            }
        }

        private void handleEvent(WatchKey key) throws IOException {
            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.kind() != StandardWatchEventKinds.ENTRY_CREATE) {
                    continue;
                }
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                if (!tokenFileLinkName.equals(ev.context().getFileName().toString())) {
                    continue;
                }
                synchronized (lock) {
                    refreshToken();
                    tokenRefreshException = null;
                }
            }
            key.reset();
        }

        private void refreshToken() throws IOException {
            Path tokenFilePath = tokenDir.resolve(tokenFileName);
            token = Files.readString(tokenFilePath);
        }

        private void close() {
            watcherThread.interrupt();
        }
    }
}
