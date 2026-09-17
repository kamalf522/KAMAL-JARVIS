package com.kamal.jarvis;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity
        implements TextToSpeech.OnInitListener {

    private static final int REQUEST_AUDIO = 1001;
    private static final int REQUEST_VOICE = 1002;
    private static final int REQUEST_NOTIFICATIONS = 1003;

    private TextToSpeech textToSpeech;

    private CommandRouter commandRouter;

    private TextView statusText;
    private TextView chatText;
    private EditText inputText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        commandRouter =
                new CommandRouter(this);

        textToSpeech =
                new TextToSpeech(
                        this,
                        this
                );

        createInterface();

        requestRequiredPermissions();

        startJarvisBackgroundService();
    }

    // =========================================================
    // PERMISSIONS
    // =========================================================

    private void requestRequiredPermissions() {

        if (Build.VERSION.SDK_INT >= 33) {

            if (checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.POST_NOTIFICATIONS
                        },
                        REQUEST_NOTIFICATIONS
                );

                return;
            }
        }

        requestAudioPermission();
    }

    private void requestAudioPermission() {

        if (Build.VERSION.SDK_INT < 23) {
            return;
        }

        if (checkSelfPermission(
                Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.RECORD_AUDIO
                    },
                    REQUEST_AUDIO
            );
        }
    }

    // =========================================================
    // JARVIS BACKGROUND SERVICE
    // =========================================================

    private void startJarvisBackgroundService() {

        try {

            Intent serviceIntent =
                    new Intent(
                            this,
                            JarvisBackgroundService.class
                    );

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.O) {

                startForegroundService(
                        serviceIntent
                );

            } else {

                startService(
                        serviceIntent
                );
            }

        } catch (Exception e) {

            if (statusText != null) {

                statusText.setText(
                        "● JARVIS ONLINE — BACKGROUND ERROR"
                );
            }
        }
    }

    // =========================================================
    // USER INTERFACE
    // =========================================================

    private void createInterface() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                24,
                24,
                24,
                24
        );

        // -----------------------------------------------------
        // TITLE
        // -----------------------------------------------------

        TextView title =
                new TextView(this);

        title.setText(
                "KAMAL JARVIS"
        );

        title.setTextSize(28);

        title.setGravity(
                Gravity.CENTER
        );

        title.setPadding(
                0,
                10,
                0,
                10
        );

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        // -----------------------------------------------------
        // STATUS
        // -----------------------------------------------------

        statusText =
                new TextView(this);

        statusText.setText(
                "● JARVIS ONLINE"
        );

        statusText.setTextSize(16);

        statusText.setGravity(
                Gravity.CENTER
        );

        statusText.setPadding(
                0,
                5,
                0,
                15
        );

        root.addView(
                statusText
        );

        // -----------------------------------------------------
        // CHAT AREA
        // -----------------------------------------------------

        chatText =
                new TextView(this);

        chatText.setText(
                "JARVIS: مرحبا كمال.\n" +
                "أنا جاهز لتنفيذ أوامرك.\n" +
                "النظام الخلفي يعمل عند تشغيل JARVIS.\n\n"
        );

        chatText.setTextSize(17);

        chatText.setPadding(
                12,
                12,
                12,
                12
        );

        ScrollView scrollView =
                new ScrollView(this);

        scrollView.addView(
                chatText
        );

        LinearLayout.LayoutParams chatParams =
                new LinearLayout.LayoutParams(
                        -1,
                        0
                );

        chatParams.weight = 1;

        root.addView(
                scrollView,
                chatParams
        );

        // -----------------------------------------------------
        // INPUT
        // -----------------------------------------------------

        inputText =
                new EditText(this);

        inputText.setHint(
                "اكتب أمرك هنا..."
        );

        inputText.setTextSize(16);

        inputText.setSingleLine(false);

        root.addView(
                inputText,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        // -----------------------------------------------------
        // EXECUTE BUTTON
        // -----------------------------------------------------

        Button executeButton =
                new Button(this);

        executeButton.setText(
                "تنفيذ الأمر"
        );

        executeButton.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        executeCommand();
                    }
                }
        );

        root.addView(
                executeButton
        );

        // -----------------------------------------------------
        // VOICE BUTTON
        // -----------------------------------------------------

        Button voiceButton =
                new Button(this);

        voiceButton.setText(
                "🎙 التحدث مع JARVIS"
        );

        voiceButton.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        startVoiceRecognition();
                    }
                }
        );

        root.addView(
                voiceButton
        );

        // -----------------------------------------------------
        // BACKGROUND STATUS BUTTON
        // -----------------------------------------------------

        Button backgroundButton =
                new Button(this);

        backgroundButton.setText(
                "حالة النظام الخلفي"
        );

        backgroundButton.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        showBackgroundStatus();
                    }
                }
        );

        root.addView(
                backgroundButton
        );

        setContentView(root);
    }

    // =========================================================
    // COMMAND EXECUTION
    // =========================================================

    private void executeCommand() {

        String command =
                inputText.getText()
                        .toString()
                        .trim();

        if (command.isEmpty()) {

            speak(
                    "قول لي الأمر الذي تريد تنفيذه."
            );

            return;
        }

        addMessage(
                "كمال: " + command
        );

        inputText.setText("");

        statusText.setText(
                "● JARVIS PROCESSING..."
        );

        String response =
                commandRouter.execute(
                        command
                );

        if (response == null) {

            response =
                    "حدث خطأ غير معروف.";
        }

        if (response.equals(
                "__STOP_SPEAKING__"
        )) {

            stopSpeaking();

            statusText.setText(
                    "● JARVIS ONLINE"
            );

            return;
        }

        addMessage(
                "JARVIS: " + response
        );

        speak(response);

        statusText.setText(
                "● JARVIS ONLINE"
        );
    }

    // =========================================================
    // BACKGROUND STATUS
    // =========================================================

    private void showBackgroundStatus() {

        if (JarvisBackgroundService
                .isServiceRunning()) {

            JarvisBackgroundService service =
                    JarvisBackgroundService
                            .getInstance();

            if (service != null) {

                String status =
                        service.getStatus();

                addMessage(
                        "JARVIS: " + status
                );

                speak(status);

                return;
            }
        }

        String message =
                "JARVIS BACKGROUND SERVICE\n\n" +
                "الحالة: OFFLINE\n\n" +
                "جاري تشغيل الخدمة...";

        addMessage(
                "JARVIS: " + message
        );

        speak(message);

        startJarvisBackgroundService();
    }

    // =========================================================
    // VOICE RECOGNITION
    // =========================================================

    private void startVoiceRecognition() {

        if (Build.VERSION.SDK_INT >= 23) {

            if (checkSelfPermission(
                    Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.RECORD_AUDIO
                        },
                        REQUEST_AUDIO
                );

                return;
            }
        }

        try {

            Intent intent =
                    new Intent(
                            RecognizerIntent
                                    .ACTION_RECOGNIZE_SPEECH
                    );

            intent.putExtra(
                    RecognizerIntent
                            .EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent
                            .LANGUAGE_MODEL_FREE_FORM
            );

            intent.putExtra(
                    RecognizerIntent
                            .EXTRA_LANGUAGE,
                    "ar-MA"
            );

            intent.putExtra(
                    RecognizerIntent
                            .EXTRA_PROMPT,
                    "تكلم مع JARVIS"
            );

            startActivityForResult(
                    intent,
                    REQUEST_VOICE
            );

            statusText.setText(
                    "● JARVIS LISTENING..."
            );

        } catch (Exception e) {

            statusText.setText(
                    "● JARVIS ONLINE"
            );

            speak(
                    "تعذر تشغيل التعرف على الصوت."
            );
        }
    }

    // =========================================================
    // VOICE RESULT
    // =========================================================

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode != REQUEST_VOICE) {
            return;
        }

        statusText.setText(
                "● JARVIS ONLINE"
        );

        if (resultCode != RESULT_OK ||
                data == null) {

            speak(
                    "لم أسمع الأمر."
            );

            return;
        }

        ArrayList<String> results =
                data.getStringArrayListExtra(
                        RecognizerIntent
                                .EXTRA_RESULTS
                );

        if (results == null ||
                results.isEmpty()) {

            speak(
                    "لم أتعرف على الكلام."
            );

            return;
        }

        String command =
                results.get(0);

        inputText.setText(
                command
        );

        executeCommand();
    }

    // =========================================================
    // TEXT TO SPEECH
    // =========================================================

    @Override
    public void onInit(int status) {

        if (status ==
                TextToSpeech.SUCCESS) {

            int result =
                    textToSpeech.setLanguage(
                            new Locale(
                                    "ar",
                                    "MA"
                            )
                    );

            if (result ==
                    TextToSpeech.LANG_MISSING_DATA ||
                    result ==
                    TextToSpeech.LANG_NOT_SUPPORTED) {

                textToSpeech.setLanguage(
                        new Locale("ar")
                );
            }

            textToSpeech.setSpeechRate(
                    0.95f
            );
        }
    }

    private void speak(String text) {

        if (text == null ||
                text.trim().isEmpty()) {

            return;
        }

        if (textToSpeech == null) {
            return;
        }

        textToSpeech.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "JARVIS_RESPONSE"
        );
    }

    private void stopSpeaking() {

        if (textToSpeech != null &&
                textToSpeech.isSpeaking()) {

            textToSpeech.stop();
        }
    }

    // =========================================================
    // CHAT
    // =========================================================

    private void addMessage(String message) {

        if (chatText == null) {
            return;
        }

        chatText.append(
                "\n" + message + "\n"
        );
    }

    // =========================================================
    // PERMISSION RESULT
    // =========================================================

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode ==
                REQUEST_NOTIFICATIONS) {

            requestAudioPermission();

            return;
        }

        if (requestCode ==
                REQUEST_AUDIO) {

            if (grantResults.length > 0 &&
                    grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED) {

                statusText.setText(
                        "● JARVIS ONLINE"
                );

            } else {

                statusText.setText(
                        "● JARVIS ONLINE — الصوت غير مفعل"
                );
            }
        }
    }

    // =========================================================
    // ACTIVITY LIFECYCLE
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();

        if (JarvisBackgroundService
                .isServiceRunning()) {

            statusText.setText(
                    "● JARVIS ONLINE"
            );
        }
    }

    @Override
    protected void onDestroy() {

        stopSpeaking();

        if (textToSpeech != null) {

            textToSpeech.shutdown();

            textToSpeech = null;
        }

        super.onDestroy();
    }
}