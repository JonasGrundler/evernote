package com.example.evernote.internet;


import java.io.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;


public class TrOcrClient {

    private final String ocrUrl = "http://127.0.0.1:8000/ocr";
    private final HttpClient http;

    private BufferedReader pyOut = null;

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

    public static void main(String[] args) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] imageBytes = Files.readAllBytes(Paths.get("C:\\Users\\Jonas\\.jg-evernote\\images-tmp\\3.png"));
            String json = TrOcrClient.getSingleton().ocr(imageBytes);
            System.out.println("json:" + json);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    public static TrOcrClient getSingleton() {
        return singleton;
    }

    private TrOcrClient() throws IOException {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        ProcessBuilder pbUvicorn = new ProcessBuilder(System.getenv("UVICORN_PYTHON") + "\\python.exe", "-m", "uvicorn", "server:app", "--host", "127.0.0.1", "--port", "8000");
        pbUvicorn.directory(new File(System.getenv("UVICORN_SERVER")));

        pbUvicorn.redirectErrorStream(true);

        System.out.println("starting uvicorn server ...");
        Process process = pbUvicorn.start();

        pyOut = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

        try {
            Thread.sleep(30000);
        } catch (Exception e) {
        }

        char[] c = new char[4096];
        int count = pyOut.read(c);

        System.out.println(String.valueOf(c, 0, count));
        System.out.println("uvicorn server ready");
    }

    /** Freitext aus oberem Bildbereich (Server-Endpoint /ocr). */
    public String ocr(byte[] imageBytes) throws Exception {
        String boundary = "----JavaBoundary" + System.currentTimeMillis();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        String CRLF = "\r\n";
        body.write(("--" + boundary + CRLF).getBytes(StandardCharsets.UTF_8));
        body.write(("Content-Disposition: form-data; name=\"file\"; filename=\"image.png\"" + CRLF).getBytes(StandardCharsets.UTF_8));
        body.write(("Content-Type: application/octet-stream" + CRLF + CRLF).getBytes(StandardCharsets.UTF_8));
        body.write(imageBytes); // deine Bildbytes
        body.write((CRLF + "--" + boundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8));

        String header = "multipart/form-data; boundary=" + boundary;

        HttpRequest req = null;
        HttpResponse res = null;

        try {
            req = HttpRequest.newBuilder()
                    .uri(java.net.URI.create(ocrUrl))
                    .header("Content-Type", header)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                    .build();

            res = http.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        String response = null;

        if (res != null) {
            System.out.println(res.statusCode() + " " + res.body());
            response = res.body() == null ? null : res.body().toString();
        }

        char[] c = new char[4096];
        int count = pyOut.read(c);

        return response;
    }

}
