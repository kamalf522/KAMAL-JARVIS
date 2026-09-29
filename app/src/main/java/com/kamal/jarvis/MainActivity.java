package com.kamal.jarvis;

import android.Manifest;
import android.app.Activity;
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
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.drawable.GradientDrawable;

import com.kamal.jarvis.v2.JarvisSystem;
import com.kamal.jarvis.v2.intelligence.JarvisBrain;

import java.util.ArrayList;
import java.util.Locale;

public final class MainActivity extends Activity
        implements TextToSpeech.OnInitListener {

    private static final int REQUEST_AUDIO = 2001;
    private static final int REQUEST_VOICE = 2002;
    private static final int REQUEST_NOTIFICATIONS = 2003;

    private JarvisSystem jarvisSystem;
    private TextToSpeech textToSpeech;

    private TextView statusText;
    private TextView coreStateText;
    private TextView chatText;
    private EditText inputText;
    private ScrollView chatScroll;
    private View statusDot;

    private boolean ttsReady;
    private boolean commandRunning;

    private final int BG = Color.rgb(5, 9, 18);
    private final int PANEL = Color.rgb(11, 18, 32);
    private final int PANEL_2 = Color.rgb(15, 25, 43);
    private final int CYAN = Color.rgb(35, 210, 255);
    private final int WHITE = Color.rgb(235, 244, 255);
    private final int MUTED = Color.rgb(139, 158, 183);
    private final int GREEN = Color.rgb(70, 235, 155);
    private final int RED = Color.rgb(255, 90, 100);
    private final int YELLOW = Color.rgb(255, 205, 70);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(BG);
        window.setNavigationBarColor(BG);

        createInterface();

        textToSpeech =
                new TextToSpeech(
                        this,
                        this
                );

        initializeJarvis();

        requestRequiredPermissions();

        handleIncomingIntent(getIntent());
    }

    private void initializeJarvis() {

        try {

            jarvisSystem =
                    new JarvisSystem(this);

            new Thread(() -> {

                final com.kamal.jarvis.v2.core.JarvisResult<Boolean>
                        result =
                        jarvisSystem.start();

                runOnUiThread(() -> {

                    if (result != null &&
                            result.isSuccess()) {

                        setOnlineState();

                        appendChat(
                                "JARVIS: النظام الأساسي اشتغل بنجاح."
                        );

                    } else {

                        setErrorState();

                        String message =
                                result == null
                                        ? "Startup returned no result."
                                        : result.getMessage();

                        appendChat(
                                "JARVIS: فشل تشغيل النظام: "
                                        + safe(message)
                        );
                    }
                });

            }).start();

        } catch (Exception exception) {

            jarvisSystem = null;

            setErrorState();

            appendChat(
                    "JARVIS: فشل تهيئة النظام."
            );
        }
    }

    private void createInterface() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                dp(16),
                dp(14),
                dp(16),
                dp(14)
        );

        root.setBackgroundColor(BG);

        LinearLayout header =
                new LinearLayout(this);

        header.setOrientation(
                LinearLayout.HORIZONTAL
        );

        header.setGravity(
                Gravity.CENTER_VERTICAL
        );

        LinearLayout titleBox =
                new LinearLayout(this);

        titleBox.setOrientation(
                LinearLayout.VERTICAL
        );

        TextView title =
                makeText(
                        "KAMAL",
                        27,
                        WHITE,
                        Typeface.BOLD
                );

        TextView subtitle =
                makeText(
                        "J A R V I S  •  V2 SYSTEM",
                        9,
                        MUTED,
                        Typeface.BOLD
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

        LinearLayout statusBox =
                new LinearLayout(this);

        statusBox.setOrientation(
                LinearLayout.HORIZONTAL
        );

        statusBox.setGravity(
                Gravity.CENTER_VERTICAL
        );

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
                        YELLOW,
                        50,
                        YELLOW,
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

        statusText =
                makeText(
                        "STARTING",
                        10,
                        YELLOW,
                        Typeface.BOLD
                );

        LinearLayout.LayoutParams statusParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        statusParams.leftMargin = dp(6);

        statusBox.addView(
                statusText,
                statusParams
        );

        header.addView(statusBox);

        root.addView(header);

        addSpace(root, 16);

        LinearLayout coreCard =
                new LinearLayout(this);

        coreCard.setOrientation(
                LinearLayout.VERTICAL
        );

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

        coreCard.addView(
                makeText(
                        "JARVIS CORE",
                        11,
                        CYAN,
                        Typeface.BOLD
                )
        );

        addSpace(coreCard, 6);

        coreStateText =
                makeText(
                        "INITIALIZING...",
                        20,
                        WHITE,
                        Typeface.BOLD
                );

        coreCard.addView(coreStateText);

        addSpace(coreCard, 4);

        coreCard.addView(
                makeText(
                        "Brain • Evolution • Runtime • Security • Tools",
                        10,
                        MUTED,
                        Typeface.NORMAL
                )
        );

        root.addView(coreCard);

        addSpace(root, 14);

        LinearLayout sessionCard =
                new LinearLayout(this);

        sessionCard.setOrientation(
                LinearLayout.VERTICAL
        );

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

        sessionCard.addView(
                makeText(
                        "LIVE SESSION",
                        10,
                        CYAN,
                        Typeface.BOLD
                )
        );

        addSpace(sessionCard, 8);

        chatScroll =
                new ScrollView(this);

        chatText =
                makeText(
                        "JARVIS: كنوجد النظام...",
                        14,
                        WHITE,
                        Typeface.NORMAL
                );

        chatText.setGravity(
                Gravity.RIGHT | Gravity.TOP
        );

        chatText.setLineSpacing(
                0,
                1.15f
        );

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
                        dp(160)
                )
        );

        sessionCard.addView(
                chatScroll,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(160)
                )
        );

        root.addView(sessionCard);

        addSpace(root, 12);

        LinearLayout inputRow =
                new LinearLayout(this);

        inputRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        inputText =
                new EditText(this);

        inputText.setSingleLine(false);
        inputText.setTextColor(WHITE);
        inputText.setHintTextColor(MUTED);
        inputText.setHint(
                "كتب الأمر ديالك هنا..."
        );

        inputText.setTextSize(14);

        inputText.setPadding(
                dp(14),
                dp(10),
                dp(14),
                dp(10)
        );

        inputText.setBackground(
                roundedBackground(
                        PANEL_2,
                        18,
                        Color.rgb(30, 74, 98),
                        1
                )
        );

        inputRow.addView(
                inputText,
                new LinearLayout.LayoutParams(
                        0,
                        dp(52),
                        1
                )
        );

        Button sendButton =
                createButton(
                        "SEND",
                        CYAN
                );

        LinearLayout.LayoutParams sendParams =
                new LinearLayout.LayoutParams(
                        dp(82),
                        dp(52)
                );

        sendParams.leftMargin = dp(8);

        inputRow.addView(
                sendButton,
                sendParams
        );

        sendButton.setOnClickListener(
                v -> executeTypedCommand()
        );

        root.addView(inputRow);

        addSpace(root, 8);

        Button voiceButton =
                createButton(
                        "🎙  TALK TO JARVIS",
                        GREEN
                );

        root.addView(
                voiceButton,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(50)
                )
        );

        voiceButton.setOnClickListener(
                v -> startVoiceRecognition()
        );

        addSpace(root, 8);

        LinearLayout quickRow =
                new LinearLayout(this);

        quickRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        Button statusButton =
                createButton(
                        "STATUS",
                        CYAN
                );

        Button stopButton =
                createButton(
                        "STOP",
                        RED
                );

        quickRow.addView(
                statusButton,
                new LinearLayout.LayoutParams(
                        0,
                        dp(44),
                        1
                )
        );

        LinearLayout.LayoutParams stopParams =
                new LinearLayout.LayoutParams(
                        0,
                        dp(44),
                        1
                );

        stopParams.leftMargin = dp(8);

        quickRow.addView(
                stopButton,
                stopParams
        );

        root.addView(quickRow);

        statusButton.setOnClickListener(
                v -> showStatus()
        );

        stopButton.setOnClickListener(
                v -> stopJarvis()
        );

        setContentView(root);
    }

    private void executeTypedCommand() {

        if (inputText == null) {
            return;
        }

        String command =
                inputText.getText()
                        .toString()
                        .trim();

        if (command.isEmpty()) {
            return;
        }

        inputText.setText("");

        hideKeyboard();

        executeCommand(command);
    }

    private void executeCommand(
            String command
    ) {

        if (commandRunning) {
            return;
        }

        if (jarvisSystem == null) {

            appendChat(
                    "JARVIS: النظام مازال ما تهيأش."
            );

            return;
        }

        commandRunning = true;

        setBusyState();

        appendChat(
                "YOU: " + command
        );

        new Thread(() -> {

            String response;

            try {

                com.kamal.jarvis.v2.core.JarvisResult<
                        JarvisBrain.BrainResponse
                        > result =
                        jarvisSystem.processCommand(
                                command
                        );

                response =
                        buildBrainResponse(
                                result
                        );

            } catch (Exception exception) {

                response =
                        "وقع خطأ حقيقي أثناء تنفيذ الأمر: "
                                + safe(
                                exception.getMessage()
                        );
            }

            final String finalResponse =
                    response;

            runOnUiThread(() -> {

                appendChat(
                        "JARVIS: "
                                + finalResponse
                );

                speak(finalResponse);

                commandRunning = false;

                if (jarvisSystem != null &&
                        jarvisSystem.isInitialized()) {

                    setOnlineState();

                } else {

                    setErrorState();
                }
            });

        }).start();
    }

    private String buildBrainResponse(
            com.kamal.jarvis.v2.core.JarvisResult<
                    JarvisBrain.BrainResponse
                    > result
    ) {

        if (result == null) {
            return "النظام رجع بدون نتيجة.";
        }

        if (!result.isSuccess()) {

            return "الأمر ما تنفذش: "
                    + safe(result.getMessage());
        }

        JarvisBrain.BrainResponse response =
                result.getData();

        if (response == null) {

            return safe(result.getMessage());
        }

        String message =
                response.getMessage();

        if (message != null &&
                !message.trim().isEmpty()) {

            return message;
        }

        return safe(result.getMessage());
    }

    private void showStatus() {

        if (jarvisSystem == null) {

            appendChat(
                    "JARVIS: النظام غير مهيأ."
            );

            return;
        }

        JarvisSystem.SystemStatus status =
                jarvisSystem.getStatus();

        String message =
                "SYSTEM STATUS\n"
                        + "Initialized: "
                        + status.isInitialized()
                        + "\nSecurity: "
                        + status.isSecurityActive()
                        + "\nRuntime: "
                        + status.isRuntimeRunning()
                        + "\nWorkspace: "
                        + status.isProjectWorkspaceReady()
                        + "\nTools: "
                        + status.getToolCount()
                        + "\nPermissions: "
                        + status.getGrantedPermissionCount()
                        + "\nEvolution: "
                        + status.getEvolutionState();

        appendChat(
                "JARVIS:\n" + message
        );
    }

    private void stopJarvis() {

        if (jarvisSystem == null) {
            return;
        }

        new Thread(() -> {

            com.kamal.jarvis.v2.core.JarvisResult<Boolean>
                    result =
                    jarvisSystem.stop();

            runOnUiThread(() -> {

                if (result != null &&
                        result.isSuccess()) {

                    coreStateText.setText(
                            "SYSTEM STOPPED"
                    );

                    statusText.setText(
                            "OFFLINE"
                    );

                    statusText.setTextColor(
                            RED
                    );

                    statusDot.setBackground(
                            roundedBackground(
                                    RED,
                                    50,
                                    RED,
                                    0
                            )
                    );

                    appendChat(
                            "JARVIS: تم إيقاف النظام."
                    );

                } else {

                    appendChat(
                            "JARVIS: فشل إيقاف النظام."
                    );
                }
            });

        }).start();
    }

    private void setOnlineState() {

        if (coreStateText == null) {
            return;
        }

        coreStateText.setText(
                "SYSTEM ONLINE"
        );

        coreStateText.setTextColor(
                WHITE
        );

        statusText.setText(
                "ONLINE"
        );

        statusText.setTextColor(
                GREEN
        );

        statusDot.setBackground(
                roundedBackground(
                        GREEN,
                        50,
                        GREEN,
                        0
                )
        );
    }

    private void setBusyState() {

        coreStateText.setText(
                "PROCESSING..."
        );

        coreStateText.setTextColor(
                YELLOW
        );

        statusText.setText(
                "BUSY"
        );

        statusText.setTextColor(
                YELLOW
        );

        statusDot.setBackground(
                roundedBackground(
                        YELLOW,
                        50,
                        YELLOW,
                        0
                )
        );
    }

    private void setErrorState() {

        if (coreStateText == null) {
            return;
        }

        coreStateText.setText(
                "SYSTEM ERROR"
        );

        coreStateText.setTextColor(
                RED
        );

        statusText.setText(
                "ERROR"
        );

        statusText.setTextColor(
                RED
        );

        statusDot.setBackground(
                roundedBackground(
                        RED,
                        50,
                        RED,
                        0
                )
        );
    }

    private void startVoiceRecognition() {

        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(
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
                    "قول الأمر ديالك..."
            );

            startActivityForResult(
                    intent,
                    REQUEST_VOICE
            );

        } catch (Exception exception) {

            appendChat(
                    "JARVIS: التعرف على الصوت غير متاح."
            );
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

        if (requestCode != REQUEST_VOICE ||
                resultCode != RESULT_OK ||
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

        String command =
                results.get(0);

        if (command != null &&
                !command.trim().isEmpty()) {

            executeCommand(
                    command.trim()
            );
        }
    }

    @Override
    public void onInit(int status) {

        if (status ==
                TextToSpeech.SUCCESS) {

            int languageResult =
                    textToSpeech.setLanguage(
                            new Locale(
                                    "ar",
                                    "MA"
                            )
                    );

            ttsReady =
                    languageResult !=
                            TextToSpeech.LANG_MISSING_DATA
                            &&
                            languageResult !=
                                    TextToSpeech.LANG_NOT_SUPPORTED;

        } else {

            ttsReady = false;
        }
    }

    private void speak(
            String text
    ) {

        if (!ttsReady ||
                textToSpeech == null ||
                text == null ||
                text.trim().isEmpty()) {
            return;
        }

        try {

            textToSpeech.speak(
                    text,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "JARVIS_RESPONSE"
            );

        } catch (Exception ignored) {
        }
    }

    private void requestRequiredPermissions() {

        ArrayList<String> permissions =
                new ArrayList<>();

        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(
                        Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED) {

            permissions.add(
                    Manifest.permission.RECORD_AUDIO
            );
        }

        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(
                        Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {

            permissions.add(
                    Manifest.permission.POST_NOTIFICATIONS
            );
        }

        if (!permissions.isEmpty()) {

            requestPermissions(
                    permissions.toArray(
                            new String[0]
                    ),
                    REQUEST_AUDIO
            );
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

        if (jarvisSystem != null) {

            jarvisSystem.synchronizePermissions();
        }

        if (requestCode ==
                REQUEST_AUDIO) {

            if (Build.VERSION.SDK_INT >= 33 &&
                    checkSelfPermission(
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
    }

    private void appendChat(
            String message
    ) {

        if (chatText == null) {
            return;
        }

        String old =
                chatText.getText()
                        .toString();

        if (old.trim().isEmpty()) {

            chatText.setText(
                    message
            );

        } else {

            chatText.setText(
                    old
                            + "\n\n"
                            + message
            );
        }

        if (chatScroll != null) {

            chatScroll.post(
                    () -> chatScroll.fullScroll(
                            View.FOCUS_DOWN
                    )
            );
        }
    }

    private TextView makeText(
            String text,
            float size,
            int color,
            int typefaceStyle
    ) {

        TextView view =
                new TextView(this);

        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);

        view.setTypeface(
                Typeface.create(
                        "sans-serif",
                        typefaceStyle
                )
        );

        return view;
    }

    private Button createButton(
            String text,
            int color
    ) {

        Button button =
                new Button(this);

        button.setText(text);
        button.setTextColor(BG);
        button.setTextSize(11);
        button.setAllCaps(false);

        button.setTypeface(
                Typeface.create(
                        "sans-serif",
                        Typeface.BOLD
                )
        );

        button.setGravity(
                Gravity.CENTER
        );

        button.setBackground(
                roundedBackground(
                        color,
                        16,
                        color,
                        0
                )
        );

        return button;
    }

    private GradientDrawable roundedBackground(
            int color,
            float radius,
            int strokeColor,
            int strokeWidth
    ) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(color);
        drawable.setCornerRadius(
                dp(radius)
        );

        if (strokeWidth > 0) {

            drawable.setStroke(
                    dp(strokeWidth),
                    strokeColor
            );
        }

        return drawable;
    }

    private void addSpace(
            LinearLayout parent,
            int height
    ) {

        View space =
                new View(this);

        parent.addView(
                space,
                new LinearLayout.LayoutParams(
                        1,
                        dp(height)
                )
        );
    }

    private int dp(float value) {

        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
                        + 0.5f
        );
    }

    private String safe(
            String value
    ) {

        if (value == null ||
                value.trim().isEmpty()) {

            return "unknown";
        }

        return value;
    }

    private void hideKeyboard() {

        try {

            InputMethodManager manager =
                    (InputMethodManager)
                            getSystemService(
                                    Context.INPUT_METHOD_SERVICE
                            );

            if (manager != null &&
                    inputText != null) {

                manager.hideSoftInputFromWindow(
                        inputText.getWindowToken(),
                        0
                );
            }

        } catch (Exception ignored) {
        }
    }

    private void handleIncomingIntent(
            Intent intent
    ) {

        if (intent == null) {
            return;
        }

        String command =
                intent.getStringExtra(
                        "command"
                );

        if (command != null &&
                !command.trim().isEmpty()) {

            executeCommand(
                    command.trim()
            );
        }
    }

    @Override
    protected void onNewIntent(
            Intent intent
    ) {

        super.onNewIntent(intent);

        setIntent(intent);

        handleIncomingIntent(intent);
    }

    @Override
    protected void onDestroy() {

        try {

            if (jarvisSystem != null) {
                jarvisSystem.stop();
            }

        } catch (Exception ignored) {
        }

        try {

            if (textToSpeech != null) {

                textToSpeech.stop();
                textToSpeech.shutdown();

                textToSpeech = null;
            }

        } catch (Exception ignored) {
        }

        super.onDestroy();
    }
}