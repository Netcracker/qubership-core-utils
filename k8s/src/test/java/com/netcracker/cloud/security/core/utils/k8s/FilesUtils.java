package com.netcracker.cloud.security.core.utils.k8s;

import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FilesUtils {

    public static boolean tryCreateSymbolicLink(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
            return true;
        } catch (UnsupportedOperationException | SecurityException | IOException e) {
            return false;
        }
    }

    @SneakyThrows
    public static Path createTimestampDir(Path storageRoot) {
        var timestamp = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss.SSSSSSSSS").format(LocalDateTime.now());
        var timestampDir = storageRoot.resolve(".." + timestamp);
        return Files.createDirectories(timestampDir);
    }
}
