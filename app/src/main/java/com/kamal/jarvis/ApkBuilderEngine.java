package com.kamal.jarvis;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * JARVIS APK BUILDER ENGINE
 *
 * مسؤول عن:
 * - التحقق من Android Project
 * - تجهيز Build
 * - محاولة البناء المحلي إذا كان Gradle متاحا
 * - تتبع حالة Build
 * - اكتشاف APK الناتج
 * - حساب SHA-256
 * - حفظ تاريخ عمليات البناء
 *
 * ملاحظة:
 * Android المثبت لا يضمن وجود Gradle/Android SDK داخله.
 * لذلك هذا المحرك لا يدعي نجاح Build إذا لم يكن
 * Build Toolchain موجودا.
 */
public class ApkBuilderEngine {

    private static final String PREFS_NAME =
            "jarvis_apk_builder";

    private static final String KEY_LAST_BUILD =
            "last_build";

    private static final String KEY_HISTORY =
            "build_history";

    private static final String KEY_STATUS =
            "build_status";

    private final Context context;

    private final SelfBuilderEngine selfBuilderEngine;

    public ApkBuilderEngine(Context context) {

        this.context =
                context.getApplicationContext();

        selfBuilderEngine =
                new SelfBuilderEngine(
                        this.context
                );
    }

    // =========================================================
    // PROJECT VALIDATION
    // =========================================================

    public String validateProject() {

        try {

            File workspace =
                    new File(
                            selfBuilderEngine
                                    .getWorkspacePath()
                    );

            if (!workspace.exists()) {

                return
                        "BUILD ERROR\n"
                        + "Workspace غير موجود.";
            }

            File settings =
                    new File(
                            workspace,
                            "settings.gradle"
                    );

            File appGradle =
                    new File(
                            workspace,
                            "app/build.gradle"
                    );

            File rootGradle =
                    new File(
                            workspace,
                            "build.gradle"
                    );

            StringBuilder result =
                    new StringBuilder();

            result.append(
                    "JARVIS PROJECT CHECK\n"
            );

            result.append(
                    "============================\n\n"
            );

            result.append(
                    "Workspace: "
            )
                    .append(
                            workspace.exists()
                                    ? "OK"
                                    : "MISSING"
                    )
                    .append("\n");

            result.append(
                    "settings.gradle: "
            )
                    .append(
                            settings.exists()
                                    ? "OK"
                                    : "MISSING"
                    )
                    .append("\n");

            result.append(
                    "build.gradle: "
            )
                    .append(
                            rootGradle.exists()
                                    ? "OK"
                                    : "MISSING"
                    )
                    .append("\n");

            result.append(
                    "app/build.gradle: "
            )
                    .append(
                            appGradle.exists()
                                    ? "OK"
                                    : "MISSING"
                    )
                    .append("\n\n");

            if (!settings.exists() ||
                    !appGradle.exists() ||
                    !rootGradle.exists()) {

                result.append(
                        "STATUS: NOT READY"
                );

                saveStatus(
                        "NOT_READY"
                );

                return result.toString();
            }

            result.append(
                    "STATUS: PROJECT READY\n"
            );

            result.append(
                    "ولكن Build يحتاج Gradle/Android SDK."
            );

            saveStatus(
                    "PROJECT_READY"
            );

            return result.toString();

        } catch (Exception e) {

            saveStatus(
                    "ERROR"
            );

            return
                    "Project validation failed:\n"
                    + safeError(e);
        }
    }

    // =========================================================
    // BUILD
    // =========================================================

    public String buildDebugApk() {

        String buildId =
                createBuildId();

        saveStatus(
                "BUILDING"
        );

        recordBuild(
                "START",
                buildId,
                "بدأ Build Debug APK"
        );

        String validation =
                validateProject();

        if (validation.contains(
                "NOT READY"
        )) {

            saveStatus(
                    "FAILED"
            );

            recordBuild(
                    "FAILED",
                    buildId,
                    "Project غير جاهز"
            );

            return
                    "BUILD FAILED\n\n"
                    + validation;
        }

        File workspace =
                new File(
                        selfBuilderEngine
                                .getWorkspacePath()
                );

        File gradlew =
                new File(
                        workspace,
                        "gradlew"
                );

        File gradlewBat =
                new File(
                        workspace,
                        "gradlew.bat"
                );

        if (!gradlew.exists() &&
                !gradlewBat.exists()) {

            saveStatus(
                    "WAITING_FOR_BUILDER"
            );

            recordBuild(
                    "WAITING",
                    buildId,
                    "Gradle Wrapper غير موجود"
            );

            return
                    "BUILD READY\n\n"
                    + "Project صالح.\n"
                    + "ولكن Gradle Wrapper غير موجود داخل Workspace.\n\n"
                    + "JARVIS ما غاديش يكذب عليك ويقول APK تبنى.\n"
                    + "الـAPK خاصو Build Environment خارجي "
                    + "أو Gradle/Android SDK موجود.";
        }

        String command;

        if (gradlew.exists()) {

            command =
                    "./gradlew assembleDebug";

        } else {

            command =
                    "gradlew.bat assembleDebug";
        }

        String output;

        try {

            output =
                    executeCommand(
                            command,
                            workspace
                    );

        } catch (Exception e) {

            saveStatus(
                    "FAILED"
            );

            recordBuild(
                    "FAILED",
                    buildId,
                    safeError(e)
            );

            return
                    "BUILD FAILED\n\n"
                    + safeError(e);
        }

        File apk =
                findApk(
                        workspace
                );

        if (apk == null ||
                !apk.exists()) {

            saveStatus(
                    "FAILED"
            );

            recordBuild(
                    "FAILED",
                    buildId,
                    "Gradle انتهى بدون APK"
            );

            return
                    "BUILD FAILED\n\n"
                    + "Gradle ما خرجش APK.\n\n"
                    + "OUTPUT:\n"
                    + limitOutput(output);
        }

        String hash;

        try {

            hash =
                    sha256(
                            apk
                    );

        } catch (Exception e) {

            hash =
                    "HASH_ERROR";
        }

        saveStatus(
                "SUCCESS"
        );

        recordBuild(
                "SUCCESS",
                buildId,
                apk.getAbsolutePath()
                        + " | SHA256="
                        + hash
        );

        return
                "BUILD SUCCESS ✓\n\n"
                + "Build ID: "
                + buildId
                + "\n\n"
                + "APK:\n"
                + apk.getAbsolutePath()
                + "\n\n"
                + "SHA-256:\n"
                + hash;
    }

    // =========================================================
    // BUILD REQUEST
    // =========================================================

    public String prepareBuildRequest(
            String reason
    ) {

        String buildId =
                createBuildId();

        try {

            File workspace =
                    new File(
                            selfBuilderEngine
                                    .getWorkspacePath()
                    );

            File request =
                    new File(
                            workspace,
                            "build_request.json"
                    );

            JSONObject json =
                    new JSONObject();

            json.put(
                    "buildId",
                    buildId
            );

            json.put(
                    "type",
                    "debug"
            );

            json.put(
                    "command",
                    "gradle assembleDebug"
            );

            json.put(
                    "reason",
                    reason == null
                            ? "JARVIS evolution"
                            : reason
            );

            json.put(
                    "created",
                    now()
            );

            json.put(
                    "status",
                    "requested"
            );

            writeText(
                    request,
                    json.toString(4)
            );

            recordBuild(
                    "REQUEST",
                    buildId,
                    request.getAbsolutePath()
            );

            saveStatus(
                    "REQUESTED"
            );

            return
                    "BUILD REQUEST CREATED ✓\n\n"
                    + "Build ID: "
                    + buildId
                    + "\n"
                    + "File:\n"
                    + request.getAbsolutePath();

        } catch (Exception e) {

            return
                    "فشل إنشاء Build Request:\n"
                    + safeError(e);
        }
    }

    // =========================================================
    // APK DISCOVERY
    // =========================================================

    public String findLatestApk() {

        try {

            File workspace =
                    new File(
                            selfBuilderEngine
                                    .getWorkspacePath()
                    );

            File apk =
                    findApk(
                            workspace
                    );

            if (apk == null) {

                return
                        "ما لقيتش APK داخل Workspace.";
            }

            String hash;

            try {

                hash =
                        sha256(
                                apk
                        );

            } catch (Exception e) {

                hash =
                        "غير متوفر";
            }

            return
                    "LATEST APK\n\n"
                    + "Path:\n"
                    + apk.getAbsolutePath()
                    + "\n\n"
                    + "Size: "
                    + apk.length()
                    + " bytes\n\n"
                    + "SHA-256:\n"
                    + hash;

        } catch (Exception e) {

            return
                    "فشل البحث عن APK:\n"
                    + safeError(e);
        }
    }

    // =========================================================
    // STATUS
    // =========================================================

    public String getStatus() {

        android.content.SharedPreferences prefs =
                context.getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE
                );

        String status =
                prefs.getString(
                        KEY_STATUS,
                        "IDLE"
                );

        String lastBuild =
                prefs.getString(
                        KEY_LAST_BUILD,
                        "لا يوجد"
                );

        return
                "JARVIS APK BUILDER\n"
                + "============================\n"
                + "Status: "
                + status
                + "\n"
                + "Last Build:\n"
                + lastBuild;
    }

    public boolean isHealthy() {

        try {

            return selfBuilderEngine
                    .isHealthy();

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // HISTORY
    // =========================================================

    public String getBuildHistory() {

        android.content.SharedPreferences prefs =
                context.getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE
                );

        String history =
                prefs.getString(
                        KEY_HISTORY,
                        "[]"
                );

        try {

            JSONArray array =
                    new JSONArray(history);

            if (array.length() == 0) {

                return
                        "ما كاين حتى Build فالتاريخ.";
            }

            StringBuilder result =
                    new StringBuilder();

            result.append(
                    "JARVIS BUILD HISTORY\n\n"
            );

            int start =
                    Math.max(
                            0,
                            array.length() - 20
                    );

            for (int i = start;
                 i < array.length();
                 i++) {

                JSONObject item =
                        array.getJSONObject(i);

                result.append("[")
                        .append(
                                item.optString(
                                        "time"
                                )
                        )
                        .append("] ");

                result.append(
                        item.optString(
                                "status"
                        )
                );

                result.append(" | ");

                result.append(
                        item.optString(
                                "buildId"
                        )
                );

                result.append("\n");

                result.append(
                        item.optString(
                                "message"
                        )
                );

                result.append("\n\n");
            }

            return result.toString();

        } catch (Exception e) {

            return
                    "تعذر قراءة Build History.";
        }
    }

    // =========================================================
    // COMMAND EXECUTION
    // =========================================================

    private String executeCommand(
            String command,
            File directory
    ) throws Exception {

        Process process =
                Runtime.getRuntime()
                        .exec(
                                command,
                                null,
                                directory
                        );

        StringBuilder output =
                new StringBuilder();

        InputStream input =
                process.getInputStream();

        InputStream error =
                process.getErrorStream();

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                input,
                                StandardCharsets.UTF_8
                        )
                );

        BufferedReader errorReader =
                new BufferedReader(
                        new InputStreamReader(
                                error,
                                StandardCharsets.UTF_8
                        )
                );

        String line;

        while ((line =
                reader.readLine()) != null) {

            output.append(line)
                    .append("\n");
        }

        while ((line =
                errorReader.readLine()) != null) {

            output.append(line)
                    .append("\n");
        }

        int exitCode =
                process.waitFor();

        output.append(
                "\nEXIT CODE: "
        )
                .append(
                        exitCode
                );

        if (exitCode != 0) {

            throw new Exception(
                    limitOutput(
                            output.toString()
                    )
            );
        }

        return output.toString();
    }

    // =========================================================
    // APK SEARCH
    // =========================================================

    private File findApk(
            File directory
    ) {

        if (directory == null ||
                !directory.exists()) {

            return null;
        }

        File[] files =
                directory.listFiles();

        if (files == null) {

            return null;
        }

        File newest =
                null;

        for (File file : files) {

            if (file.isDirectory()) {

                File found =
                        findApk(file);

                if (found != null &&
                        (newest == null ||
                                found.lastModified()
                                        > newest.lastModified())) {

                    newest = found;
                }

            } else {

                if (file.getName()
                        .toLowerCase(
                                Locale.US
                        )
                        .endsWith(".apk")) {

                    if (newest == null ||
                            file.lastModified()
                                    > newest.lastModified()) {

                        newest = file;
                    }
                }
            }
        }

        return newest;
    }

    // =========================================================
    // HISTORY
    // =========================================================

    private void recordBuild(
            String status,
            String buildId,
            String message
    ) {

        try {

            android.content.SharedPreferences prefs =
                    context.getSharedPreferences(
                            PREFS_NAME,
                            Context.MODE_PRIVATE
                    );

            JSONArray history =
                    new JSONArray(
                            prefs.getString(
                                    KEY_HISTORY,
                                    "[]"
                            )
                    );

            JSONObject item =
                    new JSONObject();

            item.put(
                    "status",
                    status
            );

            item.put(
                    "buildId",
                    buildId
            );

            item.put(
                    "message",
                    message == null
                            ? ""
                            : message
            );

            item.put(
                    "time",
                    now()
            );

            history.put(item);

            while (history.length() > 50) {

                history.remove(0);
            }

            prefs.edit()
                    .putString(
                            KEY_HISTORY,
                            history.toString()
                    )
                    .putString(
                            KEY_LAST_BUILD,
                            item.toString()
                    )
                    .apply();

        } catch (Exception ignored) {
        }
    }

    private void saveStatus(
            String status
    ) {

        context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        )
                .edit()
                .putString(
                        KEY_STATUS,
                        status
                )
                .apply();
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private String createBuildId() {

        return
                "BUILD_"
                        + new SimpleDateFormat(
                                "yyyyMMdd_HHmmss_SSS",
                                Locale.US
                        )
                        .format(
                                new Date()
                        );
    }

    private String now() {

        return
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                )
                        .format(
                                new Date()
                        );
    }

    private String sha256(
            File file
    ) throws Exception {

        MessageDigest digest =
                MessageDigest.getInstance(
                        "SHA-256"
                );

        FileInputStream input =
                new FileInputStream(
                        file
                );

        byte[] buffer =
                new byte[8192];

        int read;

        while ((read =
                input.read(buffer)) != -1) {

            digest.update(
                    buffer,
                    0,
                    read
            );
        }

        input.close();

        byte[] hash =
                digest.digest();

        StringBuilder result =
                new StringBuilder();

        for (byte b : hash) {

            result.append(
                    String.format(
                            Locale.US,
                            "%02x",
                            b
                    )
            );
        }

        return result.toString();
    }

    private void writeText(
            File file,
            String content
    ) throws Exception {

        File parent =
                file.getParentFile();

        if (parent != null &&
                !parent.exists()) {

            parent.mkdirs();
        }

        FileOutputStream output =
                new FileOutputStream(
                        file
                );

        output.write(
                content.getBytes(
                        StandardCharsets.UTF_8
                )
        );

        output.close();
    }

    private String safeError(
            Exception e
    ) {

        if (e == null) {

            return "Unknown error";
        }

        String message =
                e.getMessage();

        if (message == null ||
                message.trim().isEmpty()) {

            return e.getClass()
                    .getSimpleName();
        }

        return message;
    }

    private String limitOutput(
            String output
    ) {

        if (output == null) {

            return "";
        }

        int max =
                6000;

        if (output.length() <= max) {

            return output;
        }

        return output.substring(
                output.length() - max
        );
    }
}