package com.example.evernote.internet;

import com.evernote.auth.EvernoteAuth;
import com.evernote.clients.ClientFactory;
import com.evernote.clients.NoteStoreClient;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.type.Tag;
import com.evernote.thrift.TException;
import com.example.evernote.EMailToTags;

import java.util.List;

public class RemoteNoteStore {

    private NoteStoreClient noteStore;

    private List<Tag> tags;

    private static final RemoteNoteStore singleton;

    static {
        RemoteNoteStore ls = null;
        try {
            ls = new RemoteNoteStore();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        singleton = ls;
    }

    public static RemoteNoteStore getSingleton() {
        return singleton;
    }

    private RemoteNoteStore() throws TException, EDAMSystemException, EDAMUserException {
        EvernoteAuth userAuth = new EvernoteAuth(com.evernote.auth.EvernoteService.PRODUCTION, LocalTokenStore.getSingleton().load());
        ClientFactory factory = new ClientFactory(userAuth);
        NoteStoreClient noteStore = factory.createNoteStoreClient();
        tags = noteStore.listTags();
    }

    public List<Tag> getTags() {
        return tags;
    }

    public NoteStoreClient getNoteStore() {
        return noteStore;
    }
}
