package com.example.deepgram_speechtotext;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class DeepgramClient {
    private static final String TAG = "DeepgramClient";
    private WebSocketClient mWebSocketClient;
    private final String apiKey;
    private final TextResultListener listener;
    private boolean isConnected = false;
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private Thread recordingThread;
    private StringBuilder fullTranscript = new StringBuilder();

    public interface TextResultListener {
        void onTextResult(String text, boolean isFinal);
        void onError(String error);
        void onConnectionStatusChanged(boolean connected);
    }

    public DeepgramClient(String apiKey, TextResultListener listener) {
        this.apiKey = apiKey;
        this.listener = listener;
    }

    public void connect() {
        try {
            // Improved URI with parameters for better accuracy
            URI uri = new URI("wss://api.deepgram.com/v1/listen" +
                    "?encoding=linear16" +
                    "&sample_rate=16000" +
                    "&channels=1" +
                    "&model=nova-3" +
                    "&language=en-US" +
                    "&smart_format=true" +
                    "&interim_results=true" +
                    "&utterance_end_ms=1000" +
                    "&endpointing=300" +
                    "&diarize=true");

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Token " + apiKey);

            mWebSocketClient = new WebSocketClient(uri, new Draft_6455(), headers, 30000) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Log.d(TAG, "Connection opened");
                    isConnected = true;
                    listener.onConnectionStatusChanged(true);
                }

                @Override
                public void onMessage(String message) {
                    try {
                        JSONObject jsonResponse = new JSONObject(message);

                        // Check if this is a final result
                        boolean isFinal = false;
                        if (jsonResponse.has("is_final")) {
                            isFinal = jsonResponse.getBoolean("is_final");
                        }

                        JSONObject channel = jsonResponse.getJSONObject("channel");
                        JSONArray alternatives = channel.getJSONArray("alternatives");

                        if (alternatives.length() > 0) {
                            JSONObject alternative = alternatives.getJSONObject(0);
                            String transcript = alternative.getString("transcript");

                            if (!transcript.isEmpty()) {
                                // Check for speaker information
                                String speakerInfo = "";
                                if (alternative.has("speaker") && !alternative.isNull("speaker")) {
                                    int speaker = alternative.getInt("speaker");
                                    speakerInfo = "Speaker " + speaker + ": ";
                                }

                                String formattedText = speakerInfo + transcript;

                                // Handle final transcripts differently
                                if (isFinal) {
                                    if (fullTranscript.length() > 0) {
                                        fullTranscript.append(" ");
                                    }
                                    fullTranscript.append(formattedText);
                                    listener.onTextResult(fullTranscript.toString(), true);
                                } else {
                                    // This is an interim result
                                    listener.onTextResult(formattedText, false);
                                }
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to parse response", e);
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.d(TAG, "Connection closed: " + reason);
                    isConnected = false;
                    listener.onConnectionStatusChanged(false);
                }

                @Override
                public void onError(Exception ex) {
                    Log.e(TAG, "WebSocket error", ex);
                    listener.onError("WebSocket error: " + ex.getMessage());
                    isConnected = false;
                    listener.onConnectionStatusChanged(false);
                }
            };
            mWebSocketClient.connect();
        } catch (URISyntaxException e) {
            Log.e(TAG, "Invalid URI", e);
            listener.onError("Invalid URI: " + e.getMessage());
        }
    }

    public void resetTranscript() {
        fullTranscript = new StringBuilder();
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void startRecording() {
        if (!isConnected) {
            listener.onError("WebSocket not connected");
            return;
        }

        if (isRecording) {
            return;
        }

        // Initialize AudioRecord
        int sampleRate = 16000;
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

        if (bufferSize != AudioRecord.ERROR_BAD_VALUE && bufferSize != AudioRecord.ERROR) {
            try {
                audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.VOICE_RECOGNITION, // Use VOICE_RECOGNITION for better quality
                        sampleRate,
                        channelConfig,
                        audioFormat,
                        bufferSize * 3 // Increase buffer size for better performance
                );

                if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                    audioRecord.startRecording();
                    isRecording = true;

                    recordingThread = new Thread(() -> {
                        byte[] buffer = new byte[bufferSize];
                        while (isRecording) {
                            int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                            if (bytesRead > 0 && isConnected) {
                                mWebSocketClient.send(buffer);
                            }
                        }
                    });
                    recordingThread.start();
                } else {
                    listener.onError("AudioRecord failed to initialize");
                }
            } catch (Exception e) {
                listener.onError("Error initializing audio recording: " + e.getMessage());
                Log.e(TAG, "Error initializing audio recording", e);
            }
        } else {
            listener.onError("Buffer size error");
        }
    }

    public void stopRecording() {
        isRecording = false;
        if (audioRecord != null) {
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                try {
                    audioRecord.stop();
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Error stopping AudioRecord", e);
                }
            }
            audioRecord.release();
            audioRecord = null;
        }
    }

    public void disconnect() {
        stopRecording();
        if (mWebSocketClient != null && isConnected) {
            mWebSocketClient.close();
            isConnected = false;
        }
    }
}