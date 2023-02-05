package com.treinchauffeur.mijndw;


import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.treinchauffeur.mijndw.io.DWReader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class MainActivity extends Activity {
    public static final int PICK_FILE_REQUEST = 1312;
    public static final String TAG = "MainActivity";
    ClipboardManager clipboard;

    DWReader dwReader;

    Button convertBtn, btnLoadFile, btnSaveFile;
    EditText dwContent, icsContent;
    TextView loadedStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Loading all the UI elements
        dwContent = findViewById(R.id.dwContent);
        convertBtn = findViewById(R.id.btnConvertFile);
        btnLoadFile = findViewById(R.id.btnLoadFile);
        loadedStatus = findViewById(R.id.loadedStatus);
        icsContent = findViewById(R.id.icsContent);
        btnSaveFile = findViewById(R.id.saveFile);

        dwReader = new DWReader(this);

        btnLoadFile.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/*");
            startActivityForResult(intent, PICK_FILE_REQUEST);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Log.d(TAG, "File retrieved, loading.. " + data);
            Uri fileUri = data.getData();
            dwReader.startConversion(this, fileUri);

            loadedStatus.setText("File loaded!");
            btnLoadFile.setText("Reset: Load another file");
            dwContent.setVisibility(View.VISIBLE);
            convertBtn.setVisibility(View.VISIBLE);
            dwContent.setText(dwReader.fullFileString());

            //Converts the loaded DW data to a iCalendar string
            convertBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    icsContent.setVisibility(View.VISIBLE);
                    btnSaveFile.setVisibility(View.VISIBLE);
                    icsContent.setText(dwReader.getCalendarICS());
                    ClipData clip = ClipData.newPlainText("DW calenderitems", dwReader.getCalendarICS());
                    clipboard.setPrimaryClip(clip);
                }
            });

            //Saves the file to a temporary location & offers it to the user using an intent.
            //Sends user to the Google Calendar app page on the play store if no calendar app is available.
            btnSaveFile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        File file = new File(getExternalFilesDir(null).getPath() + "/converted.ics");
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

        }
    }
}