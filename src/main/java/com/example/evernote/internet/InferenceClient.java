package com.example.evernote.internet;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class InferenceClient {

    private BufferedWriter infPyIn;
    private BufferedReader infPyOut;
    private final String suffix;

    public InferenceClient(String suffix) {

        this.suffix = suffix;

        ProcessBuilder pbInf = new ProcessBuilder("python", "C:\\Users\\Jonas\\IdeaProjects\\PaddleOCR\\inference_tags.py", "--suffix", suffix);
        pbInf.redirectErrorStream(true);

        try {
            System.out.println("starting inference ...");
            Process process = pbInf.start();

            infPyOut = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            infPyIn = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));

            String line;

            while ((line = infPyOut.readLine()) != null && ! line.equals("inference ready")) {
                System.out.println(line);
            }

            System.out.println(line);

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    public String infer(File oneRowCSV) throws IOException {
        String line;
        while ((line = infPyOut.readLine()) == null || ! line.trim().equals("waiting")) {
            if (line != null) {
                System.out.println("inference:" + line);
            }
        }
        infPyIn.write(oneRowCSV.getAbsolutePath());
        infPyIn.newLine();
        infPyIn.flush();
        // read first...
        String predictedTags = infPyOut.readLine();
        while ((line = infPyOut.readLine()) == null || ! line.trim().equals("done"));
        return predictedTags;
    }
}
