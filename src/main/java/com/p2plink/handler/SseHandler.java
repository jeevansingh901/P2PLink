package com.p2plink.handler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import com.p2plink.services.SseHub;
import java.io.IOException;

public class SseHandler implements HttpHandler {


    private final SseHub hub;

    public SseHandler(SseHub hub) {
        this.hub = hub;
    }


    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Headers h = exchange.getResponseHeaders();
        h.add("Access-Control-Allow-Origin", "*");
        h.add("Access-Control-Allow-Methods", "GET, OPTIONS");
        h.add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String fileId = path.substring(path.lastIndexOf('/') + 1);
        Headers rh = exchange.getResponseHeaders();
        rh.add("Content-Type", "text/event-stream");
        rh.add("Cache-Control", "no-cache");
        rh.add("Connection", "keep-alive");
        exchange.sendResponseHeaders(200, 0);
        hub.subscribe(fileId, exchange);

    }
}
