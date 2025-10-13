package com.example.evernote;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Locale;

public class TrOcrClient {
    private final HttpClient http;
    private final ObjectMapper om = new ObjectMapper();
    private final URI base;

    public TrOcrClient(String baseUrl) {
        this.base = URI.create(baseUrl.endsWith("/") ? baseUrl : baseUrl + "/");
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    private JsonNode postBytes(String path, byte[] imageBytes) throws Exception {
        // „top“ immer mit Punkt als Dezimaltrenner
        HttpRequest req = HttpRequest.newBuilder(base.resolve(path))
                .header("Content-Type", "application/octet-stream")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofByteArray(imageBytes))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 != 2) {
            throw new IllegalStateException("TrOCR HTTP " + res.statusCode() + " – " + res.body());
        }
        return om.readTree(res.body());
    }

    /** Freitext aus oberem Bildbereich (Server-Endpoint /ocr). */
    public String ocr(byte[] imageBytes) throws Exception {
        String boundary = "----JavaBoundary" + System.currentTimeMillis();
        var body = new java.io.ByteArrayOutputStream();
        var CRLF = "\r\n";
        body.write(("--" + boundary + CRLF).getBytes());
        body.write(("Content-Disposition: form-data; name=\"file\"; filename=\"image.jpg\"" + CRLF).getBytes());
        body.write(("Content-Type: application/octet-stream" + CRLF + CRLF).getBytes());
        body.write(imageBytes); // deine Bildbytes
        body.write((CRLF + "--" + boundary + "--" + CRLF).getBytes());

        var req = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://127.0.0.1:8000/ocr"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();

        var res = java.net.http.HttpClient.newHttpClient().send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
        System.out.println(res.statusCode() + " " + res.body());
        return res.body().toString();

    }

}
