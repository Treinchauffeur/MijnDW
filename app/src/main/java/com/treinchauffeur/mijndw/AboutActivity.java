package com.treinchauffeur.mijndw;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.treinchauffeur.mijndw.misc.Settings;

public class AboutActivity extends Activity {

    /**
     * Runs on app startup.
     * We're setting the alpha of the background images because, in xml, it doesn't work apparently..
     * Also setting navigation actions: toolbar 'back' icon.
     *
     * @param savedInstanceState honestly, I don't know what this is.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ImageView bgImageCalendar = findViewById(R.id.bgImageCalendarAbout);
        ImageView bgImageClock = findViewById(R.id.bgImageClockAbout);
        ImageView bgImageTrainICM = findViewById(R.id.bgImageTrainICMAbout);
        ImageView bgImageTrainVIRM = findViewById(R.id.bgImageTrainVIRMAbout);
        ImageView bgImageTrainLoc = findViewById(R.id.bgImageTrainLocAbout);
        ImageView bgImageTrainVelaro = findViewById(R.id.bgImageTrainVelaroAbout);

        MaterialToolbar toolbar = findViewById(R.id.toolbarAbout);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        int transparency = (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) ? 50 : 110;
        bgImageTrainICM.setImageAlpha(transparency);
        bgImageTrainVIRM.setImageAlpha(transparency);
        bgImageTrainLoc.setImageAlpha(transparency);
        bgImageTrainVelaro.setImageAlpha(transparency);
        bgImageCalendar.setImageAlpha(transparency);
        bgImageClock.setImageAlpha(transparency);

        handleButtons();
    }

    /**
     * Binds the actions to the buttons placed on the activity.
     */
    private void handleButtons() {
        MaterialButton emailButton = findViewById(R.id.emailButtonAbout);
        emailButton.setOnClickListener(v -> {
            final Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:"));
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{Settings.DEV_EMAIL});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Over: Mijn DW");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Mijn DW versie " + BuildConfig.VERSION_NAME);
            startActivity(Intent.createChooser(emailIntent, "E-mail versturen.."));
        });

        MaterialButton privacyButton = findViewById(R.id.privacyButtonAbout);
        privacyButton.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/treinchauffeur/MijnDW/blob/master/Privacy%20Policy"))));
    }
}