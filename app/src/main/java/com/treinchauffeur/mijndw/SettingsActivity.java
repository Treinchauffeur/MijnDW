package com.treinchauffeur.mijndw;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ScrollView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class SettingsActivity extends Activity {

    public static final String TAG = "SettingsActivity";
    private SharedPreferences prefs;
    private MaterialSwitch wholeDaysSwitch;
    private TextInputEditText toIgnore, prefix, replacement;
    private ScrollView scrollViewMain;
    private MaterialCardView card;

    /**
     * We have a seperate activity that's used for some more advanced settings.
     *
     * @param savedInstanceState honestly, I don't know what this is.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        prefs = getSharedPreferences(getString(R.string.sharedPrefs), Context.MODE_PRIVATE);

        card = findViewById(R.id.settingsCard);
        scrollViewMain = findViewById(R.id.scrollViewMain);

        toIgnore = findViewById(R.id.ignoreEditText);
        prefix = findViewById(R.id.prefixEditText);
        replacement = findViewById(R.id.replacementEditText);
        wholeDaysSwitch = findViewById(R.id.wholeDaysCheckBox);

        MaterialToolbar toolbar = findViewById(R.id.toolbarAbout);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        handleInterface();
        fetchSettings();
    }

    /**
     * Binds the actions to the buttons placed on the activity.
     */
    private void handleInterface() {
        wholeDaysSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("fullDaysOnly", compoundButton.isChecked());
            editor.apply();
        });

        toIgnore.setText(prefs.getString("toIgnore", ""));
        prefix.setText(prefs.getString("prefix", ""));
        replacement.setText(prefs.getString("replacement", ""));

        card.post(() -> {
            int baseHeight = card.getMeasuredHeight();
            int toScroll = 1000;

            prefix.setOnFocusChangeListener((v, hasFocus) -> {
                ViewGroup.LayoutParams params = card.getLayoutParams();
                if (hasFocus) {
                    params.height = baseHeight + toScroll;
                    card.setLayoutParams(params);
                    scrollViewMain.postDelayed(() -> scrollViewMain.smoothScrollTo(0,
                            scrollViewMain.getHeight()), 50);
                } else {
                    params.height = baseHeight;
                    card.setLayoutParams(params);
                }
            });

            replacement.setOnFocusChangeListener((v, hasFocus) -> {
                ViewGroup.LayoutParams params = card.getLayoutParams();
                if (hasFocus) {
                    params.height = baseHeight + toScroll;
                    card.setLayoutParams(params);
                    scrollViewMain.postDelayed(() -> scrollViewMain.smoothScrollTo(0,
                            scrollViewMain.getHeight()), 50);
                } else {
                    params.height = baseHeight;
                    card.setLayoutParams(params);
                }
            });

            replacement.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    if(!charSequence.toString().contains(";"))
                        replacement.setError("Geen puntkomma (;) gevonden. Tekst wordt genegeerd.");
                }

                @Override
                public void afterTextChanged(Editable editable) {}
            });

        });



    }

    /**
     * Will fetch the ready-known settings from sharedpreferences.
     */
    private void fetchSettings() {
        SharedPreferences.Editor editor = prefs.edit();
        if(!prefs.contains("fullDaysOnly")) {
            wholeDaysSwitch.setChecked(false);
            editor.putBoolean("fullDaysOnly", false);
            editor.apply();
        } else {
            wholeDaysSwitch.setChecked(prefs.getBoolean("fullDaysOnly", false));
        }

    }

    @Override
    public void onBackPressed() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("fullDaysOnly", wholeDaysSwitch.isChecked());
        editor.putString("toIgnore", Objects.requireNonNull(toIgnore.getText()).toString());
        editor.putString("prefix", Objects.requireNonNull(prefix.getText()).toString());
        editor.putString("replacement", Objects.requireNonNull(replacement.getText()).toString());

        editor.apply();
        super.onBackPressed();
    }
}