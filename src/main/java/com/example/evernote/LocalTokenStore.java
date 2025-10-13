package com.example.evernote;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
/** Einfacher lokaler Token-Store (~/.evernote-oauth/access-token.txt).
 *  In Produktion: sicher verschlüsseln & mit restrictiven Dateirechten speichern. */
public class LocalTokenStore {
    private final Path tokenFile;
    public LocalTokenStore() {
        String userHome = System.getProperty("user.home");
        this.tokenFile = Paths.get(userHome, ".evernote-oauth", "access-token.txt");
    }
    public void save(String token) throws IOException {
        Objects.requireNonNull(token, "token");
        Files.createDirectories(tokenFile.getParent());
        Files.writeString(tokenFile, token, StandardCharsets.UTF_8);
    }
    public String load() {
        try {
            if (Files.exists(tokenFile)) {
                String token = Files.readString(tokenFile, StandardCharsets.UTF_8);
                if (token != null && !token.isBlank()) return token.trim();
            }
        } catch (IOException ignored) {}
        return null;
    }
    public void clear() {
        try { Files.deleteIfExists(tokenFile); } catch (IOException ignored) {}
    }
    public Path path() { return tokenFile; }

}