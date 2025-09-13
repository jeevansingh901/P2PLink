package com.p2plink.services;

import com.p2plink.handler.FileSenderHandler;
import com.p2plink.utils.UploadUtils;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileSharer {

    private final Map<Integer, String> availableFiles;
    private final Map<Integer, String> originalNames;

    public FileSharer() {
        this.availableFiles = new ConcurrentHashMap<>();
        this.originalNames = new ConcurrentHashMap<>();
    }

    public String getOriginalName(int port) {
        String filePath = availableFiles.get(port);
        if (filePath == null) return null;
        return originalNames.getOrDefault(port, new File(filePath).getName());
    }

    public int offerFile(String filePath, String originalName) {
        int retries = 50;
        while (retries-- > 0) {
            int port = UploadUtils.generateCode();
            if (!availableFiles.containsKey(port)) {
                availableFiles.put(port, filePath);
                originalNames.put(port, originalName);
                return port;
            }
        }
        throw new IllegalStateException("Unable to allocate unique port for file sharing");
    }

    public void startFileServer(int port) {
        String filePath = availableFiles.get(port);
        String originalName = originalNames.get(port);

        if (filePath == null) {
            System.err.println("No file mapped to port " + port);
            return;
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.printf("Serving file '%s' on port %d%n", new File(filePath).getName(), port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
                new Thread(new FileSenderHandler(clientSocket, filePath, originalName)).start();
            }
        } catch (IOException e) {
            System.err.println("Error starting file server on port " + port + ": " + e.getMessage());
        }
    }
}
