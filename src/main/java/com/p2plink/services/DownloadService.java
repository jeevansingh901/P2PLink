package com.p2plink.services;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.mindrot.jbcrypt.BCrypt;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

public class DownloadService {

    private final FileRegistry registry;
    private final SseHub sseHub;

    public DownloadService(FileRegistry registry, SseHub sseHub) {
        this.registry = registry;
        this.sseHub = sseHub;
    }

    public void streamFile(HttpExchange exchange, String fileId) throws IOException {
        FileRegistry.FileEntry entry = registry.getFile(fileId);

        if (entry == null) {
            sendResponse(exchange, 404, "File not found");
            return;
        }
        long now = System.currentTimeMillis();
        if (registry.isExpired(entry, now)) {
            registry.removeFile(fileId);
            sendResponse(exchange, 410, "Gone: file expired");
            return;
        }
        if (entry.getPassHash() != null) {
            String provided = exchange.getRequestHeaders().getFirst("X-Passphrase");
            if (provided == null || !BCrypt.checkpw(provided, entry.getPassHash())) {
                exchange.getResponseHeaders().add("WWW-Authenticate", "Bearer realm=\"share\"");
                sendResponse(exchange, 401, "Unauthorized: passphrase required or invalid");
                return;
            }
        }

        File file = new File(entry.getFilePath());
        String fileName = entry.getOriginalName();

        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Accept-Ranges", "bytes");
        headers.add("Content-Disposition", "attachment; filename=\"" + fileName  + "\"");
        headers.add("Content-Type", "application/octet-stream");
        long total = file.length();
        String range = exchange.getRequestHeaders().getFirst("Range");
        long start = 0, end = total - 1;
        boolean partial = false;

        if (range != null && range.startsWith("bytes=")) {
            partial = true;
            String spec = range.substring(6).trim();
            if (spec.endsWith("-")) {
                start = Long.parseLong(spec.substring(0, spec.length() - 1));
            } else {
                String[] p = spec.split("-");
                start = Long.parseLong(p[0]);
                end = Long.parseLong(p[1]);
            }
            if (start < 0 || start >= total || end < start || end >= total) {
                headers.add("Content-Range", "bytes */" + total);
                sendResponse(exchange, 416, "Requested Range Not Satisfiable");
                return;
            }
        }

        if (sseHub != null) sseHub.publish(fileId, "download_started",
                "{\"fileId\":\"" + fileId + "\",\"partial\":" + partial + ",\"start\":" + start + ",\"total\":" + total + "}");

        if (partial) {
            long len = end - start + 1;
            headers.add("Content-Range", "bytes " + start + "-" + end + "/" + total);
            exchange.sendResponseHeaders(206, len);
            try (RandomAccessFile raf = new RandomAccessFile(file, "r");
                 FileChannel ch = raf.getChannel();
                 WritableByteChannel out = Channels.newChannel(exchange.getResponseBody())) {

                long pos = start;
                while (pos <= end) {
                    long xfer = ch.transferTo(pos, (end - pos + 1), out);
                    if (xfer <= 0) break;
                    pos += xfer;
                }
            }
        } else {
            exchange.sendResponseHeaders(200, total);
            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = exchange.getResponseBody()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = fis.read(buf)) != -1) {
                    os.write(buf, 0, n);
                }
            }
        }

        entry.incrementDownloadCount();

        if (entry.isOneTime()) {
            registry.removeFile(fileId);
            if (sseHub != null) sseHub.publish(fileId, "consumed", "{\"fileId\":\"" + fileId + "\"}");
        } else {
            if (sseHub != null) sseHub.publish(fileId, "download_complete", "{\"fileId\":\"" + fileId + "\"}");
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        exchange.sendResponseHeaders(statusCode, message.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(message.getBytes());
        }
    }
}
