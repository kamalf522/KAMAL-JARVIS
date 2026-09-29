package com.kamal.jarvis.v2.tools;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.core.ToolContract;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JARVIS V2 - Android Intent Tool
 *
 * أداة حقيقية لتنفيذ مجموعة آمنة من Android Intents.
 *
 * مدعومة:
 * - فتح إعدادات الجهاز.
 * - فتح إعدادات التطبيق.
 * - فتح رابط HTTPS/HTTP.
 * - فتح صفحة معلومات التطبيق.
 *
 * لا تقوم بتجاوز صلاحيات Android ولا تنفذ Intents
 * غير المسموح بها من هذه الطبقة.
 */
public final class AndroidIntentTool implements ToolContract {

    public static final String TOOL_ID =
            "android.intent";

    private final Context context;

    public AndroidIntentTool(Context context) {

        if (context == null) {
            throw new IllegalArgumentException(
                    "Context cannot be null."
            );
        }

        this.context =
                context.getApplicationContext();
    }

    @Override
    public String getId() {
        return TOOL_ID;
    }

    @Override
    public String getName() {
        return "Android Intent";
    }

    @Override
    public String getDescription() {
        return "Executes safe Android system intents such as settings and web navigation.";
    }

    @Override
    public boolean isAvailable() {
        return context != null;
    }

    @Override
    public JarvisResult<ToolOutput> execute(
            ToolInput input
    ) {

        if (input == null) {
            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Tool input cannot be null."
            );
        }

        String action =
                normalize(input.getAction());

        String originalCommand =
                stringValue(
                        input.get("original_command")
                );

        try {

            /*
             * -------------------------------------------------
             * Settings
             * -------------------------------------------------
             */

            if (isSettingsAction(action, originalCommand)) {

                Intent intent =
                        new Intent(
                                Settings.ACTION_SETTINGS
                        );

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                context.startActivity(intent);

                return success(
                        action,
                        "Android Settings opened successfully."
                );
            }

            /*
             * -------------------------------------------------
             * App settings
             * -------------------------------------------------
             */

            if (containsAny(
                    action,
                    "app_settings",
                    "application_settings"
            )) {

                Intent intent =
                        new Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        );

                intent.setData(
                        Uri.parse(
                                "package:"
                                        + context.getPackageName()
                        )
                );

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                context.startActivity(intent);

                return success(
                        action,
                        "JARVIS application settings opened successfully."
                );
            }

            /*
             * -------------------------------------------------
             * Web URL
             * -------------------------------------------------
             */

            String url =
                    extractUrl(
                            originalCommand
                    );

            if (url != null) {

                Intent intent =
                        new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(url)
                        );

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                if (intent.resolveActivity(
                        context.getPackageManager()
                ) == null) {

                    return failure(
                            JarvisError.Type.EXECUTION_FAILED,
                            "No Android application can open this URL."
                    );
                }

                context.startActivity(intent);

                return success(
                        action,
                        "URL opened successfully: " + url
                );
            }

            /*
             * -------------------------------------------------
             * Unknown action
             * -------------------------------------------------
             */

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Unsupported Android intent action: "
                            + action
            );

        } catch (Exception exception) {

            return JarvisResult.failure(
                    JarvisError.fromException(
                            JarvisError.Type.EXECUTION_FAILED,
                            "Android intent execution failed.",
                            TOOL_ID,
                            exception
                    )
            );
        }
    }

    @Override
    public Map<String, String> getMetadata() {

        Map<String, String> metadata =
                new LinkedHashMap<>();

        metadata.put(
                "platform",
                "Android"
        );

        metadata.put(
                "execution",
                "real"
        );

        metadata.put(
                "security",
                "allowlisted"
        );

        return Collections.unmodifiableMap(
                metadata
        );
    }

    private boolean isSettingsAction(
            String action,
            String command
    ) {

        if (containsAny(
                action,
                "open_settings",
                "settings",
                "open_system_settings"
        )) {
            return true;
        }

        return containsAny(
                command,
                "افتح الاعدادات",
                "افتح الإعدادات",
                "الإعدادات",
                "الاعدادات",
                "settings"
        );
    }

    private String extractUrl(
            String command
    ) {

        if (command == null ||
                command.isEmpty()) {
            return null;
        }

        String[] parts =
                command.split("\\s+");

        for (String part : parts) {

            String value =
                    part.trim();

            if (value.startsWith("https://") ||
                    value.startsWith("http://")) {

                return value;
            }
        }

        return null;
    }

    private static String stringValue(
            Object value
    ) {

        return value == null
                ? ""
                : String.valueOf(value);
    }

    private static String normalize(
            String value
    ) {

        return value == null
                ? ""
                : value.trim().toLowerCase();
    }

    private static boolean containsAny(
            String value,
            String... values
    ) {

        String normalized =
                normalize(value);

        for (String item : values) {

            if (normalized.contains(
                    normalize(item)
            )) {
                return true;
            }
        }

        return false;
    }

    private static JarvisResult<ToolOutput> success(
            String action,
            String message
    ) {

        ToolOutput output =
                new ToolOutput(
                        TOOL_ID,
                        action,
                        message
                );

        return JarvisResult.success(
                output,
                message
        );
    }

    private static JarvisResult<ToolOutput> failure(
            JarvisError.Type type,
            String message
    ) {

        return JarvisResult.failure(
                JarvisError.of(
                        type,
                        message,
                        TOOL_ID
                )
        );
    }
}