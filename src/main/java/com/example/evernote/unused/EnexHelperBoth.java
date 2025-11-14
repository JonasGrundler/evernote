package com.example.evernote.unused;


public class EnexHelperBoth {
/*
    private XMLInputFactory f;
    private XMLStreamReader r;
    private File workingDir;
    private File dir;
    private Path enexFile;
    private int noteNr;

    private ImageToText imageExtractor;
    private PDFImageHelper pdfImageExtractor;

    private int redo;
    private static final int REDO_NOT = 0;
    private static final int REDO_ALL = 1;
    private static final int REDO_TEXT = 2;

    public EnexHelperBoth(Path path, int redo) throws Exception {
        this (path.getParent().toFile(), new File(path.toFile().getAbsolutePath() + "_txt"));

        this.enexFile = path;
        this.redo = redo;
        f = XMLInputFactory.newFactory();
        f.setProperty(XMLInputFactory.IS_COALESCING, true);
        // Sicherheit / XXE hardening
        f.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        try { f.setProperty("javax.xml.stream.isSupportingExternalEntities", false); } catch (Exception ignore) {}
        // Optional: Woodstox liefert bessere Performance, einfach als Dependency hinzufügen.

        InputStream in = Files.newInputStream(path);
        r = f.createXMLStreamReader(in);

        while (r.next() != XMLStreamConstants.START_ELEMENT);
    }

    public EnexHelperBoth() throws Exception {
        this (new LocalStore("tmp").getSubDir().toFile());
    }

    public EnexHelperBoth(File baseDir) throws Exception {
        this (baseDir, baseDir);
    }

    public EnexHelperBoth(File baseDir, File targetDir) throws Exception {

        this.noteNr = 0;
        this.redo = REDO_ALL;


        this.workingDir = new File(baseDir, "tmp");
        if (! workingDir.exists()) {
            workingDir.mkdirs();
        }
        this.dir = targetDir;
        if (! dir.exists()) {
            dir.mkdirs();
        }

        imageExtractor = new ImageToText(workingDir, dir);
        pdfImageExtractor = new PDFImageHelper(workingDir);

    }

    private EvernoteDoc parse(Note note) throws Exception {
        EvernoteDoc enDoc = new EvernoteDoc();

        enDoc.setTitle(note.getTitle());
        enDoc.setCreated(EnexTextHelper.df.format(Instant.ofEpochMilli(note.getCreated())));
        enDoc.setAuthor(note.getAttributes().getAuthor());
        enDoc.setContent(EnexTextHelper.enmlToText(note.getContent()));
        enDoc.setSource(note.getAttributes().getSource());
        int images = 0;
        if (note.getResources() != null) {
            for (Resource r : note.getResources()) {
                if ("application/pdf".equals(r.getMime())) {
                    images = processPDF(r.getData().getBody(), enDoc, images);
                } else
                if (r.getMime().startsWith("image/")) {
                    if (imageExtractor.writeImage(r.getData().getBody(), images)) {
                        images++;
                    }
                }
            }
        }
        imageExtractor.parseImages(enDoc, images);
        return enDoc;
    }

    public int parseAndWrite(Note note) throws Exception {
        EvernoteDoc enDoc = parse(note);
        write(enDoc, false);
        noteNr++;
        return noteNr - 1;
    }

    public EvernoteDoc next() throws XMLStreamException {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>> <note." + noteNr + "> <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        EvernoteDoc ed = null;
        try {
            File done = new File(dir, noteNr + ".done");
            boolean skip = done.exists();
            System.out.println("skip:" + skip);
            while (r.hasNext() && r.next() != XMLStreamConstants.START_ELEMENT);
            if (r.getEventType() == XMLStreamConstants.START_ELEMENT && "note".equals(r.getLocalName())) {
                ed = readEvernoteDoc(r, skip);
                if (! skip || redo != REDO_NOT) {
                    write(ed, skip);
                }
                if (! skip) {
                    done.createNewFile();
                }
            }
        } catch (Exception e) {e.printStackTrace(System.out);}
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>> </note" + noteNr + "> <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        noteNr++;
        return ed;
    }

    public void close() throws XMLStreamException {
        r.close();
    }

    // Liest <record>...</record> inkl. seiner Kinder, r steht beim START_ELEMENT <record>
    private EvernoteDoc readEvernoteDoc(XMLStreamReader r, boolean skip) throws XMLStreamException {

        EvernoteDoc enDoc = new EvernoteDoc();

        if (skip && redo == REDO_NOT) {
            while (!((r.next() == XMLStreamConstants.END_ELEMENT) && "note".equals(r.getLocalName())));
        } else {
            int images = 0;

            while (!((r.next() == XMLStreamConstants.END_ELEMENT) && "note".equals(r.getLocalName()))) {

                if (r.getEventType() == XMLStreamConstants.START_ELEMENT) {

                    if ("title".equals(r.getLocalName())) {
                        enDoc.setTitle(text(r));
                    }
                    else if ("tag".equals(r.getLocalName())) {
                        enDoc.addTag(text(r));
                    }
                    else if ("created".equals(r.getLocalName())) {
                        enDoc.setCreated(text(r));
                    }
                    else if ("note-attributes".equals(r.getLocalName())) {
                        while (!(r.getEventType() == XMLStreamConstants.END_ELEMENT && "note-attributes".equals(r.getLocalName()))) {
                            r.next();
                            if (r.getEventType() == XMLStreamConstants.START_ELEMENT) {
                                if ("author".equals(r.getLocalName())) {
                                    enDoc.setAuthor(text(r));
                                } else
                                if ("source".equals(r.getLocalName())) {
                                    enDoc.setSource(text(r));
                                }
                            }
                        }
                    }
                    else if ("content".equals(r.getLocalName())) {
                        String string = chars(r);
                        try {
                            enDoc.setContent(EnexTextHelper.enmlToText(string));
                        } catch (Exception e) {
                            e.printStackTrace(System.out);
                        }
                    }
                    else if ((! skip || redo == REDO_ALL) && "resource".equals(r.getLocalName())) {
                        String data = null;
                        while (!(r.getEventType() == XMLStreamConstants.END_ELEMENT && "resource".equals(r.getLocalName()))) {
                            r.next();
                            if (r.getEventType() == XMLStreamConstants.START_ELEMENT) {
                                if ("data".equals(r.getLocalName())) {
                                    data = chars(r);
                                } else
                                if ("mime".equals(r.getLocalName())) {
                                    String type = text(r);
                                    if ("application/pdf".equals(type)) {
                                        images = processPDF(Base64.getMimeDecoder().decode(data), enDoc, images);
                                    } else
                                    if (type.startsWith("image/")) {
                                        if (imageExtractor.writeImage(Base64.getMimeDecoder().decode(data), images)) {
                                            images++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            imageExtractor.parseImages(enDoc, images);
        }
        return enDoc;
    }

    private int processPDF(byte[] bytes, EvernoteDoc enDoc, int images) {
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
        images += pdfImageExtractor.processPDF(bytes);
        return images;
    }

    private String chars(XMLStreamReader r) throws XMLStreamException {
        StringBuilder sb = new StringBuilder();
        r.next();
        while (! (r.getEventType() == XMLStreamConstants.END_ELEMENT)) {
            String string = r.getText();
            sb.append(string);
            r.next();
        }
        return sb.toString();
    }

    // Liest den reinen Textinhalt eines einfachen Elements <x>TEXT</x>
    private String text(XMLStreamReader r) throws XMLStreamException {
        StringBuilder sb = new StringBuilder();
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.CHARACTERS || ev == XMLStreamConstants.CDATA)
            {
                sb.append(r.getText());
            } else if (ev == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        return sb.toString().trim();
    }


    public File getDir() {
        return dir;
    }

 */
}
