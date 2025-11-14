package com.example.evernote.internet;

import com.evernote.edam.type.Note;
import com.evernote.edam.type.Resource;
import com.example.evernote.EnDocHelper;
import com.example.evernote.EnexTextHelper;
import com.example.evernote.LocalStore;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;


/**
 * notes und Zusatzdaten werden unter der notes guid abgespeichert
 */
public class InternetHelper extends EnDocHelper {

    public InternetHelper() throws Exception {
        super (LocalStore.getSingleton().getInternet_single_notes(), REDO_TEXT, "direct");
    }

    public void parseAndWrite(Note note) throws Exception {

        super.reset();

        getEnDoc().setTitle(note.getTitle());
        getEnDoc().setCreated(EnexTextHelper.df.format(Instant.ofEpochMilli(note.getCreated())));
        getEnDoc().setAuthor(note.getAttributes().getAuthor());
        getEnDoc().setContent(EnexTextHelper.enmlToText(note.getContent()));
        getEnDoc().setSource(note.getAttributes().getSource());

        if (note.getResources() != null) {
            for (Resource r : note.getResources()) {
                if ("application/pdf".equals(r.getMime())) {
                    processPDF(r.getData().getBody());
                } else
                if (r.getMime().startsWith("image/")) {
                    writeImage(r.getData().getBody());
                }
            }
        }
        Path done = Paths.get(getTargetDir().toString(), note.getGuid() + ".done");
        boolean skip = done.toFile().exists();
        if (! skip) {
            parseImages();
        }
        write(note.getGuid(), skip);
    }

}
