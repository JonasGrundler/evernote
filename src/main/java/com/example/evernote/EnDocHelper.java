package com.example.evernote;

import org.apache.tika.Tika;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * either per .enex-File (they share a common directory then), or per single file
 * enex: numbered files
 * internet: guid
 */
public abstract class EnDocHelper {

    private String info;
    private Path targetDir;

    private int redo;
    private int images;
    private EvernoteDoc enDoc;

    public static final int REDO_NOT = 0;
    public static final int REDO_ALL = 1;
    public static final int REDO_TEXT = 2;

    public EnDocHelper(Path targetDir, int redo, String info) throws Exception {
        this.targetDir = targetDir;
        this.redo = redo;
        this.info = info;

        this.images = 0;

        Files.createDirectories(targetDir);
    }

    protected void write(String fileIdentifier, boolean skip) throws Exception {
        System.out.println("writing note to:" + targetDir.toString() + "\\" + fileIdentifier + ".*");
        File file = new File(targetDir.toFile(), fileIdentifier + ".meta.txt");
        BufferedWriter bw = new BufferedWriter(new FileWriter(file));
        //
        bw.write("file:" + info);
        bw.newLine();
        bw.write("title:" + enDoc.getTitle());
        bw.newLine();
        bw.write("author:" + enDoc.getAuthor());
        bw.newLine();
        bw.write("tags:" + enDoc.getTags().toString().replace(']', ' ').replace('[', ' ').trim());
        bw.newLine();
        bw.write("source:" + enDoc.getSource());
        bw.newLine();
        bw.write("created:" + enDoc.getCreated());
        bw.newLine();
        System.out.println("file:" + info);
        bw.close();

        file = new File(targetDir.toString(), fileIdentifier + ".content.txt");
        bw = new BufferedWriter(new FileWriter(file));
        //
        bw.write(enDoc.getContent());
        bw.close();

        if (! skip || redo == REDO_ALL) {
            file = new File(targetDir.toString(), fileIdentifier + ".pdf.txt");
            bw = new BufferedWriter(new FileWriter(file));
            //
            for (String pdf : enDoc.getPdf()) {
                bw.write(pdf);
                bw.newLine();
            }
            bw.close();

            file = new File(targetDir.toString(), fileIdentifier + ".image.txt");
            bw = new BufferedWriter(new FileWriter(file));
            //
            for (String image : enDoc.getImage()) {
                bw.write(image);
                bw.newLine();
            }
            bw.close();
        }
    }


    protected void parseImages () {
        if (images > 0) {
            ImageToText.getSingleton().parseImages(enDoc, images);
        }
    }

    protected void reset() {
        enDoc = new EvernoteDoc();
        images = 0;
    }

    protected void writeImage (byte[] bytes) {
        if (ImageToText.getSingleton().writeImage(bytes, images)) {
            images++;
        }
    }

    protected void processPDF(byte[] bytes) {
        // text
        try {
            Tika tika = new Tika();
            String s = tika.parseToString(new ByteArrayInputStream(bytes));
            s = EnexTextHelper.removeWhitespace(s);
            enDoc.addPDF(s);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        // images
        images = PDFImageHelper.getSingleton().processPDF(bytes, images);
    }

    public Path getTargetDir() {
        return targetDir;
    }

    public EvernoteDoc getEnDoc() {
        return enDoc;
    }
}
