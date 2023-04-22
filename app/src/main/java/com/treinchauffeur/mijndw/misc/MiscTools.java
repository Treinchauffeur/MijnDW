package com.treinchauffeur.mijndw.misc;

import java.util.Random;

public class MiscTools {

    /**
     * Says what it does on the tin. Generates a random number.
     *
     * @param min sets the minimum.
     * @param max sets the maximum.
     * @return the generated number as an integer.
     */
    public static int generateRandomNumber(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min + 1) + min;
    }
}
