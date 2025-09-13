package com.p2plink.services;

import org.mindrot.jbcrypt.BCrypt;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class FileRegistry {
    private final Map<String, FileEntry> files = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public String registerFile(String filePath,String originalName,Long expiry,boolean oneTime,String passPhrase) {
        String code;
        do {
            code = String.format("%06d", random.nextInt(1_000_000));
        } while (files.containsKey(code)); // ensure uniqueness
        long now = System.currentTimeMillis();
        long size = new File(filePath).length();
        long expiryAt=0;
        if (expiry != null) {
            if (expiry > 0) expiryAt = now + expiry;
            else expiryAt = 0;
        }
        else {
            expiryAt = now + 720000L;//2hour by Default Expiry
        }
        String passHash = (passPhrase != null && !passPhrase.isBlank())
                ? BCrypt.hashpw(passPhrase, BCrypt.gensalt())
                : null;
        files.put(code, new FileEntry(filePath,originalName,size,now,expiryAt,oneTime,passHash));
        return code;
    }

    public FileEntry getFile(String fileId) {
        return files.get(fileId);
    }
    public boolean isExpired(FileEntry entry, long now) {
        return entry != null && entry.getExpiresAt() > 0 && now > entry.getExpiresAt();
    }
    public boolean removeFile(String fileId) {
        FileEntry entry = files.remove(fileId);
        if (entry == null) return false;
        try {
            Files.deleteIfExists(Path.of(entry.getFilePath()));
        }
        catch (Exception ignored) {
            System.err.println("File not found: " + ignored.getMessage());
        }
        return true;
    }
    public int cleanupExpired() {
        int removed = 0;
        long now = System.currentTimeMillis();
        for (Map.Entry<String, FileEntry> entry : files.entrySet()) {
            if (isExpired(entry.getValue(), now)) {
                try {
                    Files.deleteIfExists(Path.of(entry.getValue().getFilePath()));
                }
                catch (Exception ignored) {
                    System.err.println("File not found: " + ignored.getMessage());
                }
                files.remove(entry.getKey());
                removed++;
            }
        }
        System.out.println("Number of Cleaned up expired files: " + removed);
        return removed;
    }


    public static class FileEntry {
        private final String filePath;
        private final String originalName;
        private final long fileSize;
        private final long createdAt;
        private final long expiresAt;
        private final boolean oneTime;
        private final String passHash;
        private volatile long downloadCount = 0;


        public FileEntry(String filePath, String originalName, long fileSize, long createdAt, long expiresAt, boolean oneTime, String passHash) {
            this.filePath = filePath;
            this.originalName = originalName;
            this.fileSize = fileSize;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
            this.oneTime = oneTime;
            this.passHash = passHash;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getOriginalName() {
            return originalName;
        }

        public long getFileSize() {
            return fileSize;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public long getExpiresAt() {
            return expiresAt;
        }

        public boolean isOneTime() {
            return oneTime;
        }

        public String getPassHash() {
            return passHash;
        }
        public long incrementDownloadCount() {
            return ++downloadCount;
        }

    }
}
