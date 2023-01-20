package com.treinchauffeur.mijndw;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.treinchauffeur.mijndw.io.DWReader;

import java.io.File;

public class MainActivity extends AppCompatActivity {
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
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");  // specify any file type
                startActivityForResult(intent, PICK_FILE_REQUEST);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "File retrieved, loading.. "+data.getData().getPath());

        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data.getData() != null) {
            Uri fileUri = data.getData();
            DWReader.startConversion(new File(fileUri.getPath()));
        }
    }
}