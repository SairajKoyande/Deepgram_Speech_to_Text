🗣️ Real-Time Speech-to-Text Android App (Deepgram API)
This Android application leverages the Deepgram Speech-to-Text API to provide real-time voice transcription with support for interim (partial) results. Built using Java, XML, and Deepgram's WebSocket interface, this app allows users to speak into the device and see the transcription instantly as they talk.

✨ Features
🎤 Real-time speech-to-text transcription
🔄 Supports interim (partial) results for immediate feedback
🌐 Uses Deepgram’s WebSocket API for low-latency streaming
📱 Clean and minimal UI
💬 Displays final and partial transcriptions in real time
📦 Lightweight and fast performance

🧰 Tech Stack
Technology	Description
Java	Core programming language
XML	UI layout design
WebSocket (OkHttp)	For real-time audio streaming
Deepgram API	Speech-to-Text service with real-time transcription
Android Studio	Development IDE

🚀 How It Works
1. 🎤 Audio Capture
App uses AudioRecord to capture raw audio from the microphone.
Audio is encoded in 16-bit Linear PCM (16kHz) format for Deepgram compatibility.

2. 🌐 WebSocket C
onnection to Deepgram
App establishes a secure WebSocket connection to Deepgram’s streaming API:

wss://api.deepgram.com/v1/listen?punctuate=true&interim_results=true

3. 📡 Audio Streaming
Captured audio chunks are streamed via WebSocket in real-time.

4. 🧠 Real-Time Transcription
Deepgram returns both:
Interim results – Partial transcriptions (while speaking)
Final results – Accurate, punctuated results (after a pause)

5. 📱 UI Update
The results are parsed from JSON and displayed on the UI instantly.

https://github.com/user-attachments/assets/1ea93134-6b64-4fab-9a9a-b4ac0a88cbfc
