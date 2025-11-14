package com.example.evernote.enex;

import com.example.evernote.LocalStore;
import com.example.evernote.EnexTextHelper;
import com.example.evernote.EnDocHelper;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;


public class EnexFileToEnDocs extends EnDocHelper {

    private int noteNr;
    private XMLInputFactory f;
    private XMLStreamReader r;
    private int redo;


    public EnexFileToEnDocs(Path enexFile, int redo) throws Exception {
        super (Paths.get(LocalStore.getSingleton().getEnex_batch_parsed().toString(), enexFile.getFileName() + "_txt"), redo, enexFile.getFileName().toString());

        this.redo = redo;

        f = XMLInputFactory.newFactory();
        f.setProperty(XMLInputFactory.IS_COALESCING, true);
        // Sicherheit / XXE hardening
        f.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        try { f.setProperty("javax.xml.stream.isSupportingExternalEntities", false); } catch (Exception ignore) {}
        // Optional: Woodstox liefert bessere Performance, einfach als Dependency hinzufügen.

        InputStream in = Files.newInputStream(enexFile);
        r = f.createXMLStreamReader(in);

        while (r.next() != XMLStreamConstants.START_ELEMENT);

        this.noteNr = 0;
    }

    public boolean next() throws XMLStreamException {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>> <note." + noteNr + "> <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

        try {
            File done = new File(getTargetDir().toFile(), noteNr + ".done");
            boolean skip = done.exists();
            System.out.println("skip:" + skip);
            while (r.hasNext() && r.next() != XMLStreamConstants.START_ELEMENT);
            if (r.getEventType() == XMLStreamConstants.START_ELEMENT && "note".equals(r.getLocalName())) {
                readEvernoteDoc(r, skip);
                if (! skip || redo != REDO_NOT) {
                    write(String.valueOf(noteNr), skip);
                }
                if (! skip) {
                    done.createNewFile();
                }
                noteNr++;
                return true;
            }
        } catch (Exception e) {e.printStackTrace(System.out);}
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>> </note" + noteNr + "> <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        return false;
    }

    public void close() throws XMLStreamException {
        r.close();
    }

    // Liest <record>...</record> inkl. seiner Kinder, r steht beim START_ELEMENT <record>
    private void readEvernoteDoc(XMLStreamReader r, boolean skip) throws XMLStreamException {

        reset();

        if (skip && redo == REDO_NOT) {
            while (!((r.next() == XMLStreamConstants.END_ELEMENT) && "note".equals(r.getLocalName())));
        } else {
            while (!((r.next() == XMLStreamConstants.END_ELEMENT) && "note".equals(r.getLocalName()))) {

                if (r.getEventType() == XMLStreamConstants.START_ELEMENT) {

                    if ("title".equals(r.getLocalName())) {
                        getEnDoc().setTitle(text(r));
                    }
                    else if ("tag".equals(r.getLocalName())) {
                        getEnDoc().addTag(text(r));
                    }
                    else if ("created".equals(r.getLocalName())) {
                        getEnDoc().setCreated(text(r));
                    }
                    else if ("note-attributes".equals(r.getLocalName())) {
                        while (!(r.getEventType() == XMLStreamConstants.END_ELEMENT && "note-attributes".equals(r.getLocalName()))) {
                            r.next();
                            if (r.getEventType() == XMLStreamConstants.START_ELEMENT) {
                                if ("author".equals(r.getLocalName())) {
                                    getEnDoc().setAuthor(text(r));
                                } else
                                if ("source".equals(r.getLocalName())) {
                                    getEnDoc().setSource(text(r));
                                }
                            }
                        }
                    }
                    else if ("content".equals(r.getLocalName())) {
                        String string = chars(r);
                        try {
                            getEnDoc().setContent(EnexTextHelper.enmlToText(string));
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
                                        processPDF(Base64.getMimeDecoder().decode(data));
                                    } else
                                    if (type.startsWith("image/")) {
                                        writeImage(Base64.getMimeDecoder().decode(data));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        parseImages();
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

}
