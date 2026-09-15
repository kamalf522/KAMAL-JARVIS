package com.kamal.jarvis;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int VOICE_REQUEST = 1001;
    private static final int MICROPHONE_PERMISSION = 2001;

    private TextView output;
    private EditText commandInput;
    private TextToSpeech jarvisVoice;

    private MemoryManager memory;
    private CommandRouter router;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        memory = new MemoryManager(this);

        router = new CommandRouter(
                this,
                memory,
                new CommandRouter.ResponseListener() {
                    @Override
                    public void onResponse(String message) {
                        respond(message);
                    }
                }
        );

        createVoice();
        createInterface();
    }

    private void createVoice() {

        jarvisVoice = new TextToSpeech(
                this,
                status -> {

                    if (status == TextToSpeech.SUCCESS) {

                        int result =
                                jarvisVoice.setLanguage(
                                        new Locale("ar", "MA")
                                );

                        if (result ==
                                TextToSpeech.LANG_MISSING_DATA
                                || result ==
                                TextToSpeech.LANG_NOT_SUPPORTED) {

                            jarvisVoice.setLanguage(
                                    Locale.getDefault()
                            );
                        }

                        jarvisVoice.setSpeechRate(0.95f);
                    }
                }
        );
    }

    private void createInterface() {

        LinearLayout main =
                new LinearLayout(this);

        main.setOrientation(
                LinearLayout.VERTICAL
        );

        main.setPadding(
                30,
                40,
                30,
                25
        );

        main.setBackgroundColor(
                Color.rgb(7, 11, 22)
        );

        TextView title =
                new TextView(this);

        title.setText(
                "KAMAL JARVIS"
        );

        title.setTextColor(
                Color.WHITE
        );

        title.setTextSize(29);

        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        title.setGravity(
                Gravity.CENTER
        );

        title.setPadding(
                0,
                10,
                0,
                10
        );

        TextView status =
                new TextView(this);

        status.setText(
                "● JARVIS ONLINE"
        );

        status.setTextColor(
                Color.rgb(80, 255, 140)
        );

        status.setTextSize(16);

        status.setGravity(
                Gravity.CENTER
        );

        status.setPadding(
                0,
                5,
                0,
                20
        );

        output =
                new TextView(this);

        output.setText(
                "JARVIS:\n\n" +
                "مرحبا كمال.\n\n" +
                "أنا JARVIS.\n" +
                "نظام الذاكرة والمهارات جاهز."
        );

        output.setTextColor(
                Color.WHITE
        );

        output.setTextSize(17);

        output.setPadding(
                20,
                20,
                20,
                20
        );

        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(output);

        commandInput =
                new EditText(this);

        commandInput.setHint(
                "اكتب أمرك هنا..."
        );

        commandInput.setHintTextColor(
                Color.GRAY
        );

        commandInput.setTextColor(
                Color.WHITE
        );

        commandInput.setTextSize(16);

        commandInput.setSingleLine(true);

        commandInput.setOnEditorActionListener(
                (v, actionId, event) -> {

                    executeTypedCommand();

                    return true;
                }
        );

        Button executeButton =
                new Button(this);

        executeButton.setText(
                "تنفيذ الأمر"
        );

        executeButton.setOnClickListener(
                v -> executeTypedCommand()
        );

        Button voiceButton =
                new Button(this);

        voiceButton.setText(
                "🎙 التحدث مع JARVIS"
        );

        voiceButton.setOnClickListener(
                v -> startVoiceRecognition()
        );

        main.addView(title);
        main.addView(status);

        main.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        main.addView(commandInput);
        main.addView(executeButton);
        main.addView(voiceButton);

        setContentView(main);
    }

    private void executeTypedCommand() {

        String command =
                commandInput
                        .getText()
                        .toString()
                        .trim();

        if (command.isEmpty()) {
            return;
        }

        executeCommand(command);
    }

    private void executeCommand(
            String command
    ) {

        addMessage(
                "أنت:\n" + command
        );

        router.execute(command);

        commandInput.setText("");
    }

    private void startVoiceRecognition() {

        if (android.os.Build.VERSION.SDK_INT >= 23) {

            if (checkSelfPermission(
                    Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.RECORD_AUDIO
                        },
                        MICROPHONE_PERMISSION
                );

                return;
            }
        }

        openVoiceRecognizer();
    }

    private void openVoiceRecognizer() {

        Intent intent =
                new Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "ar-MA"
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "تكلم مع JARVIS..."
        );

        try {

            startActivityForResult(
                    intent,
                    VOICE_REQUEST
            );

        } catch (ActivityNotFoundException e) {

            Toast.makeText(
                    this,
                    "التعرف على الصوت غير متوفر.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

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
                MICROPHONE_PERMISSION) {

            if (grantResults.length > 0
                    && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(
                        this,
                        "تم السماح بالميكروفون.",
                        Toast.LENGTH_SHORT
                ).show();

                openVoiceRecognizer();

            } else {

                Toast.makeText(
                        this,
                        "خاصك تسمح لـ JARVIS بالميكروفون.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

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

        if (requestCode == VOICE_REQUEST
                && resultCode == RESULT_OK
                && data != null) {

            ArrayList<String> results =
                    data.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS
                    );

            if (results != null
                    && !results.isEmpty()) {

                String command =
                        results.get(0);

                commandInput.setText(
                        command
                );

                executeCommand(command);
            }
        }
    }

    private void respond(
            String message
    ) {

        addMessage(
                "JARVIS:\n" + message
        );

        if (jarvisVoice != null) {

            jarvisVoice.speak(
                    message,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "JARVIS_RESPONSE"
            );
        }
    }

    private void addMessage(
            String message
    ) {

        String current =
                output
                        .getText()
                        .toString();

        output.setText(
                current +
                "\n\n--------------------\n\n" +
                message
        );
    }

    @Override
    protected void onDestroy() {

        if (jarvisVoice != null) {

            jarvisVoice.stop();
            jarvisVoice.shutdown();
        }

        super.onDestroy();
    }
}