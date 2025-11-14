package com.example.evernote.internet;

import com.evernote.edam.error.EDAMErrorCode;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteMetadata;
import com.evernote.edam.notestore.NotesMetadataList;
import com.evernote.edam.notestore.NotesMetadataResultSpec;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.Resource;
import com.evernote.edam.type.Tag;
import com.example.evernote.BuildCSV;
import com.example.evernote.EMailToTags;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

public class EvernoteTagging {

    private InferenceClient inferenceClientAll;
    private InferenceClient inferenceClient4;
    private InferenceClient inferenceClient1;

    private InternetHelper internetHelper;

    private EvernoteTagging() throws Exception {
        internetHelper = new InternetHelper();
        inferenceClientAll = new InferenceClient("v7_c40");
        inferenceClient4 = new InferenceClient("v8_c40_2020");
        inferenceClient1 = new InferenceClient("v8_c40_2024");
    }

    public static void main(String[] args) {
        try {
            EvernoteTagging et = new EvernoteTagging();
            et.tagNotes();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    private String enmlFromPlain(String text) {
        String esc = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">" + "<en-note>" + esc + "</en-note>";
    }

    private boolean isNumber(String s) {
        try {
            Integer.parseInt(s.trim());
            return true;
        } catch (Exception e) {}
        return false;
    }

    public String tagNotes() throws Exception {
        try {
            String source = "mobile.iphone";
            String title = "Foto";
            String year = "0";

            NoteFilter f = new NoteFilter();
            // Name immer in Anführungszeichen, ggf. " escapen:
            String nbName = "00 Eingang";
            String q = "notebook:\"" + nbName.replace("\"", "\\\"") + "\"";
            f.setWords(q);

            NotesMetadataResultSpec spec = new NotesMetadataResultSpec();
            spec.setIncludeTitle(true);
            spec.setIncludeUpdateSequenceNum(true);
            spec.setIncludeAttributes(true);

            NotesMetadataList meta = RemoteNoteStore.getSingleton().getNoteStore().findNotesMetadata(f, 0, 100, spec);
            // → liefert nur Notizen aus genau diesem Notizbuch (per Name gefiltert)

            StringBuilder sb = new StringBuilder("Letzte 100 Notizen:\n\n");
            List<NoteMetadata> l = meta.getNotes();
            for (NoteMetadata nm : l) {
                try {
                    System.out.println("attributes:" + nm.getAttributes().getAuthor() + " " + nm.getAttributes().toString());
                    sb.append(nm.getTitle());

                    Note note = LocalNoteStore.getSingleton().load(nm.getGuid(), nm.getUpdateSequenceNum());
                    if (note == null) {
                        note = RemoteNoteStore.getSingleton().getNoteStore().getNote(nm.getGuid(), true, true, true, true);
                        LocalNoteStore.getSingleton().save(note);
                        System.out.println("note from internet:" + note.getTitle());
                    } else {
                        System.out.println("note from store:" + note.getTitle());
                    }

                    // tagging from machine learning
                    boolean tagsFromPredictionsUpdated = setTagsFromPredictions (note);

                    // title for paper documents
                    boolean titleUpdated = setTitleForPaperDocuments(source, title, year, nm, note);

                    // E-Mails
                    boolean tagsFromEMailsUpdated = setTagsFromEMails(nm, note);

                    if (tagsFromPredictionsUpdated || titleUpdated || tagsFromEMailsUpdated) {
                        System.out.println("update:" + note.getTitle());
                        RemoteNoteStore.getSingleton().getNoteStore().updateNote(note);
                    }
                } catch (Exception e) {e.printStackTrace(System.out);}
                sb.append("\n");
                //meta.getNotes().forEach(n -> sb.append(n.getTitle()).append(" | GUID=").append(n.getGuid()).append('\n'));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            if (e instanceof EDAMUserException) {
                EDAMUserException eue = (EDAMUserException) e;
                if (eue.getErrorCode() == EDAMErrorCode.AUTH_EXPIRED) {
                    LocalTokenStore.getSingleton().clear();
                    return "Auth expired. Token wurde gelöscht. Bitte neu autorisieren: /evernote/oauth/start";
                }
            }
            throw e;
        }
    }

    private boolean setTagsFromPredictions(Note note) {
        boolean noteUpdated = false;
        String predictedTagsAll = null;
        String predictedTags4 = null;
        String predictedTags1 = null;
        try {
            internetHelper.parseAndWrite(note);

            ZoneId zone = ZoneId.of("Europe/Berlin");
            Instant instant = Instant.ofEpochMilli(note.getCreated());
            ZonedDateTime zdt = instant.atZone(zone);

            File csv = BuildCSV.writeSingleLine(internetHelper.getTargetDir().toFile(), note.getGuid(), String.valueOf(zdt.getYear()));
            System.out.println("csvLine:" + csv.getAbsolutePath());
            predictedTagsAll = inferenceClientAll.infer(csv);
            predictedTags4 = inferenceClient4.infer(csv);
            predictedTags1 = inferenceClient1.infer(csv);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        if (predictedTagsAll != null) {

            Set<String> allPredictedTags = new HashSet<>();
            allPredictedTags.addAll(Arrays.asList(predictedTagsAll.split(",")));
            allPredictedTags.addAll(Arrays.asList(predictedTags4.split(",")));
            allPredictedTags.addAll(Arrays.asList(predictedTags1.split(",")));

            Set<String> selectedPredictedTagsGuids = new HashSet<>();
            for (String t : allPredictedTags) {
                t = t.trim();
                if (isNumber(t)) continue;
                if (t.equals("done")) continue;
                for (Tag tag : RemoteNoteStore.getSingleton().getTags()) {
                    if (tag.getName().equals(t)) {
                        selectedPredictedTagsGuids.add(tag.getGuid());
                        break;
                    }
                }
            }

            for (String guid : selectedPredictedTagsGuids) {
                if (note.getTagGuids() == null || !note.getTagGuids().contains(guid)) {
                    note.addToTagGuids(guid);
                    noteUpdated = true;
                }
            }
        }
        return noteUpdated;
    }

    private boolean setTagsFromEMails(NoteMetadata nm, Note note) {
        boolean noteUpdated = false;
        if (
                nm.getAttributes() != null &&
                        nm.getAttributes().getSource() != null &&
                        nm.getAttributes().getSource().equals("mail.smtp") &&
                        nm.getAttributes().getAuthor() != null
        ) {
            System.out.println("tags for " + nm.getTitle());
            List<String> missingGuids = new ArrayList<>();
            for (String key : EMailToTags.getSingleton().getEMails()) {
                if (nm.getAttributes().getAuthor().endsWith(key)) {
                    for (String tagname : EMailToTags.getSingleton().getTags(key)) {
                        String guid = EMailToTags.getSingleton().getGUID(tagname);
                        if (nm.getTagGuids() == null || !nm.getTagGuids().contains(guid)) {
                            System.out.println("1 adding tags for " + nm.getTitle() + ":" + tagname);
                            missingGuids.add(guid);
                        }
                    }
                }
            }
            if (missingGuids.size() > 0) {
                for (String guid : missingGuids) {
                    note.addToTagGuids(guid);
                }
                noteUpdated = true;
            }
        }
        return noteUpdated;
    }

    private boolean setTitleForPaperDocuments(String source, String title, String year, NoteMetadata nm, Note note) {
        boolean noteUpdated = false;
        System.out.println("attributes:" + nm.getAttributes());
        System.out.println(source + ":source:" + (nm.getAttributes() == null ? "null" : nm.getAttributes().getSource()));
        System.out.println(title + ":title:" + nm.getTitle());
        if (
                nm.getAttributes() != null &&
                (nm.getAttributes().getSource() == null || source.equals(nm.getAttributes().getSource())) &&
                (nm.getTitle() == null || nm.getTitle().equals(title) || nm.getTitle().equals(""))) {
            System.out.println("----------------- reco -------------------");
            try {
                int rn = 0;
                if (
                        note.getResources() != null &&
                        note.getResources().size() > 0
                ) {

                    //for (Resource r : note.getResources())
                    Resource r = note.getResources().get(0);
                    {
                        //System.out.println("note.resource.type" + r.getMime() + ":" + note.getContentLength());

                        rn++;
                        BufferedImage top = null;
                        try {
                            BufferedImage img = ImageIO.read(new ByteArrayInputStream(r.getData().getBody()));
                            if (img != null && img.getWidth() > 1000 && img.getHeight() > 1000) {
                                top = img.getSubimage(img.getWidth() / 3 * 2, 0, img.getWidth() / 3, (img.getHeight() / 30));
                            }

                        } catch (Exception e) {
                        }
                        if (top != null) {
                            // this is used
                            try {
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                ImageIO.write(top, "jpg", baos);          // Format wählen
                                String json = TrOcrClient.getSingleton().ocr(baos.toByteArray());
                                System.out.println("trocr:" + json);
                                ObjectMapper om = new ObjectMapper();
                                String text = om.readTree(json).path("text").asText();
                                text = text.replaceAll("\\s+", "");
                                text = text.replace(',', '.');
                                if (text.startsWith(year + ".")) {
                                    while (text.endsWith(".")) {
                                        text = text.substring(0, text.length() - 1);
                                    }
                                    if (Character.isDigit(text.charAt(text.length() - 1)) || text.charAt(text.length() - 1) == ')') {
                                        if (text.length() > 8) {
                                            // change title
                                            note.setTitle(text);
                                            noteUpdated = true;
                                        }
                                    }
                                }
                                System.out.println("text:" + text);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return noteUpdated;
    }


}