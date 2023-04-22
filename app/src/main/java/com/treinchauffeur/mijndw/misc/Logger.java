package com.treinchauffeur.mijndw.misc;

import android.util.Log;

import com.treinchauffeur.mijndw.MainActivity;

public class Logger {

    /**
     * We use our own Logger for code-efficiency. It logs when either app is set to debug or
     * when a dev is using the app.
     *
     * @param TAG   TAG to pass to the main logger.
     * @param toLog message to log.
     */
    public static void debug(String TAG, String toLog) {
        if (Settings.DEBUG || MainActivity.isDev) Log.d(TAG, toLog);
    }
}
