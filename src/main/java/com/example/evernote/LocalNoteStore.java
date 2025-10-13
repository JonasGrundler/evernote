package com.example.evernote;

import com.evernote.edam.type.Note;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class LocalNoteStore {

    public LocalNoteStore() {
        String userHome = System.getProperty("user.home");
    }
    private Path getNotePath(String guid, int updateSequenceNum) {
        String userHome = System.getProperty("user.home");
        Path p = Paths.get(userHome, ".note-store", "note_" + guid + "_" + updateSequenceNum + ".obj");
        return p;
    }
    public void save(Note note) throws IOException {
        System.out.println("note " + note.getGuid() + "_" + note.getUpdateSequenceNum() + " saved in local store.");
        Path notePath = getNotePath(note.getGuid(), note.getUpdateSequenceNum());
        Files.createDirectories(notePath.getParent());
        ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(notePath.toFile())));
        oos.writeObject(note);
        oos.close();
    }
    public Note load(String guid, int updateSequenceNum) {
        Path notePath = getNotePath(guid, updateSequenceNum);
        try {
            if (Files.exists(notePath)) {
                ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(notePath.toFile())));
                Note note = (Note) ois.readObject();
                ois.close();
                return note;
            } else {
                System.out.println("note " + guid + "_" + updateSequenceNum + " not found in local store.");
            }
        } catch (Exception ignored) {ignored.printStackTrace();}
        return null;
    }

    public void writeJpg (BufferedImage img, int rn, String guid, int updateSequenceNum) throws Exception {
        String userHome = System.getProperty("user.home");
        Path path = Paths.get(userHome, ".note-store", "note_" + guid + "_" + updateSequenceNum + "_" + rn + "_" + "img.jpg");
        ImageIO.write(img,"jpg",path.toFile());
    }

}
