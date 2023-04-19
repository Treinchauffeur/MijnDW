package com.treinchauffeur.mijndw;


import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.treinchauffeur.mijndw.io.DWReader;
import com.treinchauffeur.mijndw.misc.MiscTools;

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
    CardView infoCard, usageCard;
    MaterialSwitch showProfession, showModifiers, fullDaysOnly;
    MaterialToolbar toolbar;
    private boolean isAnimating = false;

    /**
     * Starting up the app, loading the layout including all the views.
     * Doing some UI stuff like programmatically setting the background images' transparency.
     * Handling a lot of buttons like sending an email to the developer & hidden DevMode option.
     * Also handling all the options the user can set to process their file.
     * Finally, we're animating the background & infoCard.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences prefs = getSharedPreferences(getString(R.string.sharedPrefs), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        toolbar = findViewById(R.id.toolbar);
        ImageView bgImage = findViewById(R.id.bgImageCalendar);
        ImageView bgImageClock = findViewById(R.id.bgImageClock);
        ImageView bgImageTrainICM = findViewById(R.id.bgImageTrainICM);
        ImageView bgImageTrainVIRM = findViewById(R.id.bgImageTrainVIRM);
        ImageView bgImageTrainLoc = findViewById(R.id.bgImageTrainLoc);
        ImageView bgImageTrainVelaro = findViewById(R.id.bgImageTrainVelaro);


        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        int transparency = (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) ? 50 : 110;
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
        usageCard = findViewById(R.id.usageCard);
        showProfession = findViewById(R.id.professionCheckBox);
        showModifiers = findViewById(R.id.modifiersCheckBox);
        fullDaysOnly = findViewById(R.id.wholeDayCheckBox);

        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.devMode) {
                item.setChecked(!item.isChecked());
                setDev(item.isChecked());
            }
            if (item.getItemId() == R.id.mailDev) {
                final Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:"));
                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"treinchauffeur.dev@gmail.com"});
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Over: Mijn DW");
                emailIntent.putExtra(Intent.EXTRA_TEXT, "Mijn DW versie " + BuildConfig.VERSION_NAME);
                startActivity(Intent.createChooser(emailIntent, "E-mail versturen.."));
            }
            return false;
        });

        final int[] locPressedAmount = {0};
        usageCard.setOnClickListener(v -> {
            if (locPressedAmount[0] > 9) {
                setDev(!isDev);
                locPressedAmount[0] = 0;
            }
            locPressedAmount[0]++;
        });

        if (prefs.contains("DevMode")) {
            setDevWithoutToast(prefs.getBoolean("DevMode", false));
        } else {
            setDevWithoutToast(false);
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

        showProfession.setOnCheckedChangeListener((compoundButton, b) -> {
            btnReset.callOnClick();
            SharedPreferences prefs1 = getSharedPreferences(getString(R.string.sharedPrefs), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor1 = prefs1.edit();
            editor1.putBoolean("displayProfession", compoundButton.isChecked());
            editor1.apply();
        });

        showModifiers.setOnCheckedChangeListener((compoundButton, b) -> {
            btnReset.callOnClick();
            SharedPreferences prefs12 = getSharedPreferences(getString(R.string.sharedPrefs), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor12 = prefs12.edit();
            editor12.putBoolean("displayModifiers", compoundButton.isChecked());
            editor12.apply();
        });

        fullDaysOnly.setOnCheckedChangeListener((compoundButton, b) -> {
            btnReset.callOnClick();
            SharedPreferences prefs13 = getSharedPreferences(getString(R.string.sharedPrefs), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor13 = prefs13.edit();
            editor13.putBoolean("fullDaysOnly", compoundButton.isChecked());
            editor13.apply();
        });

        if (!prefs.contains("dismissedInfoCard")) {
            infoCard.setOnClickListener(view -> {
                infoCard.setVisibility(View.GONE);
                SharedPreferences prefs12 = getSharedPreferences(getString(R.string.sharedPrefs), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor12 = prefs12.edit();
                editor12.putBoolean("dismissedInfoCard", true);
                editor12.apply();
            });
        } else {
            infoCard.setVisibility(View.GONE);
        }

        btnReset.setOnClickListener(view -> {
            loadedNone.setVisibility(View.VISIBLE);
            loadedSuccess.setVisibility(View.GONE);
            loadedError.setVisibility(View.GONE);
            btnLoadFile.setVisibility(View.VISIBLE);
            btnConvert.setVisibility(View.GONE);
            btnReset.setVisibility(View.GONE);
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

        performAnimations();
    }

    /**
     * We're animating the background train images as well as hinting to the user that the infoCard can be dismissed.
     * Animations keep things nice & dynamic.
     */
    @SuppressLint("Recycle")
    private void performAnimations() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager.isPowerSaveMode() || isAnimating) return;
        isAnimating = true;

        ImageView bgImageTrainICM = findViewById(R.id.bgImageTrainICM);
        ImageView bgImageTrainVIRM = findViewById(R.id.bgImageTrainVIRM);
        ImageView bgImageTrainLoc = findViewById(R.id.bgImageTrainLoc);
        ImageView bgImageTrainVelaro = findViewById(R.id.bgImageTrainVelaro);
        int minStartDelay = 0, maxStartDelay = 3000;
        int minTrainPassTime = 5000, maxTrainPassTime = 15000;

        ObjectAnimator animationICM = ObjectAnimator.ofFloat(bgImageTrainICM, "translationX", 0f);
        animationICM.setDuration(MiscTools.generateRandomNumber(minTrainPassTime, maxTrainPassTime));
        animationICM.setInterpolator(new AccelerateDecelerateInterpolator());
        bgImageTrainICM.setX(-1500f);
        Runnable icmRunnable = animationICM::start;
        bgImageTrainICM.postDelayed(icmRunnable, MiscTools.generateRandomNumber(minStartDelay, maxStartDelay));

        ObjectAnimator animationVIRM = ObjectAnimator.ofFloat(bgImageTrainVIRM, "translationX", 0f);
        animationVIRM.setDuration(MiscTools.generateRandomNumber(minTrainPassTime, maxTrainPassTime));
        animationVIRM.setInterpolator(new AccelerateDecelerateInterpolator());
        bgImageTrainVIRM.setX(-1500f);
        Runnable virmRunnable = animationVIRM::start;
        bgImageTrainVIRM.postDelayed(virmRunnable, MiscTools.generateRandomNumber(minStartDelay, maxStartDelay));

        ObjectAnimator animationLoc = ObjectAnimator.ofFloat(bgImageTrainLoc, "translationX", 0f);
        animationLoc.setDuration(MiscTools.generateRandomNumber(minTrainPassTime, maxTrainPassTime));
        animationLoc.setInterpolator(new AccelerateDecelerateInterpolator());
        bgImageTrainLoc.setX(1500f);
        Runnable locRunnable = animationLoc::start;
        bgImageTrainLoc.postDelayed(locRunnable, MiscTools.generateRandomNumber(minStartDelay, maxStartDelay));

        ObjectAnimator animationVelaro = ObjectAnimator.ofFloat(bgImageTrainVelaro, "translationX", 0f);
        animationVelaro.setDuration(MiscTools.generateRandomNumber(minTrainPassTime, maxTrainPassTime));
        animationVelaro.setInterpolator(new AccelerateDecelerateInterpolator());
        bgImageTrainVelaro.setX(1500f);
        Runnable velaroRunnable = animationVelaro::start;
        bgImageTrainVelaro.postDelayed(velaroRunnable, MiscTools.generateRandomNumber(minStartDelay, maxStartDelay));

        if (infoCard.getVisibility() == View.VISIBLE) {
            Runnable animationRunnable = () -> {
                if (infoCard.getVisibility() == View.GONE)
                    return;

                final long now = SystemClock.uptimeMillis();
                final MotionEvent pressEvent = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 0, 0, 0);
                infoCard.dispatchTouchEvent(pressEvent);

                new Handler().postDelayed(() -> {
                    final long now1 = SystemClock.uptimeMillis();
                    final MotionEvent cancelEvent = MotionEvent.obtain(now1, now1, MotionEvent.ACTION_CANCEL, 0, 0, 0);
                    infoCard.dispatchTouchEvent(cancelEvent);
                }, 1000);
            };

            Runnable finalRunnable = () -> isAnimating = false;

            infoCard.postDelayed(animationRunnable, 5000);
            infoCard.postDelayed(animationRunnable, 10000);
            infoCard.postDelayed(animationRunnable, 15000);
            infoCard.postDelayed(animationRunnable, 20000);
            infoCard.postDelayed(animationRunnable, 25000);
            infoCard.postDelayed(finalRunnable, 27000);
        }
    }

    /**
     * Turns on or off developer mode. In Devmode you get additional fields with the raw data of both
     * the original file & the iCal output.
     *
     * @param check whether DevMode should be on or off
     */
    private void setDev(boolean check) {
        isDev = check;
        toolbar.getMenu().getItem(0).setChecked(check);
        toolbar.getMenu().getItem(0).setVisible(check);
        if (check) {
            devHint.setVisibility(View.VISIBLE);
            dwContent.setVisibility(View.VISIBLE);
            icsContent.setVisibility(View.VISIBLE);
        } else {
            devHint.setVisibility(View.GONE);
            dwContent.setVisibility(View.GONE);
            icsContent.setVisibility(View.GONE);
        }

        SharedPreferences prefs = getSharedPreferences(getString(R.string.sharedPrefs), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("DevMode", check);
        editor.apply();
        Toast.makeText(MainActivity.this, check ? "You're now a developer!" : "You're not a developer anymore!", Toast.LENGTH_SHORT).show();
    }

    /**
     * Turns on or off developer mode. In Devmode you get additional fields with the raw data of both
     * the original file & the iCal output. This one doesn't display a Toast
     *
     * @param check whether DevMode should be on or off
     */
    private void setDevWithoutToast(boolean check) {
        isDev = check;
        toolbar.getMenu().getItem(0).setChecked(check);
        toolbar.getMenu().getItem(0).setVisible(check);
        if (check) {
            devHint.setVisibility(View.VISIBLE);
            dwContent.setVisibility(View.VISIBLE);
            icsContent.setVisibility(View.VISIBLE);
        } else {
            devHint.setVisibility(View.GONE);
            dwContent.setVisibility(View.GONE);
            icsContent.setVisibility(View.GONE);
        }

        SharedPreferences prefs = getSharedPreferences(getString(R.string.sharedPrefs), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("DevMode", check);
        editor.apply();
    }

    /**
     * Handling the incoming file after the user selected it.
     *
     * @param requestCode the code used to recognise the request
     * @param resultCode  success or not
     * @param intent      recieved intent
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && intent != null) {
            Log.d(TAG, "File retrieved, loading.. " + intent);
            Uri fileUri = intent.getData();
            handleFileIntent(fileUri);
        }
    }

    /**
     * Starts reading the file & checks whether it's valid.
     *
     * @param uri URI that points to the file
     */
    @SuppressLint("SetTextI18n")
    private void handleFileIntent(Uri uri) {
        dwReader.resetData();
        dwReader.startConversion(this, uri);
        dwContent.setText(dwReader.fullFileString());
        icsContent.setText(dwReader.getCalendarICS());
        if (DWReader.dw.size() > 0) {

            loadedSuccess.setText("Week " + DWReader.weekNumber + " van jaar " + DWReader.yearNumber + " geladen!");
            loadedSuccess.setVisibility(View.VISIBLE);
            loadedNone.setVisibility(View.GONE);
            loadedError.setVisibility(View.GONE);

            btnLoadFile.setVisibility(View.GONE);
            btnReset.setVisibility(View.VISIBLE);
            btnConvert.setVisibility(View.VISIBLE);

            //Saves the file to a temporary location & offers it to the user using an intent.
            //Sends user to the Google Calendar app page on the play store if no calendar app is available.
            btnConvert.setOnClickListener(view -> {
                try {
                    File file = new File(getExternalFilesDir(null).getPath() + "/converted.ics"); // Null -> temp location
                    FileOutputStream out = new FileOutputStream(file);
                    OutputStreamWriter writer = new OutputStreamWriter(out);

                    writer.write(dwReader.getCalendarICS());
                    writer.close();
                    out.close();

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri uri1 = FileProvider.getUriForFile(MainActivity.this, getApplicationContext().getPackageName() + ".provider", file);
                    intent.setDataAndType(uri1, "text/calendar");
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

    /**
     * We want the animations to start over to keep a nice & dynamic vibe.
     */
    @Override
    protected void onResume() {
        performAnimations();
        super.onResume();
    }
}