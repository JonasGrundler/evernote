package com.example.evernote;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BuildCSV {

    private static final String[] GSW = new String[]{
        "der","die","das","dass","ein","eine","einer","einem","einen","eines",
        "den","dem","des","im","in","am","an","auf","aus","bei","vom","von","fuer",
        "mit","ohne","um","ueber","unter","bis","durch","gegen","nach","zur","zum",
        "und","oder","aber","sondern","denn","als","wie","auch","so","nur",
        "ist","sind","war","waren","wird","werden","wurde","wurden","sei","seien",
        "hat","haben","hast","hatte","hatten","haette","haetten",
        "ich","du","er","sie","es","wir","ihr","ihnen","ihn","ihm","mich","dich","uns","euch",
        "man","sich","mein","dein","sein","ihr","unser","euer",
        "dies","diese","dieser","dieses","diesem","diesen","jene","jener","jenes",
        "schon","noch","sehr","mehr","weniger","kein","keine","keinen","keinem","keines",
        "weil","wenn","falls","waehrend","beim","bis","seit","sobald","solange",
        "hier","dort","da","dann","darum","deshalb","wo","wohin","woher",
        "etwa","zwar","eben","fast","bisher","bereits","gerade","wohl","spaeter",
        "heute","gestern","morgen","damit","dabei","dazu","darueber","daran","davon",
        "ihre", "deine", "vielen", "dank", "von", "vom", "ab", "bis", "bitte", "soll", "zu", "bzw.", "vor", "nach",
        "hallo", "hier", "dort", "da", "alle", "ihrem", "ihres",
        "abs", "b.", "finden", "fragen", "freundlichen", "fur", "gilt", "gruesse", "gruessen",
        "guten", "hilfe", "ihren", "ihrer", "innerhalb", "ist", "je", "kann", "koennen", "neue",
        "per", "schrieb", "seite", "soweit", "sowie", "tag", "unsere", "unserer", "weitere", "werden",
        "bzw", "erfolgt", "kunde", "kunden", "gerne", "herr", "frau", "null",
        "deinen", "dir", "herrn", "muss", "folgenden", "fall", "moeglich", "moechten", "jederzeit", "anderen",
        "jetzt", "verfuegung", "gemaess", "muessen", "sofern", "insbesondere", "z.b", "oft", "beispiel",
        "neuen", "beachten", "mir", "geehrter", "geehrte", "andere", "anderen", "anderer", "jedoch", "worden",
        "richtig", "ja", "eigentlich", "zwischen", "jeweils", "folgende"
    };
    private static final String[] ESW = new String[]{
        "the","a","an","and","or","but","if","then","else","than","because","so",
        "of","for","to","in","on","at","by","from","with","without","about","into",
        "is","are","was","were","be","been","being","do","does","did","doing",
        "has","have","had","having","will","would","can","could","should","may","might",
        "i","you","he","she","it","we","they","me","him","her","us","them",
        "my","your","his","her","its","our","their",
        "this","that","these","those","there","here","when","where","why","how",
        "very","too","also","just","only","not",
        "up","down","out","over","again","further","once",
        "however","therefore","thus","furthermore","moreover","overall",
        "today","yesterday","tomorrow","later","already",
        "to", "from", "you", "re", "app"
    };
    private static final Map<String, String> mapWords = new HashMap<>();

    public static final String HEADLINE = "id,year,created,filename,title,author,text,tags";

    static {
        mapWords.put("ß", "ss");
        mapWords.put(" sà rl ", " sarl ");
        mapWords.put("str.", " str ");
        mapWords.put("str ", " strasse ");
        mapWords.put("leuschnerstr ", " leuschnerstrasse ");
        mapWords.put("schwabstr ", " schwabstrasse ");
        mapWords.put("falkenstr ", " falkenstrasse ");
        mapWords.put("karl-pfaff-str ", " karl-pfaff-strasse ");
        mapWords.put("richard-wagner-str ", " richard-wagner-strasse ");
        mapWords.put("stettiner str ", " stettiner strasse ");
        mapWords.put("nk-berechnung ", " nk_berechnung ");
        mapWords.put("nk-abrechnung ", " nk-berechnung ");
        mapWords.put("nk berechnung ", " nk_berechnung ");
        mapWords.put("nk abrechnung ", " nk-berechnung ");
        mapWords.put("nebenkostenberechnung ", " nk-berechnung ");
        mapWords.put("nebenkostenabrechnung ", " nk-abrechnung ");
        mapWords.put("stettinerstr", "stettiner str");
        mapWords.put("tel ", " telefon ");
        mapWords.put("strabe", "strasse ");
        mapWords.put(" service ", " services ");
        mapWords.put(" preis ", " preise ");
        mapWords.put(" tagen ", " tage ");
    }

    private static final Map<String, String> normalizeWords = new HashMap<>();
    private static final Map<String, String> normalizeWords2 = new HashMap<>();

    static {
        normalizeWords.put("leuschnerstrasse 67", "leuschnerstrasse_67");
        normalizeWords.put("leuschner strasse 67", "leuschnerstrasse_67");
        normalizeWords.put("schwab strasse", "schwabstrasse");
        normalizeWords.put("schwabstrasse 84", "schwabstrasse_84");
        normalizeWords.put("falkenstrasse 15", "falkenstrasse_15");
        normalizeWords.put("falken strasse", "falkenstrasse");
        normalizeWords.put("karl-pfaff-strasse 9", "karl_pfaff_strasse_9");
        normalizeWords.put("karl pfaff strasse 9", "karl_pfaff_strasse_9");
        normalizeWords.put("karl-pfaff-strasse 20", "karl_pfaff_strasse_20");
        normalizeWords.put("karl pfaff strasse 20", "karl_pfaff_strasse_20");
        normalizeWords.put("richard-wagner-strasse 46", "richard_wagner_strasse_46");
        normalizeWords.put("richard wagner strasse 46", "richard_wagner_strasse_46");
        normalizeWords.put("stettiner strasse 11", "stettiner_strasse_11");
        normalizeWords.put("stettiner strasse 1l", "stettiner_strasse_11");
        normalizeWords.put("stettiner strasse 1i", "stettiner_strasse_11");
        normalizeWords.put("stettiner strasse l1", "stettiner_strasse_11");
        normalizeWords.put("stettiner strasse .11", "stettiner_strasse_11");
        normalizeWords.put("jonas grundler", "jonas_grundler");
        normalizeWords.put("jun hu", "jun_hu");
        normalizeWords.put("lilian hu", "lilian_hu");
        normalizeWords.put("leonie hu", "leonie_hu");
        normalizeWords.put("jonas grundler privat", "jonas_grundler_privat");
        normalizeWords.put("jonas_grundler privat", "jonas_grundler_privat");
        normalizeWords.put("grundler jonas", "jonas_grundler");
        normalizeWords.put("jonas meinrad grundler", "jonas_grundler");
        normalizeWords.put("date received", "received");
        normalizeWords.put("wohnungen grundler", "wohnungen_grundler");
        normalizeWords.put("merz schule", "merz_schule");
        normalizeWords.put("sparda bank", "sparda_bank");
        normalizeWords.put("baden wurttemberg", "baden_wuerttemberg");
        normalizeWords.put("baden wuerttemberg", "baden_wuerttemberg");
        normalizeWords.put("baden_wuerttembergische bank", "baden_wuerttembergische_bank");
        normalizeWords.put("hanseatic bank", "hanseatic_bank");
        normalizeWords.put("bw bank", "bw_bank");
        normalizeWords.put("ikano bank", "ikano_bank");
        normalizeWords.put("bmw bank", "bmw_bank");
        normalizeWords.put("post bank", "bmw_bank");
        normalizeWords.put("dz bank", "bmw_bank");
        normalizeWords.put("deutsche bank", "deutsche_bank");
        normalizeWords.put("advanzia bank", "advanzia_bank");
        normalizeWords.put("klarna bank", "klarna_bank");
        normalizeWords.put("vr bank", "vr_bank");
        normalizeWords.put("scharnhauser bank", "scharnhauser_bank");
        normalizeWords.put("hv ", "hausverwaltung ");
    }

    static {
        normalizeWords2.put("karl pfaff strasse", "karl_pfaff_strasse");
        normalizeWords2.put("stettiner strasse", "stettiner_strasse");
        normalizeWords2.put("richard wagner strasse", "richard_wagner_strasse");
    }

    private static final Set<String> stopWords = new HashSet<>();

    static {
        stopWords.addAll(Arrays.asList(GSW));
        stopWords.addAll(Arrays.asList(ESW));
    }

    private static final Pattern NUMBER_WORD_PATTERN =
            Pattern.compile(" (\\+?\\d+(?: \\d+)+) "); // Gruppe 1 = Mittelteil

    public static String transformNumberWords(String input) {
        Matcher m = NUMBER_WORD_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();

        while (m.find()) {
            String middle = m.group(1);            // z.B. "12 345 6"
            String replaced = middle.replace(' ', '_'); // "12_345_6"

            // Ersatz inklusive führendem & folgendem Leerzeichen
            String replacement = " " + replaced + " ";
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);
        return sb.toString();
    }



    private BuildCSV() {

    }

    public static File writeSingleLine(File dir, String fileIdentifier, String year) throws Exception {
        File csv = new File (dir, fileIdentifier + ".csv");
        BufferedWriter bw = new BufferedWriter(new FileWriter(csv));
        bw.write(HEADLINE);
        bw.newLine();
        String line = readSingleLine(dir, fileIdentifier, year, fileIdentifier, false);
        if (line == null) {
            System.out.println("fileIdentifier=" + fileIdentifier + ", year=" + year + ", dir=" + dir);
        }
        bw.write(line);
        bw.newLine();
        bw.close();
        return csv;
    }

    public static String readSingleLine(File dir, String fileIdentifier, String year, String id, boolean stopWithoutTags) throws IOException {
        File meta = new File(dir, fileIdentifier + ".meta.txt");
        File content = new File(dir, fileIdentifier + ".content.txt");
        File pdf = new File(dir, fileIdentifier + ".pdf.txt");
        File image = new File(dir, fileIdentifier + ".image.txt");
        //
        BufferedReader br = new BufferedReader(new FileReader(meta));
        String filename = br.readLine().substring("file:".length());
        String title = br.readLine().substring("title:".length());
        String author = br.readLine().substring("author:".length());
        String tags = br.readLine().substring("tags:".length());
        String source = br.readLine().substring("source:".length());
        String created = br.readLine().substring("created:".length());
        if (year == null) {
            if (created.length() >= 4) {
                year = created.substring(0, 4);
            } else {
                year = "0000";
            }
        }
        try {
            BufferedReader x = new BufferedReader(new FileReader(content));
            if (x.readLine() != null) {
                String line = x.readLine();
                if (line != null) {
                    if (line.startsWith("From:")) {
                        author = line.substring("From:".length()).trim();
                    }
                }
            }
            x.close();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        if (author.equals("null")) {
            author = "Jonas Grundler (Privat)";
        }
        br.close();
        StringBuilder text = new StringBuilder();
        appendText(content, text);
        appendText(pdf, text);
        appendText(image, text);

        if (!stopWithoutTags || tags.trim().length() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("\"");
            sb.append(id);
            sb.append("\",");
            sb.append(year);
            sb.append(",\"");
            sb.append(created);
            sb.append("\",\"");
            sb.append(reduce(filename));
            sb.append("\",\"");
            sb.append(preserveEmails(wordPreProcessing(removeStopWords(title))));
            sb.append("\",\"");
            sb.append(preserveEmails(wordPreProcessing(reduce(author))));
            sb.append("\",\"");
            sb.append(preserveEmails(wordPreProcessing(removeStopWords(text.toString()))));
            sb.append("\",\"");
            sb.append(tags);
            sb.append("\"");
            return sb.toString();
        } else {
            return null;
        }
    }

    private static String preserveEmails(String line) {
        StringBuilder sb = new StringBuilder();
        StringTokenizer st = new StringTokenizer(line);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            int index = token.indexOf('@');
            if (index > 0 && index < token.length() - 1) {
                token = token.replace("@", "__at__");
                token = token.replace('.', '_');
            }
            sb.append(token);
            sb.append(' ');
        }
        return sb.toString();
    }

    private static void appendText(File file, StringBuilder sb) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append(" ");
        }
        br.close();
    }

    private static String removeStopWords(String line) {
        line = reduce (line);
        line = transformNumberWords(line);
        StringTokenizer st = new StringTokenizer(line);
        StringBuilder sb = new StringBuilder();
        while (st.hasMoreTokens()) {
            String word = st.nextToken();
            if (word.length() > 1) {
                if (!stopWords.contains(word)) {
                    sb.append(word);
                    sb.append(' ');
                }
            }
        }
        return sb.toString().trim();
    }

    private static String wordPreProcessing(String string) {
        String line = " " + string + " ";
        String org;
        do {
            org = line;
            for (Map.Entry<String, String> e : mapWords.entrySet()) {
                line = line.replace(e.getKey(), e.getValue());
            }
            for (Map.Entry<String, String> e : normalizeWords.entrySet()) {
                line = line.replace(e.getKey(), e.getValue());
            }
            line = line.replaceAll("\\s+", " ");
        } while (!org.equals(line));
        do {
            org = line;
            for (Map.Entry<String, String> e : normalizeWords2.entrySet()) {
                line = line.replace(e.getKey(), e.getValue());
            }
            line = line.replaceAll("\\s+", " ");
        } while (!org.equals(line));
        line = line.trim();
        return line;

    }

    private static String reduce(String line) {
        line = line.toLowerCase();
        line = line.replaceAll("(?<=\\p{L})\\s*-\\s*(?=\\p{L})", " ");
        line = line.replaceAll("(\\p{L})\\.(\\s)", "$1$2");
        line = line.replace("|", " ");
        line = line.replace("=", " ");
        line = line.replace(",", " ");
        line = line.replace(";", " ");
        line = line.replace(":", " ");
        line = line.replace("\"", " ");
        line = line.replace("'", " ");
        line = line.replace(">", " ");
        line = line.replace("<", " ");
        line = line.replace("!", " ");
        line = line.replace("?", " ");
        line = line.replace("ß", "ss");
        line = line.replace("ä", "ae");
        line = line.replace("ö", "oe");
        line = line.replace("ü", "ue");
        line = line.replace("(", " ");
        line = line.replace(")", " ");
        line = line.replace("[", " ");
        line = line.replace("]", " ");
        line = line.replace("{", " ");
        line = line.replace("}", " ");
        line = line.replaceAll("[\\u00A0]", " ");
        line = line.replaceAll("[\\u2007]", " ");
        line = line.replaceAll("[\\u202F]", " ");
        line = line.replaceAll("[\\u200B-\\u200D\\uFEFF]", " ");
        line = line.replace("\u00AD", " ");
        line = line.replaceAll("\\s+", " ");
        line = line.replaceAll("#+", "#");
        return line;
    }
}
