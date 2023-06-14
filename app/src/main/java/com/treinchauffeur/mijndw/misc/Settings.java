package com.treinchauffeur.mijndw.misc;

/**
 * @author treinchauffeur
 * Defines settings for the app.
 */

public class Settings {

    //Whether we should log shift details to logcat.
    public static final boolean DEBUG = true;

    //Animation definitions: setting the speeds at which the bottom and top trains move.
    public static final int velaroMinSpeed = 7000, velaroMaxSpeed = 15000;
    public static final int icmMinSpeed = 11000, icmMaxSpeed = 21000;
    public static final long CLOCK_SPIN_DURATION = 15000;
    public static final String DEV_EMAIL = "treinchauffeur.dev@gmail.com";
}
