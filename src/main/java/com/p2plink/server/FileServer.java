package com.p2plink.server;
import com.p2plink.services.FileRegistry;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class FileServer implements Runnable {

    private final int port;
    private final FileRegistry registry;

    public FileServer(int port, FileRegistry registry) {
        this.port = port;
        this.registry = registry;
    }

    @Override
    public void run() {
        try (Selector selector = Selector.open();
             ServerSocketChannel serverChannel = ServerSocketChannel.open()) {

            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("File Server listening on port " + port);

            while (true) {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> it = selectedKeys.iterator();

                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();

                    if (key.isAcceptable()) {
                        handleAccept(serverChannel, selector);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("File Server Error: " + e.getMessage());
        }
    }

    private void handleAccept(ServerSocketChannel serverChannel, Selector selector) throws IOException {
        SocketChannel client = serverChannel.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));
        System.out.println("Client connected: " + client.getRemoteAddress());
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();

        int bytesRead = client.read(buffer);
        if (bytesRead == -1) {
            client.close();
            return;
        }
        String request = new String(buffer.array(), 0, buffer.position()).trim();
        if (request.endsWith("\n")) {
            buffer.clear();
            String fileId = request.replace("GET ", "").trim();

            FileRegistry.FileEntry entry = registry.getFile(fileId);
            if (entry == null) {
                client.write(ByteBuffer.wrap("File not found\n".getBytes()));
                client.close();
                return;
            }

            sendFile(client, entry.getFilePath());
            client.close();
        }
    }

    private void sendFile(SocketChannel client, String filePath) throws IOException {
        File file = new File(filePath);
        try (FileChannel fileChannel = new FileInputStream(file).getChannel()) {
            long position = 0;
            long size = fileChannel.size();

            while (position < size) {
                long transferred = fileChannel.transferTo(position, size - position, client);
                position += transferred;
            }

            System.out.printf("Sent file %s (%d bytes) to %s%n",
                    file.getName(), size, client.getRemoteAddress());
        }
    }
}

