package com.treinchauffeur.mijndw;


import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.treinchauffeur.mijndw.io.DWReader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class MainActivity extends Activity {
    public static final int PICK_FILE_REQUEST = 1312;
    public static final String TAG = "MainActivity";
    private boolean isDev = false;
    ClipboardManager clipboard;

    DWReader dwReader;

    Button btnConvert, btnLoadFile, btnReset;
    EditText dwContent, icsContent;
    TextView loadedNone, loadedSuccess, loadedError, devHint;
    CardView infoCard;
    MaterialSwitch showProfession, showModifiers, fullDaysOnly;
    MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences prefs = getSharedPreferences(getString(R.string.sharedPrefs), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        ImageView bgImage = findViewById(R.id.bgImageCalendar);
        ImageView bgImageClock = findViewById(R.id.bgImageClock);
        ImageView bgImageTrainICM = findViewById(R.id.bgImageTrainICM);
        ImageView bgImageTrainVIRM = findViewById(R.id.bgImageTrainVIRM);
        ImageView bgImageTrainLoc = findViewById(R.id.bgImageTrainLoc);
        ImageView bgImageTrainVelaro = findViewById(R.id.bgImageTrainVelaro);

        int transparency = 70;
        bgImageTrainICM.setImageAlpha(transparency);
        bgImageTrainVIRM.setImageAlpha(transparency);
        bgImageTrainLoc.setImageAlpha(transparency);
        bgImageTrainVelaro.setImageAlpha(transparency);
        bgImage.setImageAlpha(transparency);
        bgImageClock.setImageAlpha(transparency);


        //Loading all the UI elements
        devHint = findViewById(R.id.devHint);
        dwContent = findViewById(R.id.dwContent);
        icsContent = findViewById(R.id.icsContent);
        btnLoadFile = findViewById(R.id.btnLoadFile);
        btnConvert = findViewById(R.id.btnConvertFile);
        btnReset = findViewById(R.id.btnReset);
        loadedNone = findViewById(R.id.loadedNone);
        loadedSuccess = findViewById(R.id.loadedSuccessfully);
        loadedError = findViewById(R.id.loadedError);
        infoCard = findViewById(R.id.infoCard);
        showProfession = findViewById(R.id.professionCheckBox);
        showModifiers = findViewById(R.id.modifiersCheckBox);
        fullDaysOnly = findViewById(R.id.wholeDayCheckBox);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.devMode) {
                    item.setChecked(!item.isChecked());
                    setDev(item.isChecked());
                    editor.putBoolean("DevMode", item.isChecked());
                    editor.apply();
                }
                if (item.getItemId() == R.id.mailDev) {
                    final Intent emailIntent = new Intent(Intent.ACTION_SEND);
                    emailIntent.setType("text/plain");
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"treinchauffeur.dev@gmail.com"});
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Over: Mijn DW");
                    emailIntent.putExtra(Intent.EXTRA_TEXT, "Mijn DW versie " + BuildConfig.VERSION_NAME);
                    startActivity(Intent.createChooser(emailIntent, "E-mail versturen..."));
                }
                return false;
            }
        });
        if (prefs.contains("DevMode")) {
            isDev = prefs.getBoolean("DevMode", false);
            toolbar.getMenu().getItem(0).setChecked(isDev);
        }

        if (isDev) {
            devHint.setVisibility(View.VISIBLE);
            dwContent.setVisibility(View.VISIBLE);
            icsContent.setVisibility(View.VISIBLE);
        } else {
            devHint.setVisibility(View.GONE);
            dwContent.setVisibility(View.GONE);
            icsContent.setVisibility(View.GONE);
        }
        if (!prefs.contains("displayProfession")) {
            editor.putBoolean("displayProfession", showProfession.isChecked());
        } else {
            showProfession.setChecked(prefs.getBoolean("displayProfession", false));
        }

        if (!prefs.contains("displayModifiers")) {
            editor.putBoolean("displayModifiers", showModifiers.isChecked());
        } else {
            showModifiers.setChecked(prefs.getBoolean("displayModifiers", false));
        }

        if (!prefs.contains("fullDaysOnly")) {
            editor.putBoolean("fullDaysOnly", fullDaysOnly.isChecked());
        } else {
            fullDaysOnly.setChecked(prefs.getBoolean("fullDaysOnly", false));
        }
        editor.apply();

        showProfession.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                btnReset.callOnClick();
                SharedPreferences prefs = getSharedPreferences(getString(R.string.sharedPrefs), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("displayProfession", compoundButton.isChecked());
                editor.apply();
            }
        });

        showModifiers.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                btnReset.callOnClick();
                SharedPreferences prefs = getSharedPreferences(getString(R.string.sharedPrefs), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("displayModifiers", compoundButton.isChecked());
                editor.apply();
            }
        });

        fullDaysOnly.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                btnReset.callOnClick();
                SharedPreferences prefs = getSharedPreferences(getString(R.string.sharedPrefs), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("fullDaysOnly", compoundButton.isChecked());
                editor.apply();
            }
        });

        infoCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                infoCard.setVisibility(View.GONE);
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadedNone.setVisibility(View.VISIBLE);
                loadedSuccess.setVisibility(View.GONE);
                loadedError.setVisibility(View.GONE);
                btnLoadFile.setVisibility(View.VISIBLE);
                btnConvert.setVisibility(View.GONE);
                btnReset.setVisibility(View.GONE);
            }
        });


        dwReader = new DWReader(this);

        btnLoadFile.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/*");
            startActivityForResult(intent, PICK_FILE_REQUEST);
        });

        Intent intent = getIntent();
        if (intent.getAction().equals(Intent.ACTION_VIEW)) {
            Uri fileUri = intent.getData();
            handleFileIntent(fileUri);
        }
    }

    private void setDev(boolean checked) {
        isDev = checked;
        if (checked) {
            devHint.setVisibility(View.VISIBLE);
            dwContent.setVisibility(View.VISIBLE);
            icsContent.setVisibility(View.VISIBLE);
        } else {
            devHint.setVisibility(View.GONE);
            dwContent.setVisibility(View.GONE);
            icsContent.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && intent != null) {
            Log.d(TAG, "File retrieved, loading.. " + intent);
            Uri fileUri = intent.getData();
            handleFileIntent(fileUri);
        }
    }

    private void handleFileIntent(Uri uri) {
        dwReader.resetData();
        dwReader.startConversion(this, uri);
        dwContent.setText(dwReader.fullFileString());
        icsContent.setText(dwReader.getCalendarICS());
        if (dwReader.dw.size() > 0) {

            loadedSuccess.setText("Week " + dwReader.weekNumber + " van jaar " + dwReader.yearNumber + " geladen!");
            loadedSuccess.setVisibility(View.VISIBLE);
            loadedNone.setVisibility(View.GONE);
            loadedError.setVisibility(View.GONE);

            btnLoadFile.setVisibility(View.GONE);
            btnReset.setVisibility(View.VISIBLE);
            btnConvert.setVisibility(View.VISIBLE);

            //Saves the file to a temporary location & offers it to the user using an intent.
            //Sends user to the Google Calendar app page on the play store if no calendar app is available.
            btnConvert.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        File file = new File(getExternalFilesDir(null).getPath() + "/converted.ics"); // Null -> temp location
                        FileOutputStream out = new FileOutputStream(file);
                        OutputStreamWriter writer = new OutputStreamWriter(out);

                        writer.write(dwReader.getCalendarICS());
                        writer.close();
                        out.close();

                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        Uri uri = FileProvider.getUriForFile(MainActivity.this, getApplicationContext().getPackageName() + ".provider", file);
                        intent.setDataAndType(uri, "text/calendar");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(MainActivity.this, "Please install a calendar app.", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.calendar")));
                    } catch (IOException e) {
                        Toast.makeText(MainActivity.this, "An error occurred.", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
            });
        } else {
            loadedError.setVisibility(View.VISIBLE);

            loadedSuccess.setVisibility(View.GONE);
            loadedNone.setVisibility(View.GONE);

            btnLoadFile.setVisibility(View.VISIBLE);
            btnReset.setVisibility(View.GONE);
            btnConvert.setVisibility(View.GONE);
        }
    }
}