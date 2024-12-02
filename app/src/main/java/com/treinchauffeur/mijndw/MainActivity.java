package com.treinchauffeur.mijndw;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.treinchauffeur.mijndw.io.ShiftsFileReader;
import com.treinchauffeur.mijndw.misc.Circus;
import com.treinchauffeur.mijndw.misc.Logger;
import com.treinchauffeur.mijndw.misc.Settings;
import com.treinchauffeur.mijndw.ui.ContinuousBackgroundAnimator;
import com.treinchauffeur.mijndw.ui.StartupBackgroundAnimator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.Objects;
import java.util.Properties;

public class MainActivity extends Activity {
    public static final int PICK_FILE_REQUEST = 1312, UPDATE_REQUEST_CODE = 1759;
    public static final String TAG = "MainActivity";

    public static boolean isDev = false;
    private boolean returnDaysOff = false, returnOnlyVTA = false;

    ShiftsFileReader shiftsFileReader;
    Button btnConvert, btnLoadFile, btnReset, buttonSettings;
    EditText shiftsFileContentView, iCalContentView;
    TextView loadedNone, loadedSuccess, loadedError, devHint;
    CardView welcomeCard, usageCard, updateCard, remoteCard;
    MaterialSwitch showProfession, showModifiers, daysOffSwitch, onlyVTA;
    MaterialToolbar toolbar;

    FirebaseAnalytics analytics;

    /**
     * Starting up the app, loading the layout including all the views.
     * Doing some UI stuff like programmatically setting the background images' transparency.
     * Handling a lot of buttons like sending an email to the developer & hidden DevMode option.
     * Also handling all the options the user can set to process their file.
     * Finally, we're animating the background & infoCard.
     * After all is said and done, we'll check for updates and prompt the user if one is available.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        SharedPreferences prefs = getSharedPreferences(getString(R.string.sharedPrefs), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        analytics = FirebaseAnalytics.getInstance(this);

        toolbar = findViewById(R.id.toolbar);
        ImageView bgImageCalendar = findViewById(R.id.bgImageCalendar);
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
        bgImageCalendar.setImageAlpha(transparency);
        bgImageClock.setImageAlpha(transparency);

        //Loading all the UI elements
        devHint = findViewById(R.id.devHint);
        shiftsFileContentView = findViewById(R.id.dwContent);
        iCalContentView = findViewById(R.id.icsContent);
        btnLoadFile = findViewById(R.id.btnLoadFile);
        btnConvert = findViewById(R.id.btnConvertFile);
        btnReset = findViewById(R.id.btnReset);
        buttonSettings = findViewById(R.id.advancedSettingsButton);
        loadedNone = findViewById(R.id.loadedNone);
        loadedSuccess = findViewById(R.id.loadedSuccessfully);
        loadedError = findViewById(R.id.loadedError);
        welcomeCard = findViewById(R.id.infoCard);
        usageCard = findViewById(R.id.usageCard);
        updateCard = findViewById(R.id.updateCard);
        remoteCard = findViewById(R.id.remoteCard);
        showProfession = findViewById(R.id.professionCheckBox);
        showModifiers = findViewById(R.id.modifiersCheckBox);
        daysOffSwitch = findViewById(R.id.daysOffCheckBox);
        onlyVTA = findViewById(R.id.onlyVTACheckBox);

        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.devMode) {
                item.setChecked(!item.isChecked());
                setDev(item.isChecked());
            }
            if (item.getItemId() == R.id.mailDev) {
                final Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:"));
                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{Settings.DEV_EMAIL});
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Over: Mijn DW");
                emailIntent.putExtra(Intent.EXTRA_TEXT, "Mijn DW versie " + BuildConfig.VERSION_NAME);
                startActivity(Intent.createChooser(emailIntent, "E-mail versturen.."));
            }
            if (item.getItemId() == R.id.aboutApp) {
                Intent aboutIntent = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(aboutIntent);
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
            editor.putBoolean("fullDaysOnly", false);
        }
        if (!prefs.contains("daysOff")) {
            editor.putBoolean("daysOff", daysOffSwitch.isChecked());
        } else {
            daysOffSwitch.setChecked(prefs.getBoolean("daysOff", false));
        }

        if (!prefs.contains("onlyVTA")) {
            editor.putBoolean("onlyVTA", onlyVTA.isChecked());
        } else {
            onlyVTA.setChecked(prefs.getBoolean("onlyVTA", false));
        }

        if (!prefs.contains("whatsNew") || !prefs.getString("whatsNew", "").equals(BuildConfig.VERSION_NAME)) {
            editor.putString("whatsNew", BuildConfig.VERSION_NAME);
        } else {
            findViewById(R.id.newFeatureTextView).setVisibility(View.GONE);
        }

        editor.apply();

        if (!daysOffSwitch.isChecked()) {
            onlyVTA.setChecked(false);
            onlyVTA.setVisibility(View.GONE);
        } else {
            onlyVTA.setVisibility(View.VISIBLE);
        }

        setShouldReturnDaysOff(daysOffSwitch.isChecked(), onlyVTA.isChecked());

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
        
        daysOffSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            SharedPreferences prefs14 = getSharedPreferences(getString(R.string.sharedPrefs), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor14 = prefs14.edit();
            editor14.putBoolean("daysOff", compoundButton.isChecked());
            editor14.apply();
            returnDaysOff = compoundButton.isChecked();
            btnReset.callOnClick();
            if (!compoundButton.isChecked()) {
                onlyVTA.setChecked(false);
                onlyVTA.setVisibility(View.GONE);
            } else {
                onlyVTA.setVisibility(View.VISIBLE);
            }
        });

        onlyVTA.setOnCheckedChangeListener((compoundButton, b) -> {
            returnOnlyVTA = compoundButton.isChecked();
            btnReset.callOnClick();
            SharedPreferences prefs15 = getSharedPreferences(getString(R.string.sharedPrefs), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor15 = prefs15.edit();
            editor15.putBoolean("onlyVTA", compoundButton.isChecked());
            editor15.apply();
        });

        if (!prefs.contains("dismissedInfoCard")) {
            welcomeCard.setOnClickListener(view -> {
                welcomeCard.setVisibility(View.GONE);
                SharedPreferences prefs12 = getSharedPreferences(getString(R.string.sharedPrefs), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor12 = prefs12.edit();
                editor12.putBoolean("dismissedInfoCard", true);
                editor12.apply();
            });
        } else {
            welcomeCard.setVisibility(View.GONE);
        }

        btnReset.setOnClickListener(view -> {
            loadedNone.setVisibility(View.VISIBLE);
            loadedSuccess.setVisibility(View.GONE);
            loadedError.setVisibility(View.GONE);
            btnLoadFile.setVisibility(View.VISIBLE);
            btnConvert.setVisibility(View.GONE);
            btnReset.setVisibility(View.GONE);
        });

        shiftsFileReader = new ShiftsFileReader(this);

        btnLoadFile.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/*");
            startActivityForResult(intent, PICK_FILE_REQUEST);
        });

        Intent intent = getIntent();
        if (Objects.equals(intent.getAction(), Intent.ACTION_VIEW)) {
            Uri fileUri = intent.getData();
            handleFileIntent(fileUri);
        }

        buttonSettings.setOnClickListener(view -> {
            btnReset.callOnClick();
            Intent intent1 = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent1);
        });

        performAnimations();
        checkForAppUpdates();
        remoteConfig();

        // ;)
        Circus circus = new Circus(this, toolbar);
        circus.startTheShow();
    }

    /**
     * We want to send the users a message in the case of something like a bug being found.
     * If a message is sent through the Firebase console, all other non-essential cards will be hidden.
     * A message will be displayed when the returned calue of the message does NOT equal
     * (or NOT end with)'inop'.
     */
    private void remoteConfig() {
        FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(30)
                .build();
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        firebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);

        firebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        task.getResult();
                        String message = firebaseRemoteConfig.getString("remote_message");
                        String formattedMessage = message;
                        try {
                            Properties properties = new Properties();
                            properties.load(new StringReader("key = " + message));
                            formattedMessage = properties.getProperty("key");
                        } catch (IOException e) {
                            Log.e(TAG, "remoteConfig: Couldn't format returned string, falling back!", e);
                        }
                        Log.d(TAG, "remoteConfig message: " + message);
                        if (!message.endsWith("inop")) {
                            welcomeCard.setVisibility(View.GONE);
                            updateCard.setVisibility(View.GONE);
                            remoteCard.setVisibility(View.VISIBLE);
                            ((TextView) remoteCard.findViewById(R.id.remoteTextView)).setText(formattedMessage);
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Firebase remoteConfig couldn't be fetched!",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Checks for available updates using the Google Play App-Updates Library.
     * If there are updates available, we will prompt the user to update using an additional cardview in the main layout.
     * When the user clicks on this CardView, they will be sent to the Google Play page to manually update.
     */
    private void checkForAppUpdates() {
        AppUpdateManager updateManager = AppUpdateManagerFactory.create(this);
        Task<AppUpdateInfo> appUpdateInfoTask = updateManager.getAppUpdateInfo();

        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            Log.d(TAG, "checkForAppUpdates: " + appUpdateInfo.updateAvailability());
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                updateCard.setVisibility(View.VISIBLE);
                updateCard.setOnClickListener(v -> {
                    Bundle params = new Bundle();
                    params.putString("inapp_update_triggered", "1");
                    analytics.logEvent("inapp_update_triggered", params);
                    final String appPackageName = getPackageName();
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                    updateCard.setVisibility(View.GONE);
                });
            }
        });

        appUpdateInfoTask.addOnFailureListener(this, e -> Log.e(TAG, "onFailure: ", e));
    }

    /**
     * We're animating the background train images as well as hinting to the user that the infoCard can be dismissed.
     * Animations keep things nice & dynamic.
     */
    private void performAnimations() {
        ViewGroup rootView = findViewById(R.id.parentView);
        StartupBackgroundAnimator startupBackgroundAnimator = new StartupBackgroundAnimator(rootView, MainActivity.this);
        ContinuousBackgroundAnimator continuousBackgroundAnimator = new ContinuousBackgroundAnimator(rootView, MainActivity.this);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager.isPowerSaveMode() || !startupBackgroundAnimator.isFinished()) {
            continuousBackgroundAnimator.setStaticLocations();
            return;
        }

        startupBackgroundAnimator.startAnimations();
        continuousBackgroundAnimator.startAnimations(true);
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
            shiftsFileContentView.setVisibility(View.VISIBLE);
            iCalContentView.setVisibility(View.VISIBLE);
        } else {
            devHint.setVisibility(View.GONE);
            shiftsFileContentView.setVisibility(View.GONE);
            iCalContentView.setVisibility(View.GONE);
        }

        SharedPreferences prefs = getSharedPreferences(getString(R.string.sharedPrefs), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("DevMode", check);
        editor.apply();
        Toast.makeText(MainActivity.this, check ? "You're now a developer!" : "You're not a developer anymore!", Toast.LENGTH_SHORT).show();
    }

    /**
     * Turns on or off developer mode. In Devmode you get additional fields with the raw data of both
     * the original file & the iCal output. This one doesn't display a Toast.
     *
     * @param check whether DevMode should be on or off
     */
    private void setDevWithoutToast(boolean check) {
        isDev = check;
        toolbar.getMenu().getItem(0).setChecked(check);
        toolbar.getMenu().getItem(0).setVisible(check);
        if (check) {
            devHint.setVisibility(View.VISIBLE);
            shiftsFileContentView.setVisibility(View.VISIBLE);
            iCalContentView.setVisibility(View.VISIBLE);
            findViewById(R.id.scrollViewMain).setKeepScreenOn(true);
        } else {
            devHint.setVisibility(View.GONE);
            shiftsFileContentView.setVisibility(View.GONE);
            iCalContentView.setVisibility(View.GONE);
            findViewById(R.id.scrollViewMain).setKeepScreenOn(false);
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
            Logger.debug(TAG, "File retrieved, loading.. " + intent);
            Uri fileUri = intent.getData();
            handleFileIntent(fileUri);
        } else if (requestCode == UPDATE_REQUEST_CODE && resultCode != RESULT_OK) {
            Toast.makeText(this, "Update mislukt of geannuleerd!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Starts reading the file & checks whether it's valid.
     *
     * @param uri URI that points to the file
     */
    @SuppressLint("SetTextI18n")
    private void handleFileIntent(Uri uri) {
        shiftsFileReader.resetData();
        shiftsFileReader.startConversion(this, uri, returnDaysOff, returnOnlyVTA);
        shiftsFileContentView.setText(ShiftsFileReader.fullFileString());
        iCalContentView.setText(shiftsFileReader.getCalendarICS());

        if (!ShiftsFileReader.dw.isEmpty()) {
            if (ShiftsFileReader.weekNumber > 54)
                loadedSuccess.setText("Jaar " + ShiftsFileReader.yearNumber + " geladen!");
            else
                loadedSuccess.setText("Week " + ShiftsFileReader.weekNumber + " van jaar " + ShiftsFileReader.yearNumber + " geladen!");
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
                    File file = new File(Objects.requireNonNull(getExternalFilesDir(null)).getPath() + "/converted.ics"); // Null -> temp location
                    FileOutputStream out = new FileOutputStream(file);
                    OutputStreamWriter writer = new OutputStreamWriter(out);

                    writer.write(shiftsFileReader.getCalendarICS());
                    writer.close();
                    out.close();

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri uri1 = FileProvider.getUriForFile(MainActivity.this, getApplicationContext().getPackageName() + ".provider", file);
                    intent.setDataAndType(uri1, "text/calendar");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    //Analytics
                    if(!isDev) {
                        Bundle params = new Bundle();
                        params.putString("converted_dws", "1");
                        analytics.logEvent("converted_dws", params);

                        if(showProfession.isChecked()) {
                            Bundle bundle2 = new Bundle();
                            bundle2.putString("options_withprofession", "1");
                            analytics.logEvent("options_withprofession", bundle2);
                        }
                        if(showModifiers.isChecked()) {
                            Bundle bundle2 = new Bundle();
                            bundle2.putString("options_additionalsymbols", "1");
                            analytics.logEvent("options_additionalsymbols", bundle2);
                        }
                        if(daysOffSwitch.isChecked()) {
                            Bundle bundle2 = new Bundle();
                            bundle2.putString("options_withdaysoff", "1");
                            analytics.logEvent("options_withdaysoff", bundle2);
                        }
                        if(onlyVTA.isChecked()) {
                            Bundle bundle2 = new Bundle();
                            bundle2.putString("options_withdaysoff_onlyvta", "1");
                            analytics.logEvent("options_withdaysoff_onlyvta", bundle2);
                        }

                        SharedPreferences prefs = getSharedPreferences(getString(R.string.sharedPrefs), Context.MODE_PRIVATE);
                        if(prefs.getBoolean("fullDaysOnly", false)) {
                            Bundle bundle2 = new Bundle();
                            bundle2.putString("options_entiredays", "1");
                            analytics.logEvent("options_entiredays", bundle2);
                        }
                        if(!prefs.getString("toIgnore", "").isBlank()) {
                            Bundle bundle2 = new Bundle();
                            bundle2.putString("options_ignoreshift", "1");
                            analytics.logEvent("options_ignoreshift", bundle2);
                        }
                        if(!prefs.getString("prefix", "").isBlank()) {
                            Bundle bundle2 = new Bundle();
                            bundle2.putString("options_prefix", "1");
                            analytics.logEvent("options_prefix", bundle2);
                        }
                        if(!prefs.getString("replacement", "").isBlank()) {
                            Bundle bundle2 = new Bundle();
                            bundle2.putString("options_replacetitle", "1");
                            analytics.logEvent("options_replacetitle", bundle2);
                        }
                    }

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
     * The user might want to add their days off as individual calendar items to their calendar.
     * This is where those options are defined.
     *
     * @param daysOff whether we should return days off as calendar items.
     * @param onlyVTA whether we should ONLY return VTA components (VL, CF etc.) as calendar items instead of regular days off (R, -, WV, etc.)
     */
    public void setShouldReturnDaysOff(boolean daysOff, boolean onlyVTA) {
        returnDaysOff = daysOff;
        returnOnlyVTA = onlyVTA;
    }
}