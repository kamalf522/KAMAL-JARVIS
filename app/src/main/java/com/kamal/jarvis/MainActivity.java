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

    private boolean ttsReady = false;
    private boolean commandRunning = false;

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

        commandRouter = new CommandRouter(this);
        textToSpeech = new TextToSpeech(this, this);

        createInterface();
        requestRequiredPermissions();
        startJarvisBackgroundService();

        handleIncomingIntent(getIntent());
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

    private TextView makeText(
            String text,
            float size,
            int color,
            Typeface typeface
    ) {

        TextView view =
                new TextView(this);

        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(typeface);

        return view;
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

    // =========================================================
    // INTERFACE
    // =========================================================

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

        // =====================================================
        // HEADER
        // =====================================================

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
                        Typeface.create(
                                "sans-serif",
                                Typeface.BOLD
                        )
                );

        TextView subtitle =
                makeText(
                        "J A R V I S  •  PERSONAL SYSTEM",
                        9,
                        MUTED,
                        Typeface.create(
                                "sans-serif",
                                Typeface.BOLD
                        )
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

        statusDot =
                new View(this);

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

        statusText =
                makeText(
                        "ONLINE",
                        10,
                        GREEN,
                        Typeface.create(
                                "sans-serif",
                                Typeface.BOLD
                        )
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

        // =====================================================
        // CORE CARD
        // =====================================================

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

        TextView coreTitle =
                makeText(
                        "JARVIS CORE",
                        11,
                        CYAN,
                        Typeface.create(
                                "sans-serif",
                                Typeface.BOLD
                        )
                );

        coreCard.addView(coreTitle);

        addSpace(coreCard, 6);

        coreStateText =
                makeText(
                        "SYSTEM READY",
                        20,
                        WHITE,
                        Typeface.create(
                                "sans-serif",
                                Typeface.BOLD
                        )
                );

        coreCard.addView(coreStateText);

        addSpace(coreCard, 4);

        TextView description =
                makeText(
                        "Command • Voice • Automation • Intelligence • Learning",
                        10,
                        MUTED,
                        Typeface.create(
                                "sans-serif",
                                Typeface.NORMAL
                        )
                );

        coreCard.addView(description);

        root.addView(coreCard);

        addSpace(root, 14);

        // =====================================================
        // SESSION
        // =====================================================

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

        TextView sessionTitle =
                makeText(
                        "LIVE SESSION",
                        10,
                        CYAN,
                        Typeface.create(
                                "sans-serif",
                                Typeface.BOLD
                        )
                );

        sessionCard.addView(sessionTitle);

        addSpace(sessionCard, 8);

        chatScroll =
                new ScrollView(this);

        chatScroll.setFillViewport(false);

        chatText =
                makeText(
                        "JARVIS: مرحبا كمال.\nأنا جاهز لاستقبال أوامرك.",
                        14,
                        WHITE,
                        Typeface.create(
                                "sans-serif",
                                Typeface.NORMAL
                        )
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
                        ScrollView.LayoutParams.WRAP_CONTENT
                )
        );

        sessionCard.addView(