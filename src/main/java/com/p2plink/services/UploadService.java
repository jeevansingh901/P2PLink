package com.p2plink.services;

import com.p2plink.parser.Multiparser;
import com.p2plink.utils.ParseResult;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.UUID;

public class UploadService {

    private final FileRegistry registry;
    private final SseHub sseHub;

    public UploadService(FileRegistry registry) {
        this(registry, null);
    }

    public UploadService(FileRegistry registry, SseHub sseHub) {
        this.registry = registry;
        this.sseHub = sseHub;
    }

    public void handleFileUpload(HttpExchange exchange) throws IOException {
        Headers requestHeader = exchange.getRequestHeaders();
        Headers responseHeader = exchange.getResponseHeaders();

        String uploadDir = System.getProperty("java.io.tmpdir") + File.separator + "peerlink-uploads";
        String originalName = requestHeader.getFirst("X-Filename"); // frontend can pass this
        if (originalName == null || originalName.isBlank()) {
            originalName = "unnamed-file";
        }
        String passphrase = requestHeader.getFirst("X-Passphrase");
        Long ttlMillis = null; // optional TTL
        String ttlHeader = requestHeader.getFirst("X-TTL-Millis");
        if (ttlHeader != null) {
            try {
                ttlMillis = Long.parseLong(ttlHeader);
            } catch (NumberFormatException ignored) {
                System.err.println("Couldn't parse expiry header for upload");
            }
        }
        boolean oneTime = "true".equalsIgnoreCase(requestHeader.getFirst("X-One-Time"));


        File uploadDirFile = new File(uploadDir);
        if (!uploadDirFile.exists()) {
            uploadDirFile.mkdirs();
        }
        String uniqueName = UUID.randomUUID().toString() + "-" + originalName;
        File savedFile = new File(uploadDirFile, uniqueName);
        long bytes = 0;
        try (InputStream in = exchange.getRequestBody();
             FileOutputStream out = new FileOutputStream(savedFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                bytes += bytesRead;
            }
        }
        System.out.println("Total bytes uploaded: " + bytes);
        String fileId = registry.registerFile(savedFile.getAbsolutePath(), originalName, ttlMillis, oneTime, passphrase);
       //Optional
        if (sseHub != null) {
            String payload = "{\"event\":\"upload_complete\",\"fileId\":\"" + fileId + "\",\"size\":" + bytes + "}";
            sseHub.publish(fileId, "upload", payload);
        }
        String response = "{ \"fileId\": \"" + fileId + "\", \"size\": " + bytes + ", \"oneTime\": " + oneTime + ", \"protected\": " + (passphrase != null && !passphrase.isBlank()) + " }";
        responseHeader.add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }


    private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        exchange.sendResponseHeaders(statusCode, message.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(message.getBytes());
        }
    }
}
