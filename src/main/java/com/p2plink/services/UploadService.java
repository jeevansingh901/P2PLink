package com.p2plink.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class UploadService {

    private final Path uploadDir = Paths.get("uploads");
    private final FileRegistry registry;
    private final SseHub sseHub;

    // Track upload progress by fileName
    private final ConcurrentHashMap<String, AtomicLong> uploadedBytesMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> totalSizeMap = new ConcurrentHashMap<>();

    public UploadService(FileRegistry registry, SseHub sseHub) {
        this.registry = registry;
        this.sseHub = sseHub;
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create upload dir", e);
        }
    }

    public void saveChunk(String fileName, int chunkIndex, int totalChunks, long totalSize, InputStream in) throws IOException {
        Path partFile = uploadDir.resolve(fileName + ".part");
        totalSizeMap.putIfAbsent(fileName, totalSize);

        long bytesThisChunk = 0;
        try (OutputStream out = Files.newOutputStream(partFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                bytesThisChunk += bytesRead;
            }
        }
        uploadedBytesMap.computeIfAbsent(fileName, k -> new AtomicLong(0)).addAndGet(bytesThisChunk);
        long uploaded = uploadedBytesMap.get(fileName).get();
        long total = totalSizeMap.get(fileName);
        double percent = (double) uploaded / total * 100.0;
        String json = String.format("{\"uploaded\":%d,\"total\":%d,\"percent\":%.2f}", uploaded, total, percent);
        sseHub.publish(fileName, "progress", json);
    }

    public String finalizeUpload(String fileName) throws IOException {
        Path partFile = uploadDir.resolve(fileName + ".part");
        Path finalFile = uploadDir.resolve(fileName);
        Files.move(partFile, finalFile, StandardCopyOption.REPLACE_EXISTING);
        String code = registry.registerFile(finalFile.toString(), fileName, null, false, null);
        sseHub.publish(fileName, "completed", "{\"status\":\"completed\",\"code\":\"" + code + "\"}");
        return code;
    }
}
