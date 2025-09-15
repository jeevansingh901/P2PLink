package com.p2plink.controller;

import com.p2plink.handler.CorsHandler;
import com.p2plink.handler.DownloadHandler;
import com.p2plink.handler.UploadHandler;
import com.p2plink.server.FileServer;
import com.p2plink.services.FileRegistry;
import com.p2plink.services.SseHub;
import com.p2plink.services.UploadService;
import com.p2plink.services.DownloadService;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FileController {

    private final HttpServer server;
    private final String uploadDir;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduled;


    public FileController(int httpPort, int nioPort) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("0.0.0.0", httpPort), 0);
        this.uploadDir = System.getProperty("java.io.tmpdir") + File.separator + "peerlink-uploads";
        this.executorService = Executors.newFixedThreadPool(16);
        this.scheduled = Executors.newScheduledThreadPool(2);

        initUploadDir();

        FileRegistry registry = new FileRegistry();
        SseHub sseHub = new SseHub();
        sseHub.startHeartbeats(scheduled);

        // Services
        UploadService uploadService = new UploadService(registry,sseHub);
        DownloadService downloadService = new DownloadService(registry,sseHub);
        new Thread(new FileServer(nioPort, registry)).start();

        // REST endpoints
        server.createContext("/upload", new UploadHandler(uploadService));
        server.createContext("/download", new DownloadHandler(downloadService));
        server.createContext("/", new CorsHandler());
        server.createContext("/api/health", exchange -> {
            String response = "OK";
            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });
        scheduled.scheduleAtFixedRate(() -> {
            int removed = registry.cleanupExpired();
            if (removed > 0) {
                System.out.println("Cleanup removed " + removed + " expired files");
            }
        }, 1, 1, TimeUnit.MINUTES);

        server.setExecutor(executorService);
    }

    private void initUploadDir() {
        File uploadDirFile = new File(uploadDir);
        if (!uploadDirFile.exists()) {
            uploadDirFile.mkdirs();
        }
    }

    public void start() {
        server.start();
        System.out.println("HTTP API server started on port " + server.getAddress().getPort());
    }

    public void stop() {
        server.stop(0);
        executorService.shutdown();
        System.out.println("API server stopped");
    }
}
