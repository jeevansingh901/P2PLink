package com.p2plink.services;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.ClientInfoStatus;
import java.util.Map;
import java.util.concurrent.*;

public class SseHub {
    private final Map<String, CopyOnWriteArrayList<Client>> byFileId = new ConcurrentHashMap<>();


    private static class Client{

        final HttpExchange exchange;
        final OutputStream os;
        volatile boolean open = true;
        Client(HttpExchange ex) throws IOException {
            this.exchange = ex;
            this.os = ex.getResponseBody();
        }

    }
    public void subscribe(String fileId, HttpExchange exchange) throws IOException {
        byFileId.computeIfAbsent(fileId, k -> new CopyOnWriteArrayList<>()).add(new Client(exchange));
        publish(fileId, "hello", "{\"ok\":true}");//Dummy Data


    }

    public void unsubscribeAll(HttpExchange exchange) {
        for (var list : byFileId.values()) {
            list.removeIf(c -> c.exchange == exchange);
        }
    }

    public void publish(String fileId, String event, String jsonData) {
        var list = byFileId.get(fileId);
        if (list == null) {
            return;
        }

        byte[] payload = format(event, jsonData);

        for (Client c : list) {
            if (!c.open) continue;
            try {
                c.os.write(payload);
                c.os.flush();
            } catch (IOException e) {
                c.open = false;
                try { c.exchange.close(); } catch (Exception ignored) {}
            }
        }
        list.removeIf(c -> !c.open);
    }
    private byte[] format(String event, String json) {
        String s = "event: " + event + "\n" + "data: " + json + "\n\n";
        return s.getBytes(StandardCharsets.UTF_8);
    }
    public ScheduledFuture<?> startHeartbeats(ScheduledExecutorService ses) {
        return ses.scheduleAtFixedRate(() -> {
            for (var e : byFileId.entrySet()) {
                var list = e.getValue();
                if (list == null || list.isEmpty()) continue;
                for (Client c : list) {
                    if (!c.open) continue;
                    try {
                        c.os.write(":keepalive\n\n".getBytes(StandardCharsets.UTF_8));
                        c.os.flush();
                    } catch (IOException ex) {
                        c.open = false;
                        try { c.exchange.close(); } catch (Exception ignored) {}
                    }
                }
                list.removeIf(cl -> !cl.open);
            }
        }, 15, 15, TimeUnit.SECONDS);
    }
}
