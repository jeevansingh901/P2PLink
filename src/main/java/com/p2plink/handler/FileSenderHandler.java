package com.p2plink.handler;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class FileSenderHandler implements Runnable {

    private final Socket clientSocket;
    private final String filePath;
    private final String originalName;

    public FileSenderHandler(Socket clientSocket, String filePath, String originalName) {
        this.clientSocket = clientSocket;
        this.filePath = filePath;
        this.originalName = originalName;
    }

    @Override
    public void run() {
        try (FileInputStream fileInputStream = new FileInputStream(filePath);
             OutputStream out = clientSocket.getOutputStream()) {

            long fileSize = new File(filePath).length();
            String header = String.format("Filename: %s\r\nFilesize: %d\r\n\r\n", originalName, fileSize);

            out.write(header.getBytes(StandardCharsets.UTF_8));

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            System.out.printf("File '%s' sent to %s%n", originalName, clientSocket.getInetAddress());

        } catch (IOException e) {
            System.err.println("Error sending file: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
        }
    }
}
