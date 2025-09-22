package com.p2plink.handler;
import com.p2plink.services.UploadService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.Map;


public class UploadHandler implements HttpHandler {


    private final UploadService uploadService;


    public UploadHandler(UploadService uploadService) {
        this.uploadService = uploadService;
    }


    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }


        Map<String, List<String>> headers = exchange.getRequestHeaders();
        String fileName = getHeader(headers, "X-File-Name");
        String chunkIndexStr = getHeader(headers, "X-Chunk-Index");
        String totalChunksStr = getHeader(headers, "X-Total-Chunks");
        String totalSizeStr = getHeader(headers, "X-File-Size");


        if (fileName == null || chunkIndexStr == null || totalChunksStr == null || totalSizeStr == null) {
            String msg = "Missing required headers (X-File-Name, X-Chunk-Index, X-Total-Chunks, X-File-Size)";
            byte[] resp = msg.getBytes();
            exchange.sendResponseHeaders(400, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
            return;
        }


        int chunkIndex = Integer.parseInt(chunkIndexStr);
        int totalChunks = Integer.parseInt(totalChunksStr);
        long totalSize = Long.parseLong(totalSizeStr);
        // Save the chunk
        uploadService.saveChunk(fileName, chunkIndex, totalChunks, totalSize, exchange.getRequestBody());
        String code="";
        if (chunkIndex == totalChunks - 1) {
            code = uploadService.finalizeUpload(fileName);
        }
        byte[] resp = ("{\"fileId\":\"" + code + "\"}").getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, resp.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(resp);
        }
    }


    private String getHeader(Map<String, List<String>> headers, String key) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                List<String> values = entry.getValue();
                return (values != null && !values.isEmpty()) ? values.get(0) : null;
            }
        }
        return null;
    }
}
