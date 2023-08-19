package com.treinchauffeur.mijndw.misc;

/**
 * @author treinchauffeur
 * Defines settings for the app.
 */

public class Settings {

    //Whether we should log shift details to logcat.
    public static final boolean DEBUG = true;

    //Animation definitions: setting the speeds (the time it takes to move from A to B)..
    // ..at which the bottom and top trains move.
    public static final int velaroMinSpeed = 7000, velaroMaxSpeed = 15000;
    public static final int icmMinSpeed = 15000, icmMaxSpeed = 25000;
    public static final long CLOCK_SPIN_DURATION = 15000;

    //Developer information
    public static final String DEV_EMAIL = "treinchauffeur.dev@gmail.com";
}
