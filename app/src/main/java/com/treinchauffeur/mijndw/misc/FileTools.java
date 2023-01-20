package com.treinchauffeur.mijndw.misc;

import java.io.File;
import java.util.ArrayList;

/**
 *
 * @author Leonk
 * Some handy tools for file handling
 */

public class FileTools {
    private static String TAG = "FileTools";
    public ArrayList<File> files;

    public FileTools() {
        files = new ArrayList<File>();
    };


    public void listFilesInFolder(final File folder) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesInFolder(fileEntry);
            } else {
                Logger.log(TAG, fileEntry.getName());
            }
        }
    }

    public ArrayList<File> loadFilesInFolder(final File folder) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                loadFilesInFolder(fileEntry);
            } else {
                //Logger.log(TAG, fileEntry.getName());
                files.add(fileEntry);
            }
        }
        return files;

    }
}
