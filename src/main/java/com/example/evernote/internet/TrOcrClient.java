package com.example.evernote.internet;

import com.example.evernote.ImageToText;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;


public class TrOcrClient {

    private final String baseUrl = "http://127.0.0.1:8000";
    private final HttpClient http;
    private final ObjectMapper om = new ObjectMapper();
    private final URI base;

    private static final TrOcrClient singleton;

    static {
        TrOcrClient ls = null;
        try {
            ls = new TrOcrClient();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        singleton = ls;
    }

    public static TrOcrClient getSingleton() {
        return singleton;
    }

    private TrOcrClient() throws IOException {
        this.base = URI.create(baseUrl.endsWith("/") ? baseUrl : baseUrl + "/");
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        ProcessBuilder pbUvicorn = new ProcessBuilder(System.getenv("UVICORN") + "\\.python\\Scripts\\python", "-m uvicorn server:app --host 127.0.0.1 --port 8000");
        pbUvicorn.directory(new File(System.getenv("UVICORN") + "\\trocr-service"));

        pbUvicorn.redirectErrorStream(true);

        System.out.println("starting uvicorn server ...");
        Process process = pbUvicorn.start();

        BufferedReader pyOut = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

        try {
            Thread.sleep(30000);
        } catch (Exception e) {
        }

        char[] c = new char[4096];
        int count = pyOut.read(c);

        System.out.println(String.valueOf(c, 0, count));
        System.out.println("uvicorn server ready");
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
