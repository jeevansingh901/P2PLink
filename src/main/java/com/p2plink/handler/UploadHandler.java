package com.p2plink.handler;
import com.p2plink.services.UploadService;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


import java.io.*;


public class UploadHandler implements HttpHandler {
    private final UploadService uploadService;

    public UploadHandler(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        // ✅ Handle CORS preflight first
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            String response = "Method Not Allowed "+exchange.getRequestMethod();

            exchange.sendResponseHeaders(204, response.getBytes().length); // No content
            return;
        }

        // ✅ Then only allow POST beyond this point
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            String response = "Method Not Allowed";
            exchange.sendResponseHeaders(405, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
            return;
        }

        // ✅ Real upload logic
        uploadService.handleFileUpload(exchange);
    }


    private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        exchange.sendResponseHeaders(statusCode, message.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(message.getBytes());
        }
    }
}
