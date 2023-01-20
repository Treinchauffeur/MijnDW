package com.treinchauffeur.mijndw.misc;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    public static void log(String TAG, String msg) {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        try (FileWriter f = new FileWriter("Logs.txt", true);
             BufferedWriter b = new BufferedWriter(f);
             PrintWriter p = new PrintWriter(b);) {
            p.println("["+formatter.format(date)+"] "+TAG+": "+msg);
            Log.d(TAG, "["+formatter.format(date)+"] "+TAG+": "+msg);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public static void debug(String TAG, String msg) {
        if(!Settings.DEBUG) return;
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        try (FileWriter f = new FileWriter("Logs.txt", true);
             BufferedWriter b = new BufferedWriter(f);
             PrintWriter p = new PrintWriter(b);) {
            p.println("["+formatter.format(date)+"] "+TAG+": "+msg);
            Log.d(TAG, "["+formatter.format(date)+"] "+TAG+": "+msg);
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}
