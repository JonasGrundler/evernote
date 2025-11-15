package com.example.evernote.internet;

import com.evernote.edam.type.Tag;
import com.example.evernote.LocalStore;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class EMailToTags implements Serializable {

    private Map<String, List<String>> map = new HashMap<>();
    private Set<String> tags = new HashSet<>();
    private Map<String, String> tagToGuid = new HashMap<>();
    private Map<String, String> guidToTag = new HashMap<>();

    private long lastUpdate = -1;
    private static final long UPDATE_TIMEOUT = 12 * 60 * 60 * 1000;

    private static final Path emailToTagFile = Paths.get(LocalStore.getSingleton().getMappings().toString(), "email-to-tags.txt");
    private static final Path serializedFile = Paths.get(LocalStore.getSingleton().getMappings().toString(), "EMailToTags.ser");

    private static final EMailToTags singleton;

    static {
        EMailToTags ls = null;
        try {
            if (serializedFile.toFile().exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(serializedFile.toFile()));
                ls = (EMailToTags) ois.readObject();
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
            ls = new EMailToTags();
        }
        singleton = ls;
    }

    public static EMailToTags getSingleton() {
        return singleton;
    }

    private EMailToTags () {
    }

    public void init() {
        if (
                System.currentTimeMillis() - lastUpdate > UPDATE_TIMEOUT ||
                emailToTagFile.toFile().lastModified() > lastUpdate ||
                RemoteTagStore.getSingleton().getLastUpdate() > lastUpdate
        ) {
            try {
                //
                map.clear();
                tags.clear();
                tagToGuid.clear();
                guidToTag.clear();

                //
                if (emailToTagFile.toFile().exists()) {
                    String line;
                    BufferedReader br = new BufferedReader(new FileReader(emailToTagFile.toFile()));
                    while ((line = br.readLine()) != null) {
                        String[] s = line.split(":");
                        addInfo(s[0], s[1].split(","));
                    }
                    br.close();
                }

                // prepare tags for emails
                List<Tag> tags = RemoteTagStore.getSingleton().getTags();
                // validate...
                System.out.println("mapping");
                for (String eTag : getAllTags()) {
                    boolean found = false;
                    for (Tag tag : tags) {
                        //System.out.println("name:" + tag.getName() + ", " + tag.toString());
                        if (tag.getName().equals(eTag)) {
                            found = true;
                            setGUID(tag.getName(), tag.getGuid());
                            continue;
                        }
                    }
                    if (!found) {
                        System.out.println("eTag " + eTag + " not found!");
                    }
                }
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

    private void addInfo (String eMail, String... tags) {
        add(eMail, tags);
    }

    private void add (String eMail, String[] eTags) {
        List<String> l = new ArrayList<>();
        map.put(eMail, l);
        for (String tag : eTags) {
            String t = tag.trim();
            l.add(t);
            tags.add(t);
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

}
