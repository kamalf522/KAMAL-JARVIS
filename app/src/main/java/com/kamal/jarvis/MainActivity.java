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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createVoice();
        createInterface();
    }

    // =========================
    // VOICE
    // =========================

    private void createVoice() {

        jarvisVoice = new TextToSpeech(this, status -> {

            if (status == TextToSpeech.SUCCESS) {

                int result = jarvisVoice.setLanguage(
                        new Locale("ar", "MA")
                );

                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {

                    jarvisVoice.setLanguage(
                            Locale.getDefault()
                    );
                }

                jarvisVoice.setSpeechRate(0.95f);
            }
        });
    }

    // =========================
    // INTERFACE
    // =========================

    private void createInterface() {

        LinearLayout main = new LinearLayout(this);

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

        // TITLE

        TextView title = new TextView(this);

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

        // STATUS

        TextView status = new TextView(this);

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

        // OUTPUT

        output = new TextView(this);

        output.setText(
                "JARVIS:\n\n" +
                "مرحبا كمال.\n\n" +
                "أنا JARVIS.\n" +
                "جاهز لاستقبال أوامرك."
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

        ScrollView scroll = new ScrollView(this);

        scroll.addView(output);

        // INPUT

        commandInput = new EditText(this);

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

        // EXECUTE BUTTON

        Button executeButton = new Button(this);

        executeButton.setText(
                "تنفيذ الأمر"
        );

        executeButton.setOnClickListener(v -> {

            String command =
                    commandInput
                            .getText()
                            .toString()
                            .trim();

            if (!command.isEmpty()) {

                executeCommand(command);
            }

        });

        // VOICE BUTTON

        Button voiceButton = new Button(this);

        voiceButton.setText(
                "🎙 التحدث مع JARVIS"
        );

        voiceButton.setOnClickListener(v ->
                startVoiceRecognition()
        );

        // ADD VIEWS

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

    // =========================
    // COMMAND SYSTEM
    // =========================

    private void executeCommand(String command) {

        String lower =
                command
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .trim();

        addMessage(
                "أنت:\n" + command
        );

        // GREETING

        if (containsAny(
                lower,
                "مرحبا",
                "سلام",
                "السلام عليكم",
                "hello",
                "hi"
        )) {

            respond(
                    "مرحبا كمال. أنا JARVIS، جاهز لخدمتك."
            );
        }

        // TIME

        else if (containsAny(
                lower,
                "الوقت",
                "ساعة",
                "شحال فالوقت",
                "time"
        )) {

            SimpleDateFormat format =
                    new SimpleDateFormat(
                            "HH:mm",
                            Locale.getDefault()
                    );

            String time =
                    format.format(
                            new Date()
                    );

            respond(
                    "الوقت الآن هو " + time
            );
        }

        // DATE

        else if (containsAny(
                lower,
                "التاريخ",
                "اليوم",
                "شنو النهار",
                "