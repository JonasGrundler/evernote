package com.example.evernote.enex;

import com.example.evernote.LocalStore;
import com.example.evernote.BuildCSV;
import org.apache.commons.collections4.list.TreeList;

import java.io.*;
import java.util.*;

public class EnDocsToCSVs {

    private static File parsedDir = LocalStore.getSingleton().getEnex_batch_parsed().toFile();
    private static File csvSummary = new File(LocalStore.getSingleton().getEnex_batch_csv().toString(), "summary.csv");
    private static File tagsSummary = new File(LocalStore.getSingleton().getEnex_batch_csv().toString(), "tagsSummary.csv");

    public static void main(String[] args) {
        try {
            createSummaries();
            createSummary();
            createTagsHistory();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    private static void createTagsHistory() throws IOException {
        // summary

        BufferedReader br = new BufferedReader(new FileReader(csvSummary));

        SortedMap<Integer, List<String>> yearToTags = new TreeMap<>();

        // id,year,created,filename,title,author,text,tags
        String line;

        // header
        br.readLine();

        while ((line = br.readLine()) != null) {
            String[] rows = line.split(",");
            int year = Integer.parseInt(rows[1].trim());
            List<String> usedTags = yearToTags.get(year);
            if (usedTags == null) {
                usedTags = new ArrayList<>();
                yearToTags.put(year, usedTags);
            }
            String[] tags = rows[7].replace('"', ' ').split(",");
            for (String tag : tags) {
                usedTags.add(tag.trim());
            }
        }
        br.close();

        SortedMap<Integer, List<String>> yearToTagsFirstUsed = new TreeMap<>();
        Set<String> processedTags = new HashSet<>();
        for (Map.Entry<Integer, List<String>> e : yearToTags.entrySet()) {
            List<String> yearToTagsFirstUsedTags = yearToTagsFirstUsed.get(e.getKey());
            if (yearToTagsFirstUsedTags == null) {
                yearToTagsFirstUsedTags = new ArrayList<>();
                yearToTagsFirstUsed.put(e.getKey(), yearToTagsFirstUsedTags);
            }
            for (String tag : e.getValue()) {
                if (! processedTags.contains(tag)) {
                    yearToTagsFirstUsedTags.add(tag);
                    processedTags.add(tag);
                }
            }
        }

        BufferedWriter bw = new BufferedWriter(new FileWriter(tagsSummary));
        bw.write("year,tags");
        bw.newLine();
        for (Map.Entry<Integer, List<String>> e : yearToTagsFirstUsed.entrySet()) {
            bw.write(String.valueOf(e.getKey()));
            bw.write(',');
            bw.write('"');
            for (String tag : e.getValue()) {
                bw.write(tag);
                bw.write(',');
            }
            bw.write('"');
            bw.newLine();
        }
        bw.close();
    }

    private static void createSummaries() throws IOException {
        // enex
        for (File f : parsedDir.listFiles()) {
            // f - enex dir
            if (!f.isDirectory() && f.getName().endsWith(".done")) {
                String enex = f.getName().substring(0, f.getName().length() - 5);
                File csv = new File(LocalStore.getSingleton().getEnex_batch_csv().toFile(), enex + "_summary.csv");
                File csvDone = new File(LocalStore.getSingleton().getEnex_batch_csv().toFile(), enex + "_summary.done");
                String year = f.getName();
                year = year.substring(0, year.lastIndexOf('.'));
                if (year.charAt(3) == '-') {
                    year = year.substring(4);
                }
                year = year.substring(0, 4);
                try {
                    int y = Integer.parseInt(year);
                } catch (Exception e) {
                    year = null;
                }
                if (!csvDone.exists()) {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(csv));
                    bw.write(BuildCSV.HEADLINE);
                    bw.newLine();

                    File dir = new File(f.getParent(), enex + "_txt");

                    for (File p : dir.listFiles()) {
                        if (p.getName().endsWith(".done")) {
                            String fileIdentifier = p.getName().substring(0, p.getName().length() - 5);
                            String line = BuildCSV.readSingleLine(
                                    dir,
                                    fileIdentifier,
                                    year,
                                    enex.replace(' ', '_').replace('.', '_') + "." + fileIdentifier,
                                    true);
                            if (line != null) {
                                bw.write(line);
                                bw.newLine();
                            }
                        }
                    }

                    bw.close();
                    csvDone.createNewFile();
                }
            }
        }
    }

    public static void createSummary() throws IOException {
        Map<String, Integer> wc = new TreeMap<>();

        BufferedWriter bw = new BufferedWriter(new FileWriter(csvSummary));
        bw.write(BuildCSV.HEADLINE);
        bw.newLine();
        for (File f : LocalStore.getSingleton().getEnex_batch_csv().toFile().listFiles()) {
            if (f.getName().endsWith(".done")) {
                System.out.println("found:" + f.getName());
                File cv = new File(f.getParentFile(), f.getName().substring(0, f.getName().length() - 5) + ".csv");
                BufferedReader br = new BufferedReader(new FileReader(cv));
                String line = br.readLine();
                while ((line = br.readLine()) != null) {
                    bw.write(line);
                    bw.newLine();

                    //1929.0001 Baugesuch des Herrn Oskar Kleinbach..enex_summary.csv
                    //1929.0001 Baugesuch des Herrn Oskar Kleinbach..enex_summary.csv

                    /**
                     * statistics
                     */
                    StringTokenizer st1 = new StringTokenizer(line, ",");
                    st1.nextToken();
                    st1.nextToken();
                    st1.nextToken();
                    st1.nextToken();
                    line = st1.nextToken() + " " + st1.nextToken() + " " + st1.nextToken();
                    StringTokenizer st = new StringTokenizer(line, " \",");
                    String word;
                    while (st.hasMoreTokens()) {
                        word = st.nextToken();
                        Integer i = wc.get(word);
                        if (i == null) {
                            i = 1;
                        } else {
                            i = i + 1;
                        }
                        wc.put(word, i);
                    }
                    /**
                     *
                     */
                }
            }
        }
        bw.close();

        // statistics
        List<String> top = new ArrayList<>();
        TreeSet<String> l = new TreeSet<>();
        int count = 0;
        int max = 0;
        for (Map.Entry<String, Integer> e : wc.entrySet()) {
            max = Math.max(max, e.getValue());
            count += e.getValue();
            String s = "                 " + e.getValue();
            l.add(s.substring(s.length() - 10) + " " + e.getKey());

        }
        System.out.println("total " + wc.size() + " different words of total " + count);
        /*for (Map.Entry<String, Integer> e : wc.entrySet()) {
            if (e.getValue() > max / 100) {
                System.out.println("word " + e.getKey() + " frequency:" + e.getValue());
            }
        }*/
        int i = 0;
        int le = "     26633 ".length();
        for (String s : l.descendingSet()) {
            String key = s.substring(le).trim();
            System.out.println(s + "(" + key + ")");
            //
            boolean isnumber = false;
            try {
                Integer.parseInt(key);
                isnumber = true;
            } catch (Exception x) {}
            if (! isnumber) {
                top.add(key);
            }

            if (++i > 100) {
                break;
            }
        }

        /*
        //
        try {
            System.out.println("top=" + top);
            int max_top = 10;
            Map<String, Integer> sm = new HashMap<>();
            Map<String, Integer> sm2 = new HashMap<>();
            for (int o1 = 0; o1 < top.size() && o1 < max_top; o1++) {
                String s1 = " " + top.get(o1) + " ";
                sm.put(s1, 0);
                sm2.put(s1, 0);
                for (int o2 = 0; o2 < top.size() && o2 < max_top; o2++) {
                    String s2 = s1 + top.get(o2) + " ";
                    sm.put(s2, 0);
                    sm2.put(s2, 0);
                    for (int o3 = 0; o3 < top.size() && o3 < max_top; o3++) {
                        String s3 = s2 + top.get(o3) + " ";
                        sm.put(s3, 0);
                        sm2.put(s3, 0);
                    }
                }
            }

            File csvSummary = new File(LocalStore.getSingleton().getEnex_batch_csv().toString(), "summary.csv");
            BufferedReader br = new BufferedReader(new FileReader(csvSummary));
            String line = br.readLine();
            int lcount = 0;
            int maxx = -1;
            while ((line = br.readLine()) != null) {
                if ((lcount % 100) == 0) System.out.println("line:" + lcount);
                lcount++;
                String[] s = line.split(",");
                String sx = " " + s[4] + " " + s[5] + " " + s[6] + " ";
                for (Map.Entry<String, Integer> e : sm.entrySet()) {
                    int ix = 0;
                    do {
                        ix = sx.indexOf(e.getKey(), ix) + 1;
                        if (ix > 0) {
                            e.setValue(e.getValue() + 1);
                            int m = sm2.get(e.getKey()) + e.getKey().length();
                            sm2.put(e.getKey(), m);
                            maxx = Math.max(maxx, m);
                        }
                    } while (ix > 0);
                }
            }
            br.close();

            for (Map.Entry<String, Integer> e : sm2.entrySet()) {
                if (e.getValue() > maxx * 0.1) {
                    System.out.println("key " + e.getKey() + " needs " + e.getValue() + " in " + sm.get(e.getKey()) + " occurences");
                }
            }
            } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        */
    }
}
