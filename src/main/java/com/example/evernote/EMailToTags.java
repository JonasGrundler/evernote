package com.example.evernote;

import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.type.Tag;
import com.evernote.thrift.TException;
import com.example.evernote.internet.RemoteNoteStore;

import java.util.*;

public class EMailToTags {

    private Map<String, List<String>> map = new HashMap<>();
    private Set<String> tags = new HashSet<>();
    private Map<String, String> tagToGuid = new HashMap<>();
    private Map<String, String> guidToTag = new HashMap<>();

    private static final EMailToTags singleton;

    static {
        EMailToTags ls = null;
        try {
            ls = new EMailToTags();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        singleton = ls;
    }

    public static EMailToTags getSingleton() {
        return singleton;
    }

    private EMailToTags () throws TException, EDAMSystemException, EDAMUserException {
        addInfo("@merz-schule.de", "Merz-Schule");
        addInfo("felbel.hausverwaltung@gmail.com", "HV Felbel Hausverwaltung");
        addInfo("@email.etg24.de", "etg24");
        addInfo("@paypal.de", "PayPal");
        addInfo("@cloudHQ.net", "cloudHQ");
        addInfo("@advanzia.com", "Advanzia Bank");
        addInfo("@mini-charging.com", "BMW", "Jun");
        addInfo("@apcoaflow.com", "APCOA FLOW");
        addInfo("@waldschule-degerloch.de", "Waldschule Degerloch");
        addInfo("@mailing.milesandmore.com", "Miles & More", "Lufthansa");
        addInfo("maccount@microsoft.com", "verdimo");
        addInfo("nicht.antworten@kundenservice.vodafone.com", "Vodafone");
        // hint: verdimo gleich in Verdimo Posteingang verschieben

        init();
    }

    private void init() throws TException, EDAMSystemException, EDAMUserException {
        // prepare tags for emails
        List<Tag> tags = RemoteNoteStore.getSingleton().getTags();
        // validate...
        System.out.println("mapping");
        for (String eTag : getAllTags()) {
            boolean found = false;
            for (Tag tag : tags) {
                if (tag.getName().equals(eTag)) {
                    found = true;
                    setGUID(tag.getName(), tag.getGuid());
                    continue;
                }
            }
            if (! found) {
                System.out.println("eTag " + eTag + " not found!");
            }
        }

    }

    private void addInfo (String eMail, String... tags) {
        add(eMail, tags);
    }

    private void add (String eMail, String[] eTags) {
        List<String> l = new ArrayList<>();
        map.put(eMail, l);
        for (String tag : eTags) {
            l.add(tag);
            tags.add(tag);
        }
    }

    public List<String> getTags (String text) {
        List<String> tags = new ArrayList<>();
        for (String key : map.keySet()) {
            if (text.endsWith(key)) {
                tags.addAll(map.get(key));
            }
        }
        return tags;
    }

    public String getGUID(String tag) {
        return tagToGuid.get(tag);
    }

    public String getTag(String guid) {
        return guidToTag.get(guid);
    }

    private void setGUID(String tag, String guid) {
        tagToGuid.put(tag, guid);
        guidToTag.put(guid, tag);
    }

    public Set<String> getAllTags() {
        return tags;
    }

    public Set<String> getEMails() {
        return map.keySet();
    }

    public void removeTag(String tag) {
        tags.remove(tag);
        for (List<String> l : map.values()) {
            l.remove(tag);
        }
        tags.remove(tag);
        String guid = tagToGuid.remove(tag);
        if (guid != null) {
            guidToTag.remove(guid);
        }
    }

}
