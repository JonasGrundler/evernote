package com.example.evernote;

import java.util.ArrayList;
import java.util.List;

public class EvernoteDoc {

    private String filename;
    private String title;
    private final List<String> tags = new ArrayList<>();
    private String author;
    private String source;
    private String content;
    private String created;
    private final List<String> pdf = new ArrayList<>();
    private final List<String> image = new ArrayList<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void addTag(String tag) {
        tags.add(tag);
    }

    public List<String> getTags() {
        return tags;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void addPDF(String text) {
        pdf.add(text);
    }

    public void addImage(String text) {
        image.add(text);
    }

    public List<String> getPdf() {
        return pdf;
    }

    public List<String> getImage() {
        return image;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}
