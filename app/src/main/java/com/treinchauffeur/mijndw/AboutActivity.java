package com.treinchauffeur.mijndw;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class AboutActivity extends Activity {

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

    private void handleButtons() {
        MaterialButton emailButton = findViewById(R.id.emailButtonAbout);
        emailButton.setOnClickListener(v -> {
            final Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:"));
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"treinchauffeur.dev@gmail.com"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Over: Mijn DW");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Mijn DW versie " + BuildConfig.VERSION_NAME);
            startActivity(Intent.createChooser(emailIntent, "E-mail versturen.."));
        });

        MaterialButton privacyButton = findViewById(R.id.privacyButtonAbout);
        privacyButton.setOnClickListener(v -> {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/LeonKlaczynski/MijnDW/blob/master/Privacy%20Policy")));
        });
    }
}