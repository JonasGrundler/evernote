package com.example.evernote.enex;

import com.example.evernote.LocalStore;
import com.example.evernote.EnDocHelper;

import java.io.File;
import java.nio.file.Paths;


/*
    read .enex-Files from a directory and run the process
 */
public class EnexFilesProcessor {

    public static void main (String[] args) {

        int redo = EnDocHelper.REDO_NOT;

        try {

            File enexFiles = LocalStore.getSingleton().getEnex_batch_exports().toFile();
            System.out.println("from:" + enexFiles.toString());
            for (File enexFile : enexFiles.listFiles()) {
                if (enexFile.getAbsolutePath().endsWith(".enex")) {
                    System.out.println("working on " + enexFile.getAbsolutePath());
                    File done = new File (LocalStore.getSingleton().getEnex_batch_parsed().toString(), enexFile.getName() + ".done");
                    if (! done.exists() || redo != EnDocHelper.REDO_NOT) {
                        EnexFileToEnDocs eh = new EnexFileToEnDocs(Paths.get(enexFile.toURI()), redo);
                        while (eh.next());
                        eh.close();
                    }
                    if (! done.exists()) {
                        done.createNewFile();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }
}
