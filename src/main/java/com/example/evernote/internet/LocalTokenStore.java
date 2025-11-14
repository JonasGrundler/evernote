package com.example.evernote.internet;
import com.example.evernote.LocalStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class LocalTokenStore {

    private final Path tokenFile;
    private String token = null;

    private static final LocalTokenStore singleton;

    static {
        LocalTokenStore ls = null;
        try {
            ls = new LocalTokenStore();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        singleton = ls;
    }

    public static LocalTokenStore getSingleton() {
        return singleton;
    }

    private LocalTokenStore() {
        tokenFile = Paths.get(LocalStore.getSingleton().getInternet_single_oauth().toString(), "access-token.txt");
    }
    public void save(String token) throws IOException {
        Objects.requireNonNull(token, "token");
        Files.createDirectories(tokenFile.getParent());
        Files.writeString(tokenFile, token, StandardCharsets.UTF_8);
    }
    public String load() {
        if (token == null) {
            try {
                if (Files.exists(tokenFile)) {
                    token = Files.readString(tokenFile, StandardCharsets.UTF_8);
                    if (token != null && !token.isBlank()) return token.trim();
                }
            } catch (IOException ignored) {
                token = null;
            }
        }
        return token;
    }
    public void clear() {
        try {
            Files.deleteIfExists(tokenFile);
        } catch (IOException ignored) {}
    }

}