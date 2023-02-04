package com.treinchauffeur.mijndw;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.treinchauffeur.mijndw.io.DWReader;

import java.io.File;

public class MainActivity extends Activity {
    public static final int PICK_FILE_REQUEST = 1312;
    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnLoadFile = findViewById(R.id.btnLoadFile);
        btnLoadFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("text/*");
                startActivityForResult(intent, PICK_FILE_REQUEST);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Log.d(TAG, "File retrieved, loading.. " + data);
            Uri fileUri = data.getData();
            DWReader.startConversion(this, fileUri);
        }
    }
}