package com.p2plink.handler;

import com.p2plink.services.DownloadService;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class DownloadHandler implements HttpHandler {

    private final DownloadService downloadService;

    public DownloadHandler(DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET");

        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String fileId = path.substring(path.lastIndexOf('/') + 1);

        downloadService.streamFile(exchange, fileId);
    }
}
