package com.treinchauffeur.mijndw;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.treinchauffeur.mijndw.io.DWReader;

public class MainActivity extends Activity {
    public static final int PICK_FILE_REQUEST = 1312;
    public static final String TAG = "MainActivity";

    DWReader dwReader;

    Button convertBtn, btnLoadFile;
    EditText dwContent;
    TextView loadedStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dwContent = findViewById(R.id.dwContent);
        convertBtn = findViewById(R.id.btnConvertFile);
        btnLoadFile = findViewById(R.id.btnLoadFile);
        loadedStatus = findViewById(R.id.loadedStatus);

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
            btnLoadFile.setText("Load another file");
            dwContent.setVisibility(View.VISIBLE);
            convertBtn.setVisibility(View.VISIBLE);
            dwContent.setText(dwReader.fullFileString());

            convertBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //TODO convert to ICAL using biweekly
                }
            });
        }
    }
}