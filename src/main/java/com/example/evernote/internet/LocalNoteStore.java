package com.example.evernote.internet;

import com.evernote.edam.type.Note;
import com.example.evernote.LocalStore;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LocalNoteStore {

    private static final LocalNoteStore singleton;

    static {
        LocalNoteStore ls = null;
        try {
            ls = new LocalNoteStore();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        singleton = ls;
    }

    public static LocalNoteStore getSingleton() {
        return singleton;
    }

    private LocalNoteStore() {
    }

    private Path getNotePath(String guid, int updateSequenceNum) {
        Path p = Paths.get(LocalStore.getSingleton().getInternet_single_notes().toString(), "note_" + guid + "_" + updateSequenceNum + ".obj");
        return p;
    }
    public void save(Note note) throws IOException {
        Path notePath = getNotePath(note.getGuid(), note.getUpdateSequenceNum());
        Files.createDirectories(notePath.getParent());
        ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(notePath.toFile())));
        oos.writeObject(note);
        oos.close();
    }
    public Note load(String guid, int updateSequenceNum) {
        Path notePath = getNotePath(guid, updateSequenceNum);
        System.out.println("note from store:" + notePath.toString());
        try {
            if (Files.exists(notePath)) {
                ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(notePath.toFile())));
                Note note = (Note) ois.readObject();
                ois.close();
                return note;
            } else {
                System.out.println("note " + guid + "_" + updateSequenceNum + " not found in local store.");
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return null;
    }

    public boolean isTagged (String guid) {
        Path p = Paths.get(LocalStore.getSingleton().getInternet_single_notes().toString(), "note_" + guid + ".done");
        return p.toFile().exists();
    }

    public void tag (String guid) throws IOException {
        Path p = Paths.get(LocalStore.getSingleton().getInternet_single_notes().toString(), "note_" + guid + ".done");
        if (! p.toFile().exists()) {
            p.toFile().createNewFile();
        }
    }

    public void unTag (String guid) throws IOException {
        Path p = Paths.get(LocalStore.getSingleton().getInternet_single_notes().toString(), "note_" + guid + ".done");
        if (p.toFile().exists()) {
            p.toFile().delete();
        }
    }
}
