package com.example.evernote;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LocalStore {

    private final Path base;

    private final Path enex_batch;
    private final Path enex_batch_csv;
    private final Path enex_batch_exports;
    private final Path enex_batch_parsed;

    private final Path internet_single;
    private final Path internet_single_notes;
    private final Path internet_single_oauth;

    private final Path images_tmp;

    private final Path mappings;

    private static final LocalStore singleton;

    static {
        LocalStore ls = null;
        try {
            ls = new LocalStore();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        singleton = ls;
    }

    private LocalStore() throws IOException {

        //test
        //base = create("C:\\Users\\Jonas\\Downloads\\0", ".jg-evernote");

        base = create(Paths.get(System.getProperty("user.home")).toString(), ".jg-evernote");

        enex_batch = create(base.toString(), "enex-batch");
        enex_batch_csv = create(enex_batch.toString(), "csv");
        enex_batch_exports = create(enex_batch.toString(), "exports");
        enex_batch_parsed = create(enex_batch.toString(), "parsed");

        internet_single = create(base.toString(), "internet-single");
        internet_single_notes = create(internet_single.toString(), "notes");
        internet_single_oauth = create(internet_single.toString(), "oauth");

        images_tmp = create(base.toString(), "images-tmp");

        mappings = create(base.toString(), "mappings");
    }

    private Path create(String b, String s) throws IOException {
        Path path = Paths.get(b, s);
        return Files.createDirectories(path);
    }

    public static LocalStore getSingleton() {
        return singleton;
    }

    public Path getEnex_batch_csv() {
        return enex_batch_csv;
    }

    public Path getEnex_batch_exports() {
        return enex_batch_exports;
    }

    public Path getEnex_batch_parsed() {
        return enex_batch_parsed;
    }

    public Path getInternet_single_notes() {
        return internet_single_notes;
    }

    public Path getInternet_single_oauth() {
        return internet_single_oauth;
    }

    public Path getImages_tmp() { return images_tmp; }

    public Path getMappings() { return mappings; }
}
