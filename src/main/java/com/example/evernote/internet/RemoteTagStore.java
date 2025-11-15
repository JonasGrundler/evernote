package com.example.evernote.internet;

import com.evernote.auth.EvernoteAuth;
import com.evernote.clients.ClientFactory;
import com.evernote.clients.NoteStoreClient;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.type.Tag;
import com.evernote.thrift.TException;
import com.example.evernote.LocalStore;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class RemoteTagStore implements Serializable {

    private List<Tag> tags;

    private long lastUpdate = -1;
    private static final long UPDATE_TIMEOUT = 12 * 60 * 60 * 1000;

    private static final Path serializedFile = Paths.get(LocalStore.getSingleton().getMappings().toString(), "RemoteTagStore.ser");

    private static final RemoteTagStore singleton;

    static {
        RemoteTagStore ls = null;
        try {
            if (serializedFile.toFile().exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(serializedFile.toFile()));
                ls = (RemoteTagStore) ois.readObject();
                ois.close();
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            try {
                serializedFile.toFile().delete();
            } catch (Exception e2) {
                e2.printStackTrace(System.out);
            }
        }
        if (ls == null) {
            ls = new RemoteTagStore();
        }
        singleton = ls;
    }

    public static RemoteTagStore getSingleton() {
        return singleton;
    }

    private RemoteTagStore() {
    }

    public void init() {
        if (System.currentTimeMillis() - lastUpdate > UPDATE_TIMEOUT) {
            try {
                tags = RemoteNoteStore.getSingleton().getNoteStore().listTags();
                lastUpdate = System.currentTimeMillis();
                try {
                    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(serializedFile.toFile()));
                    oos.writeObject(this);
                    oos.close();
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }
    }

    public List<Tag> getTags() {
        init();
        return tags;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }
}
