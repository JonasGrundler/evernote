package com.example.evernote;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.JSoupParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class EnexTextHelper {

    public static DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss").withZone(ZoneId.of("Europe/Berlin"));

    private EnexTextHelper () {

    }

    public static String enmlToText(String enml) throws Exception {
        enml = enml.replaceFirst("(?is)^\\s*<\\?xml[^>]*>\\s*", "")
                .replaceFirst("(?is)<!DOCTYPE[^>]*>\\s*", "");
        ContentHandler handler = new BodyContentHandler(-1);
        new JSoupParser().parse(
                new ByteArrayInputStream(enml.getBytes(StandardCharsets.UTF_8)),
                handler,
                new Metadata(),
                new ParseContext()
        );
        String s = handler.toString().replaceAll("\\n{3,}", "\n\n").trim();

        return removeWhitespace(s);
    }

    public static String removeWhitespace(String s) {
        // 1) Mehrere leere Zeilen (auch mit Spaces/Tabs) entfernen
        s = s.replaceAll("(?m)^[\\p{Zs}\\t]*\\R+", "");

        // 2) Laufende horizontale Whitespaces (Spaces/Tabs/NBSP) → ein einzelnes Space
        s = s.replaceAll("[\\p{Zs}\\t]+", " ");

        // Optional sauber: Spaces direkt vor/ nach Zeilenumbrüchen entfernen
        s = s.replaceAll("[ \\t]+\\R", "\n")
                .replaceAll("\\R[ \\t]+", "\n");

        return s.trim();
    }
}
