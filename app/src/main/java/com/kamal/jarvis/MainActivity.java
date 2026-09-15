package com.kamal.jarvis;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int VOICE_REQUEST = 1001;

    private TextView output;
    private EditText commandInput;
    private TextToSpeech jarvisVoice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createVoice();
        createInterface();
    }

    private void createVoice() {

        jarvisVoice = new TextToSpeech(this, status -> {

            if (status == TextToSpeech.SUCCESS) {
                jarvisVoice.setLanguage(Locale.getDefault());
                jarvisVoice.setSpeechRate(0.95f);
            }

        });
    }

    private void createInterface() {

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(30, 40, 30, 25);
        main.setBackgroundColor(Color.rgb(7, 11, 22));

        TextView title = new TextView(this);
        title.setText("KAMAL JARVIS");
        title.setTextColor(Color.WHITE);
        title.setTextSize(29);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 10, 0, 10);

        TextView status = new TextView(this);
        status.setText("● JARVIS ONLINE");
        status.setTextColor(Color.rgb(80, 255, 140));
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 5, 0, 20);

        output = new TextView(this);
        output.setText(
                "JARVIS:\n" +
                "مرحبا كمال.\n\n" +
                "أنا جاهز لاستقبال أوامرك."
        );
        output.setTextColor(Color.WHITE);
        output.setTextSize(17);
        output.setPadding(20, 20, 20, 20);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(output);

        commandInput = new EditText(this);
        commandInput.setHint("اكتب أمرك هنا...");
        commandInput.setHintTextColor(Color.GRAY);
        commandInput.setTextColor(Color.WHITE);
        commandInput.setTextSize(16);
        commandInput.setSingleLine(true);

        Button executeButton = new Button(this);
        executeButton.setText("تنفيذ الأمر");

        executeButton.setOnClickListener(v -> {

            String command = commandInput
                    .getText()
                    .toString()
                    .trim();

            if (!command.isEmpty()) {
                executeCommand(command);
            }

        });

        Button voiceButton = new Button(this);
        voiceButton.setText("🎙 التحدث مع JARVIS");

        voiceButton.setOnClickListener(v -> startVoiceRecognition());

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

    private void executeCommand(String command) {

        String lower = command.toLowerCase(Locale.ROOT);

        addMessage("أنت:\n" + command);

        if (lower.contains("مرحبا")
                || lower.contains("سلام")
                || lower.contains("hello")) {

            respond("مرحبا كمال. أنا JARVIS، جاهز لخدمتك.");

        }

        else if (lower.contains("الوقت")
                || lower.contains("ساعة")
                || lower.contains("time")) {

            java.text.SimpleDateFormat format =
                    new java.text.SimpleDateFormat(
                            "HH:mm",
                            Locale.getDefault()
                    );

            String time = format.format(
                    new java.util.Date()
            );

            respond("الوقت الآن هو " + time);

        }

        else if (lower.contains("يوتيوب")
                || lower.contains("youtube")) {

            respond("سأفتح يوتيوب.");

            try {

                Intent intent = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.youtube.com")
                );

                startActivity(intent);

            } catch (Exception e) {

                respond("تعذر فتح يوتيوب.");

            }

        }

        else if (lower.contains("جوجل")
                || lower.contains("google")) {

            respond("سأفتح جوجل.");

            try {

                Intent intent = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com")
                );

                startActivity(intent);

            } catch (Exception e) {

                respond("تعذر فتح جوجل.");

            }

        }

        else if (lower.contains("افتح الهاتف")
                || lower.contains("الهاتف")
                || lower.contains("phone")) {

            respond("سأحاول فتح تطبيق الهاتف.");

            try {

                Intent intent = new Intent(
                        Intent.ACTION_DIAL
                );

                startActivity(intent);

            } catch (Exception e) {

                respond("تعذر فتح الهاتف.");

            }

        }

        else if (lower.contains("افتح الإعدادات")
                || lower.contains("الإعدادات")
                || lower.contains("settings")) {

            respond("سأفتح إعدادات الهاتف.");

            try {

                Intent intent = new Intent(
                        android.provider.Settings.ACTION_SETTINGS
                );

                startActivity(intent);

            } catch (Exception e) {

                respond("تعذر فتح الإعدادات.");

            }

        }

        else if (lower.contains("توقف")
                || lower.contains("اسكت")
                || lower.contains("stop")) {

            respond("حسناً.");

        }

        else {

            respond(
                    "وصلني أمرك، لكن هذه المهارة غير مضافة لي بعد.\n\n" +
                    "سنضيف لها مهارات حقيقية في المراحل القادمة."
            );
        }

        commandInput.setText("");
    }

    private void startVoiceRecognition() {

        Intent intent = new Intent(
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
                    "التعرف على الصوت غير متوفر في الهاتف",
                    Toast.LENGTH_LONG
            ).show();

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

                String command = results.get(0);

                commandInput.setText(command);

                executeCommand(command);
            }
        }
    }

    private void respond(String message) {

        addMessage("JARVIS:\n" + message);

        if (jarvisVoice != null) {

            jarvisVoice.speak(
                    message,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "JARVIS_RESPONSE"
            );
        }
    }

    private void addMessage(String message) {

        String current = output.getText().toString();

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