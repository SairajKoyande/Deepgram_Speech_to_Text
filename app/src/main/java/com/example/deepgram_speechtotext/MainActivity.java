package com.example.deepgram_speechtotext;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final String DEEPGRAM_API_KEY = "6502fb733d1b4d58d05402b43423b350ba2c6a03"; // Replace with your API key

    private TextView textViewResult;
    private TextView textViewInterim;
    private Button buttonRecord;
    private Button buttonReset;
    private ProgressBar progressConnecting;
    private DeepgramClient deepgramClient;
    private boolean isRecording = false;
    private boolean permissionGranted = false;
    private boolean isConnecting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewResult = findViewById(R.id.textViewResult);
        textViewInterim = findViewById(R.id.textViewInterim);
        buttonRecord = findViewById(R.id.buttonRecord);
        buttonReset = findViewById(R.id.buttonReset);
        progressConnecting = findViewById(R.id.progressConnecting);
        progressConnecting.setVisibility(View.GONE);

        // Check if permission is already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            permissionGranted = true;
        } else {
            // Request necessary permissions
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }

        // Initialize Deepgram client
        deepgramClient = new DeepgramClient(DEEPGRAM_API_KEY, new DeepgramClient.TextResultListener() {
            @Override
            public void onTextResult(String text, boolean isFinal) {
                runOnUiThread(() -> {
                    if (isFinal) {
                        textViewResult.setText(text);
                        textViewInterim.setText("");

                        // Auto-scroll to the bottom for final results
                        final ScrollView scrollView = findViewById(R.id.scrollView);
                        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
                    } else {
                        // Show interim results in a different area
                        textViewInterim.setText(text);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Deepgram error: " + error);

                    // Reset UI if there was an error while connecting
                    if (isConnecting) {
                        isConnecting = false;
                        progressConnecting.setVisibility(View.GONE);
                        buttonRecord.setText("Start Recording");
                        buttonRecord.setEnabled(true);
                    }
                });
            }

            @Override
            public void onConnectionStatusChanged(boolean connected) {
                runOnUiThread(() -> {
                    if (connected) {
                        isConnecting = false;
                        progressConnecting.setVisibility(View.GONE);

                        // Now that we're connected, start recording
                        deepgramClient.startRecording();
                        isRecording = true;
                        buttonRecord.setText("Stop Recording");
                        buttonRecord.setEnabled(true);

                        Toast.makeText(MainActivity.this, "Connected to Deepgram", Toast.LENGTH_SHORT).show();
                    } else if (isRecording) {
                        // Connection was lost while recording
                        Toast.makeText(MainActivity.this, "Connection lost", Toast.LENGTH_SHORT).show();
                        stopRecording();
                    }
                });
            }
        });

        buttonRecord.setOnClickListener(v -> {
            if (!permissionGranted) {
                Toast.makeText(this, "Recording permission is required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isRecording) {
                stopRecording();
            } else {
                startRecording();
            }
        });

        buttonReset.setOnClickListener(v -> {
            textViewResult.setText("");
            textViewInterim.setText("");
            if (deepgramClient != null) {
                deepgramClient.resetTranscript();
            }
        });
    }

    private void startRecording() {
        buttonRecord.setEnabled(false);
        isConnecting = true;
        progressConnecting.setVisibility(View.VISIBLE);
        buttonRecord.setText("Connecting...");

        // Just establish connection - recording will start in the callback
        deepgramClient.connect();
    }

    private void stopRecording() {
        deepgramClient.stopRecording();
        deepgramClient.disconnect();
        isRecording = false;
        buttonRecord.setText("Start Recording");
        textViewInterim.setText("");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            permissionGranted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
    }

    @Override
    protected void onDestroy() {
        if (isRecording) {
            stopRecording();
        }
        super.onDestroy();
    }
}