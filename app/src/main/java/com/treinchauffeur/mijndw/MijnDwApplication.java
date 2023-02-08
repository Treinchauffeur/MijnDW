package com.treinchauffeur.mijndw;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

public class MijnDwApplication extends Application {

    @Override
    public void onCreate() {
        DynamicColors.applyToActivitiesIfAvailable(this);
        super.onCreate();
    }
}
