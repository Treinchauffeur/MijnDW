package com.treinchauffeur.mijndw.misc;

import java.util.Random;

public class MiscTools {

    public static int generateRandomNumber(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min + 1) + min;
    }

}
