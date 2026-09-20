package com.kamal.jarvis;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.drawable.GradientDrawable;

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
    private TextView coreStateText;
    private TextView chatText;
    private EditText inputText;
    private ScrollView chatScroll;
    private View statusDot;

    private final int BG = Color.rgb(5, 9, 18);
    private final int PANEL = Color.rgb(11, 18, 32);
    private final int PANEL_2 = Color.rgb(15, 25, 43);
    private final int CYAN = Color.rgb(35, 210, 255);
    private final int WHITE = Color.rgb(235, 244, 255);
    private final int MUTED = Color.rgb(139, 158, 183);
    private final int GREEN = Color.rgb(70, 235, 155);
    private final int RED = Color.rgb(255, 90, 100);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(BG);
        window.setNavigationBarColor(BG);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(0);
        }

        commandRouter = new CommandRouter(this);
        textToSpeech = new TextToSpeech(this, this);

        createInterface();
        requestRequiredPermissions();
        startJarvisBackgroundService();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable roundedBackground(
            int color,
            float radius,
            int strokeColor,
            int strokeWidth) {

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));

        if (strokeWidth > 0) {
            drawable.setStroke(dp(strokeWidth), strokeColor);
        }

        return drawable;
    }

    private TextView makeText(
            String text,
            float size,
            int color,
            Typeface typeface) {

        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(typeface);
        return view;
    }

    private void addSpace(LinearLayout parent, int height) {
        View space = new View(this);

        parent.addView(
                space,
                new LinearLayout.LayoutParams(
                        1,
                        dp(height)
                )
        );
    }

    private void createInterface() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(
                dp(16),
                dp(14),
                dp(16),
                dp(14)
        );
        root.setBackgroundColor(BG);

        // =========================
        // HEADER
        // =========================

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);

        TextView title = makeText(
                "KAMAL",
                27,
                WHITE,
                Typeface.create("sans-serif", Typeface.BOLD)
        );

        TextView subtitle = makeText(
                "J A R V I S  •  PERSONAL SYSTEM",
                9,
                MUTED,
                Typeface.create("sans-serif", Typeface.BOLD)
        );

        titleBox.addView(title);
        titleBox.addView(subtitle);

        header.addView(
                titleBox,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        LinearLayout statusBox = new LinearLayout(this);
        statusBox.setOrientation(LinearLayout.HORIZONTAL);
        statusBox.setGravity(Gravity.CENTER_VERTICAL);
        statusBox.setPadding(
                dp(10),
                dp(7),
                dp(10),
                dp(7)
        );
        statusBox.setBackground(
                roundedBackground(
                        Color.rgb(9, 27, 31),
                        30,
                        Color.rgb(25, 91, 105),
                        1
                )
        );

        statusDot = new View(this);
        statusDot.setBackground(
                roundedBackground(
                        GREEN,
                        50,
                        GREEN,
                        0
                )
        );

        statusBox.addView(
                statusDot,
                new LinearLayout.LayoutParams(
                        dp(8),
                        dp(8)
                )
        );

        statusText = makeText(
                "ONLINE",
                10,
                GREEN,
                Typeface.create("sans-serif", Typeface.BOLD)
        );

        LinearLayout.LayoutParams statusTextParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        statusTextParams.leftMargin = dp(6);

        statusBox.addView(
                statusText,
                statusTextParams
        );

        header.addView(statusBox);

        root.addView(
                header,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        addSpace(root, 16);

        // =========================
        // CORE CARD
        // =========================

        LinearLayout coreCard = new LinearLayout(this);
        coreCard.setOrientation(LinearLayout.VERTICAL);
        coreCard.setPadding(
                dp(16),
                dp(14),
                dp(16),
                dp(14)
        );

        coreCard.setBackground(
                roundedBackground(
                        PANEL,
                        20,
                        Color.rgb(25, 54, 75),
                        1
                )
        );

        TextView coreTitle = makeText(
                "JARVIS CORE",
                11,
                CYAN,
                Typeface.create("sans-serif", Typeface.BOLD)
        );

        coreCard.addView(coreTitle);

        addSpace(coreCard, 6);

        coreStateText = makeText(
                "SYSTEM READY",
                20,
                WHITE,
                Typeface.create("sans-serif", Typeface.BOLD)
        );

        coreCard.addView(coreStateText);

        addSpace(coreCard, 4);

        TextView coreDescription = makeText(
                "Command engine • Voice • Automation • Intelligence",
                10,
                MUTED,
                Typeface.create("sans-serif", Typeface.NORMAL)
        );

        coreCard.addView(coreDescription);

        root.addView(
                coreCard,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        addSpace(root, 14);

        // =========================
        // LIVE SESSION
        // =========================

        LinearLayout sessionCard = new LinearLayout(this);
        sessionCard.setOrientation(LinearLayout.VERTICAL);
        sessionCard.setPadding(
                dp(14),
                dp(12),
                dp(14),
                dp(12)
        );

        sessionCard.setBackground(
                roundedBackground(
                        PANEL,
                        20,
                        Color.rgb(25, 54, 75),
                        1
                )
        );

        TextView sessionTitle = makeText(
                "LIVE SESSION",
                10,
                CYAN,
                Typeface.create("sans-serif", Typeface.BOLD)
        );

        sessionCard.addView(sessionTitle);

        addSpace(sessionCard, 8);

        chatScroll = new ScrollView(this);
        chatScroll.setFillViewport(false);
        chatScroll.setBackgroundColor(Color.TRANSPARENT);

        chatText = makeText(
                "JARVIS: مرحبا كمال.\nأنا جاهز لاستقبال أوامرك.",
                14,
                WHITE,
                Typeface.create("sans-serif", Typeface.NORMAL)
        );

        chatText.setGravity(Gravity.RIGHT | Gravity.TOP);
        chatText.setLineSpacing(0, 1.15f);
        chatText.setPadding(
                dp(10),
                dp(8),
                dp(10),
                dp(8)
        );

        chatScroll.addView(
                chatText,
                new ScrollView.LayoutParams(
                        ScrollView.LayoutParams.MATCH_PARENT,
                        ScrollView.LayoutParams.WRAP_CONTENT
                )
        );

        sessionCard.addView(
                chatScroll,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(190)
                )
        );

        root.addView(
                sessionCard,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        addSpace(root, 12);

        // =========================
        // INPUT
        // =========================

        inputText = new EditText(this);
        inputText.setTextSize(14);
        inputText.setTextColor(WHITE);
        inputText.setHintTextColor(MUTED);
        inputText.setHint("اكتب أمرك لـ JARVIS...");
        inputText.setSingleLine(true);
        inputText.setPadding(
                dp(14),
                dp(12),
                dp(14),
                dp(12)
        );

        inputText.setBackground(
                roundedBackground(
                        PANEL_2,
                        18,
                        Color.rgb(30, 68, 92),
                        1
                )
        );

        root.addView(
                inputText,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(52)
                )
        );

        addSpace(root, 8);

        // =========================
        // ACTION BUTTONS
        // =========================

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button executeButton = new Button(this);
        executeButton.setText("⚡ تنفيذ");
        executeButton.setTextSize(13);
        executeButton.setTextColor(Color.WHITE);
        executeButton.setAllCaps(false);
        executeButton.setTypeface(
                Typeface.create("sans-serif", Typeface.BOLD)
        );
        executeButton.setBackground(
                roundedBackground(
                        Color.rgb(8, 88, 116),
                        18,
                        CYAN,
                        1
                )
        );

        executeButton.setOnClickListener(
                v -> executeCommand()
        );

        Button voiceButton = new Button(this);
        voiceButton.setText("🎙 صوت");
        voiceButton.setTextSize(13);
        voiceButton.setTextColor(WHITE);
        voiceButton.setAllCaps(false);
        voiceButton.setTypeface(
                Typeface.create("sans-serif", Typeface.BOLD)
        );
        voiceButton.setBackground(
                roundedBackground(
                        PANEL_2,
                        18,
                        Color.rgb(45, 72, 96),
                        1
                )
        );

        voiceButton.setOnClickListener(
                v -> startVoiceRecognition()
        );

        LinearLayout.LayoutParams executeParams =
                new LinearLayout.LayoutParams(
                        0,
                        dp(50),
                        1
                );

        executeParams.rightMargin = dp(5);

        LinearLayout.LayoutParams voiceParams =
                new LinearLayout.LayoutParams(
                        0,
                        dp(50),
                        1
                );

        voiceParams.leftMargin = dp(5);

        actions.addView(executeButton, executeParams);
        actions.addView(voiceButton, voiceParams);

        root.addView(actions);

        addSpace(root, 8);

        // =========================
        // SYSTEM BUTTONS
        // =========================

        LinearLayout secondaryActions = new LinearLayout(this);
        secondaryActions.setOrientation(LinearLayout.HORIZONTAL);

        Button systemButton = new Button(this);
        systemButton.setText("حالة النظام");
        systemButton.setTextSize(11);
        systemButton.setTextColor(MUTED);
        systemButton.setAllCaps(false);
        systemButton.setBackground(
                roundedBackground(
                        Color.TRANSPARENT,
                        16,
                        Color.rgb(35, 55, 75),
                        1
                )
        );

        systemButton.setOnClickListener(
                v -> showSystemStatus()
        );

        Button clearButton = new Button(this);
        clearButton.setText("مسح المحادثة");
        clearButton.setTextSize(11);
        clearButton.setTextColor(MUTED);
        clearButton.setAllCaps(false);
        clearButton.setBackground(
                roundedBackground(
                        Color.TRANSPARENT,
                        16,
                        Color.rgb(35, 55, 75),
                        1
                )
        );

        clearButton.setOnClickListener(
                v -> clearChat()
        );

        LinearLayout.LayoutParams secondaryParams =
                new LinearLayout.LayoutParams(
                        0,
                        dp(42),
                        1
                );

        secondaryParams.rightMargin = dp(4);

        LinearLayout.LayoutParams secondaryParams2 =
                new LinearLayout.LayoutParams(
                        0,
                        dp(42),
                        1
                );

        secondaryParams2.leftMargin = dp(4);

        secondaryActions.addView(
                systemButton,
                secondaryParams
        );

        secondaryActions.addView(
                clearButton,
                secondaryParams2
        );

        root.addView(secondaryActions);

        setContentView(root);

        startStatusAnimation();
    }

    private void startStatusAnimation() {

        if (statusDot == null) {
            return;
        }

        AlphaAnimation animation =
                new AlphaAnimation(0.35f, 1.0f);

        animation.setDuration(900);
        animation.setRepeatMode(
                AlphaAnimation.REVERSE
        );
        animation.setRepeatCount(
                AlphaAnimation.INFINITE
        );

        statusDot.startAnimation(animation);
    }

    private void executeCommand() {

        String command = inputText.getText()
                .toString()
                .trim();

        if (command.isEmpty()) {
            return;
        }

        appendChat("أنت: " + command);

        inputText.setText("");

        hideKeyboard();

        setProcessingState();

        new Thread(() -> {

            final String response;

            try {
                response = commandRouter.execute(command);
            } catch (Exception e) {

                runOnUiThread(() -> {

                    setAttentionState();

                    appendChat(
                            "JARVIS: وقع خطأ أثناء تنفيذ الأمر."
                    );

                    speak(
                            "وقع خطأ أثناء تنفيذ الأمر"
                    );
                });

                return;
            }

            runOnUiThread(() -> {

                if (response == null ||
                        response.trim().isEmpty()) {

                    setAttentionState();

                    appendChat(
                            "JARVIS: لم أحصل على نتيجة من النظام."
                    );

                    speak(
                            "لم أحصل على نتيجة من النظام"
                    );

                    return;
                }

                if (response.equals(
                        "__STOP_SPEAKING__")) {

                    stopSpeaking();

                    setOnlineState();

                    appendChat(
                            "JARVIS: تم إيقاف الصوت."
                    );

                    return;
                }

                appendChat(
                        "JARVIS: " + response
                );

                if (looksLikeFailure(response)) {
                    setAttentionState();
                } else {
                    setOnlineState();
                }

                speak(response);
            });

        }).start();
    }

    private boolean looksLikeFailure(String response) {

        if (response == null) {
            return true;
        }

        String text = response.toLowerCase(Locale.ROOT);

        return text.contains("خطأ")
                || text.contains("فشل")
                || text.contains("لا أستطيع")
                || text.contains("لم أستطع")
                || text.contains("غير متاح")
                || text.contains("لم يتم")
                || text.contains("error")
                || text.contains("failed");
    }

    private void setProcessingState() {

        statusText.setText("PROCESSING");
        statusText.setTextColor(CYAN);

        coreStateText.setText("PROCESSING COMMAND");
        coreStateText.setTextColor(CYAN);
    }

    private void setOnlineState() {

        statusText.setText("ONLINE");
        statusText.setTextColor(GREEN);

        coreStateText.setText("SYSTEM READY");
        coreStateText.setTextColor(WHITE);
    }

    private void setAttentionState() {

        statusText.setText("ATTENTION");
        statusText.setTextColor(RED);

        coreStateText.setText("ACTION NEEDS CHECK");
        coreStateText.setTextColor(RED);
    }

    private void appendChat(String message) {

        if (chatText == null) {
            return;
        }

        String current = chatText.getText().toString();

        if (!current.isEmpty()) {
            current += "\n\n";
        }

        current += message;

        chatText.setText(current);

        if (chatScroll != null) {

            chatScroll.post(
                    () -> chatScroll.fullScroll(
                            View.FOCUS_DOWN
                    )
            );
        }
    }

    private void clearChat() {

        chatText.setText(
                "JARVIS: تم تنظيف جلسة المحادثة.\nجاهز لأوامرك."
        );

        setOnlineState();
    }

    private void showSystemStatus() {

        String status;

        try {
            status = commandRouter.execute(
                    "حالة النظام"
            );
        } catch (Exception e) {
            status = "تعذر الحصول على حالة النظام.";
        }

        appendChat(
                "JARVIS: " + status
        );

        speak(status);
    }

    private void startVoiceRecognition() {

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

        try {

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
                    "تكلم مع JARVIS"
            );

            startActivityForResult(
                    intent,
                    REQUEST_VOICE
            );

        } catch (Exception e) {

            appendChat(
                    "JARVIS: التعرف الصوتي غير متوفر حاليا."
            );
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode != REQUEST_VOICE) {
            return;
        }

        if (resultCode != RESULT_OK ||
                data == null) {

            return;
        }

        ArrayList<String> results =
                data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS
                );

        if (results == null ||
                results.isEmpty()) {

            return;
        }

        String command = results.get(0);

        inputText.setText(command);
        inputText.setSelection(
                inputText.length()
        );

        executeCommand();
    }

    private void speak(String text) {

        if (textToSpeech == null ||
                text == null ||
                text.trim().isEmpty()) {

            return;
        }

        String cleanText = text.trim();

        textToSpeech.speak(
                cleanText,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "JARVIS_RESPONSE"
        );
    }

    private void stopSpeaking() {

        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    @Override
    public void onInit(int status) {

        if (status ==
                TextToSpeech.SUCCESS) {

            int result =
                    textToSpeech.setLanguage(
                            new Locale("ar", "MA")
                    );

            if (result ==
                    TextToSpeech.LANG_MISSING_DATA
                    ||
                    result ==
                    TextToSpeech.LANG_NOT_SUPPORTED) {

                textToSpeech.setLanguage(
                        Locale.getDefault()
                );
            }

            textToSpeech.setSpeechRate(0.95f);
            textToSpeech.setPitch(0.95f);
        }
    }

    private void requestRequiredPermissions() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU) {

            if (checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.POST_NOTIFICATIONS
                        },
                        REQUEST_NOTIFICATIONS
                );
            }
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

        } catch (Exception ignored) {
        }
    }

    private void hideKeyboard() {

        try {

            InputMethodManager manager =
                    (InputMethodManager)
                            getSystemService(
                                    Context.INPUT_METHOD_SERVICE
                            );

            if (manager != null) {

                manager.hideSoftInputFromWindow(
                        inputText.getWindowToken(),
                        0
                );
            }

        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (statusText != null) {
            setOnlineState();
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