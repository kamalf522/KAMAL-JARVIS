package com.kamal.jarvis;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * JARVIS SELF BUILDER ENGINE
 *
 * المحرك المسؤول عن:
 *
 * 1. Workspace داخلي
 * 2. قراءة وكتابة Source
 * 3. إنشاء الملفات
 * 4. تعديل الملفات بشكل آمن
 * 5. Snapshot
 * 6. Backup
 * 7. Rollback
 * 8. Hash verification
 * 9. البحث داخل المشروع
 * 10. تتبع التطوير
 * 11. Skills / Capabilities
 *
 * هذا المحرك لا يعدل APK المثبت مباشرة.
 * تعديل كود Android الحقيقي يحتاج Build/Update من بعد.
 */
public class SelfBuilderEngine {

    private static final String WORKSPACE_NAME =
            "jarvis_workspace";

    private static final String BACKUPS_NAME =
            "backups";

    private static final String SNAPSHOTS_NAME =
            "snapshots";

    private static final String HISTORY_FILE =
            "builder_history.json";

    private static final int MAX_HISTORY = 150;

    private final Context context;

    private final MemoryManager memoryManager;
    private final SkillManager skillManager;
    private final CapabilityManager capabilityManager;

    private final File workspace;
    private final File backups;
    private final File snapshots;
    private final File history;

    public SelfBuilderEngine(Context context) {

        this.context =
                context.getApplicationContext();

        memoryManager =
                new MemoryManager(this.context);

        skillManager =
                new SkillManager(this.context);

        capabilityManager =
                new CapabilityManager(this.context);

        workspace =
                new File(
                        this.context.getFilesDir(),
                        WORKSPACE_NAME
                );

        backups =
                new File(
                        workspace,
                        BACKUPS_NAME
                );

        snapshots =
                new File(
                        workspace,
                        SNAPSHOTS_NAME
                );

        history =
                new File(
                        workspace,
                        HISTORY_FILE
                );

        initializeWorkspace();
    }

    // =========================================================
    // WORKSPACE
    // =========================================================

    private synchronized void initializeWorkspace() {

        try {

            if (!workspace.exists()) {
                workspace.mkdirs();
            }

            if (!backups.exists()) {
                backups.mkdirs();
            }

            if (!snapshots.exists()) {
                snapshots.mkdirs();
            }

            if (!history.exists()) {
                writeFile(
                        history,
                        "[]"
                );
            }

        } catch (Exception e) {

            recordHistory(
                    "WORKSPACE_ERROR",
                    safeError(e)
            );
        }
    }

    public synchronized String initialize() {

        initializeWorkspace();

        return
                "JARVIS SELF BUILDER\n"
                        + "============================\n"
                        + "Workspace: ONLINE ✓\n"
                        + "Backup: ONLINE ✓\n"
                        + "Snapshots: ONLINE ✓\n"
                        + "Rollback: READY ✓\n"
                        + "Path:\n"
                        + workspace.getAbsolutePath();
    }

    public String getWorkspacePath() {
        return workspace.getAbsolutePath();
    }

    // =========================================================
    // DEVELOPMENT TARGET
    // =========================================================

    public synchronized String registerDevelopment(
            String systemName,
            String description
    ) {

        if (isBlank(systemName)) {

            return
                    "خاصك تحدد اسم النظام اللي بغيتي نطورو.";
        }

        String name =
                systemName.trim();

        String details =
                isBlank(description)
                        ? "JARVIS development target"
                        : description.trim();

        memoryManager.saveMemory(
                "__builder_target__",
                name
        );

        memoryManager.saveMemory(
                "__builder_description__",
                details
        );

        recordHistory(
                "DEVELOPMENT_TARGET",
                name + " | " + details
        );

        return
                "تم تسجيل هدف التطوير ✓\n\n"
                        + "النظام: "
                        + name
                        + "\n"
                        + "الوصف: "
                        + details;
    }

    public String getCurrentTarget() {

        String target =
                memoryManager.getMemory(
                        "__builder_target__"
                );

        if (isBlank(target)) {

            return
                    "ما كاين حتى هدف تطوير حالي.";
        }

        return
                "هدف التطوير الحالي:\n"
                        + target;
    }

    // =========================================================
    // BUILD PLAN
    // =========================================================

    public synchronized String createBuildPlan(
            String systemName
    ) {

        if (isBlank(systemName)) {
            return "اسم النظام غير موجود.";
        }

        String name =
                systemName.trim();

        JSONArray plan =
                new JSONArray();

        addPlanStep(
                plan,
                1,
                "ANALYZE",
                "تحليل النظام"
        );

        addPlanStep(
                plan,
                2,
                "DEPENDENCIES",
                "تحليل dependencies"
        );

        addPlanStep(
                plan,
                3,
                "TARGET",
                "تحديد الملفات المرتبطة"
        );

        addPlanStep(
                plan,
                4,
                "SNAPSHOT",
                "إنشاء Snapshot"
        );

        addPlanStep(
                plan,
                5,
                "BACKUP",
                "إنشاء Backup"
        );

        addPlanStep(
                plan,
                6,
                "MODIFY",
                "تطبيق التغيير"
        );

        addPlanStep(
                plan,
                7,
                "VERIFY",
                "التحقق من Hash والمحتوى"
        );

        addPlanStep(
                plan,
                8,
                "TEST",
                "تشغيل الاختبارات"
        );

        addPlanStep(
                plan,
                9,
                "ROLLBACK",
                "التراجع إذا فشل"
        );

        addPlanStep(
                plan,
                10,
                "LEARN",
                "حفظ النتيجة والتعلم"
        );

        JSONObject result =
                new JSONObject();

        try {

            result.put(
                    "system",
                    name
            );

            result.put(
                    "created",
                    now()
            );

            result.put(
                    "steps",
                    plan
            );

        } catch (Exception ignored) {
        }

        String output =
                result.toString();

        memoryManager.saveMemory(
                "__builder_plan__",
                output
        );

        recordHistory(
                "BUILD_PLAN",
                name
        );

        return output;
    }

    // =========================================================
    // WRITE SOURCE
    // =========================================================

    public synchronized String writeSourceFile(
            String path,
            String content
    ) {

        if (!validPath(path)) {
            return "مسار الملف غير صالح.";
        }

        if (content == null) {
            content = "";
        }

        try {

            File target =
                    safeFile(path);

            String oldHash =
                    target.exists()
                            ? sha256File(target)
                            : "NONE";

            String backupId =
                    target.exists()
                            ? createBackupAndReturn(path)
                            : "";

            atomicWrite(
                    target,
                    content
            );

            String verified =
                    readText(target);

            if (!content.equals(verified)) {

                if (!backupId.isEmpty()) {

                    restoreBackup(
                            path,
                            backupId
                    );
                }

                return
                        "فشل التحقق من الكتابة.\n"
                                + "تم إيقاف العملية.";
            }

            String newHash =
                    sha256(verified);

            recordHistory(
                    "WRITE_SUCCESS",
                    path
                            + " | OLD="
                            + oldHash
                            + " | NEW="
                            + newHash
            );

            memoryManager.saveMemory(
                    "__last_written_file__",
                    path
            );

            memoryManager.saveMemory(
                    "__last_written_hash__",
                    newHash
            );

            return
                    "تم تعديل الملف ✓\n\n"
                            + "File: "
                            + path
                            + "\n"
                            + "SHA-256:\n"
                            + newHash;

        } catch (Exception e) {

            return
                    "فشل تعديل الملف:\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // PATCH
    // =========================================================

    public synchronized String applyTextPatch(
            String path,
            String oldText,
            String newText,
            String reason
    ) {

        if (!validPath(path)) {
            return "مسار الملف غير صالح.";
        }

        if (isBlank(oldText)) {
            return "النص القديم غير موجود.";
        }

        if (newText == null) {
            newText = "";
        }

        try {

            File target =
                    safeFile(path);

            if (!target.exists()) {

                return
                        "الملف غير موجود:\n"
                                + path;
            }

            String source =
                    readText(target);

            int first =
                    source.indexOf(oldText);

            if (first < 0) {

                return
                        "ما لقيتش النص المطلوب داخل الملف:\n"
                                + path;
            }

            int second =
                    source.indexOf(
                            oldText,
                            first + oldText.length()
                    );

            if (second >= 0) {

                return
                        "رفض التعديل الآمن ⚠\n"
                                + "النص موجود أكثر من مرة.\n"
                                + "خاص تحديد جزء أدق.";
            }

            String snapshot =
                    createSnapshot(
                            "قبل Patch: "
                                    + safeText(reason)
                    );

            String backup =
                    createBackupAndReturn(path);

            String updated =
                    source.substring(
                            0,
                            first
                    )
                            + newText
                            + source.substring(
                            first + oldText.length()
                    );

            atomicWrite(
                    target,
                    updated
            );

            String verification =
                    readText(target);

            if (!updated.equals(verification)) {

                restoreBackup(
                        path,
                        backup
                );

                return
                        "فشل التحقق من Patch.\n"
                                + "تم Rollback تلقائيا.";
            }

            String hash =
                    sha256(verification);

            recordHistory(
                    "PATCH_SUCCESS",
                    path
                            + " | reason="
                            + safeText(reason)
                            + " | snapshot="
                            + snapshot
            );

            memoryManager.saveMemory(
                    "__last_patch_file__",
                    path
            );

            memoryManager.saveMemory(
                    "__last_patch_snapshot__",
                    snapshot
            );

            return
                    "تعديل الكود ناجح ✓\n\n"
                            + "File: "
                            + path
                            + "\n"
                            + "Snapshot: "
                            + snapshot
                            + "\n"
                            + "Hash:\n"
                            + hash;

        } catch (Exception e) {

            return
                    "فشل تعديل الكود:\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // EVOLUTION TRANSACTION
    // =========================================================

    public synchronized String evolveSourceFile(
            String path,
            String oldText,
            String newText,
            String reason
    ) {

        if (!validPath(path)) {

            return
                    "Evolution مرفوض: مسار غير صالح.";
        }

        try {

            File target =
                    safeFile(path);

            if (!target.exists()) {

                return
                        "Evolution مرفوض: الملف غير موجود.";
            }

            String before =
                    readText(target);

            String beforeHash =
                    sha256(before);

            String snapshot =
                    createSnapshot(
                            "Evolution: "
                                    + safeText(reason)
                    );

            String result =
                    applyTextPatch(
                            path,
                            oldText,
                            newText,
                            reason
                    );

            if (!isEvolutionSuccess(result)) {

                recordHistory(
                        "EVOLUTION_FAILED",
                        path
                                + " | "
                                + result
                );

                return
                        "Evolution فشل ⚠\n\n"
                                + result
                                + "\n\nSnapshot:\n"
                                + snapshot;
            }

            String after =
                    readText(target);

            String afterHash =
                    sha256(after);

            if (beforeHash.equals(afterHash)) {

                rollback(
                        snapshot
                );

                return
                        "Evolution فشل: التغيير لم يحدث.";
            }

            memoryManager.saveMemory(
                    "__last_evolution_file__",
                    path
            );

            memoryManager.saveMemory(
                    "__last_evolution_hash__",
                    afterHash
            );

            memoryManager.saveMemory(
                    "__last_evolution_reason__",
                    safeText(reason)
            );

            memoryManager.saveMemory(
                    "__last_evolution_snapshot__",
                    snapshot
            );

            recordHistory(
                    "EVOLUTION_SUCCESS",
                    path
                            + " | before="
                            + beforeHash
                            + " | after="
                            + afterHash
                            + " | reason="
                            + safeText(reason)
            );

            return
                    "EVOLUTION SUCCESS ✓\n\n"
                            + "File: "
                            + path
                            + "\n"
                            + "Before:\n"
                            + beforeHash
                            + "\n\n"
                            + "After:\n"
                            + afterHash
                            + "\n\n"
                            + "Snapshot:\n"
                            + snapshot;

        } catch (Exception e) {

            return
                    "Evolution فشل ⚠\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // READ
    // =========================================================

    public String readSourceFile(
            String path
    ) {

        if (!validPath(path)) {
            return "مسار الملف غير صالح.";
        }

        try {

            File file =
                    safeFile(path);

            if (!file.exists()) {

                return
                        "الملف غير موجود:\n"
                                + path;
            }

            return readText(file);

        } catch (Exception e) {

            return
                    "فشل قراءة الملف:\n"
                            + safeError(e);
        }
    }

    public String importSource(
            String path,
            String content
    ) {

        return writeSourceFile(
                path,
                content
        );
    }

    // =========================================================
    // HASH
    // =========================================================

    public String getFileHash(
            String path
    ) {

        if (!validPath(path)) {
            return "مسار الملف غير صالح.";
        }

        try {

            File file =
                    safeFile(path);

            if (!file.exists()) {
                return "الملف غير موجود.";
            }

            return sha256File(file);

        } catch (Exception e) {

            return
                    "تعذر حساب Hash:\n"
                            + safeError(e);
        }
    }

    public boolean fileExists(
            String path
    ) {

        try {

            return validPath(path)
                    && safeFile(path).exists();

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // SEARCH
    // =========================================================

    public String searchSource(
            String query
    ) {

        if (isBlank(query)) {
            return "خاصني كلمة أو جملة للبحث.";
        }

        String search =
                query.trim()
                        .toLowerCase(Locale.ROOT);

        StringBuilder result =
                new StringBuilder();

        int matches = 0;

        try {

            List<File> files =
                    collectFiles(workspace);

            for (File file : files) {

                if (!isReadableTextFile(file)) {
                    continue;
                }

                String content =
                        readText(file);

                if (content.toLowerCase(
                        Locale.ROOT
                ).contains(search)) {

                    matches++;

                    result.append("• ")
                            .append(
                                    relativePath(file)
                            )
                            .append("\n");
                }
            }

            if (matches == 0) {

                return
                        "ما لقيتش:\n"
                                + query;
            }

            return
                    "نتائج البحث: "
                            + matches
                            + "\n\n"
                            + result;

        } catch (Exception e) {

            return
                    "فشل البحث:\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // FILE LIST
    // =========================================================

    public String listProjectFiles() {

        try {

            List<File> files =
                    collectFiles(workspace);

            StringBuilder result =
                    new StringBuilder();

            int count = 0;

            for (File file : files) {

                if (!isReadableTextFile(file)) {
                    continue;
                }

                count++;

                result.append(count)
                        .append(". ")
                        .append(
                                relativePath(file)
                        )
                        .append("\n");
            }

            if (count == 0) {
                return "Workspace فارغ حاليا.";
            }

            return
                    "PROJECT FILES: "
                            + count
                            + "\n\n"
                            + result;

        } catch (Exception e) {

            return
                    "فشل قراءة المشروع:\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // SNAPSHOT
    // =========================================================

    public synchronized String createSnapshot(
            String reason
    ) {

        try {

            initializeWorkspace();

            String id =
                    new SimpleDateFormat(
                            "yyyyMMdd_HHmmss_SSS",
                            Locale.US
                    ).format(
                            new Date()
                    );

            File root =
                    new File(
                            snapshots,
                            id
                    );

            if (!root.mkdirs()) {

                return
                        "فشل إنشاء Snapshot.";
            }

            copyDirectory(
                    workspace,
                    root,
                    root
            );

            JSONObject metadata =
                    new JSONObject();

            metadata.put(
                    "id",
                    id
            );

            metadata.put(
                    "reason",
                    safeText(reason)
            );

            metadata.put(
                    "created",
                    now()
            );

            writeFile(
                    new File(
                            root,
                            "snapshot.json"
                    ),
                    metadata.toString()
            );

            recordHistory(
                    "SNAPSHOT_CREATED",
                    id
                            + " | "
                            + safeText(reason)
            );

            return id;

        } catch (Exception e) {

            return
                    "Snapshot فشل:\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // ROLLBACK
    // =========================================================

    public synchronized String rollback(
            String snapshotId
    ) {

        if (isBlank(snapshotId)) {
            return "خاصك تحدد Snapshot.";
        }

        try {

            File snapshot =
                    new File(
                            snapshots,
                            snapshotId.trim()
                    );

            if (!snapshot.exists()) {

                return
                        "Snapshot غير موجود:\n"
                                + snapshotId;
            }

            List<File> current =
                    collectFiles(workspace);

            for (File file : current) {

                if (file.equals(history)
                        || isInside(
                        file,
                        snapshots
                )
                        || isInside(
                        file,
                        backups
                )) {

                    continue;
                }

                file.delete();
            }

            copyDirectory(
                    snapshot,
                    workspace,
                    workspace
            );

            recordHistory(
                    "ROLLBACK_SUCCESS",
                    snapshotId
            );

            memoryManager.saveMemory(
                    "__last_rollback_snapshot__",
                    snapshotId
            );

            return
                    "Rollback ناجح ✓\n"
                            + "Snapshot: "
                            + snapshotId;

        } catch (Exception e) {

            return
                    "Rollback فشل:\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // SKILLS / CAPABILITIES
    // =========================================================

    public void registerSkill(
            String name,
            String description
    ) {

        if (!isBlank(name)) {

            skillManager.addSkill(
                    name.trim(),
                    description
            );
        }
    }

    public void registerCapability(
            String name,
            String description
    ) {

        if (!isBlank(name)) {

            capabilityManager.addCapability(
                    name.trim(),
                    description
            );
        }
    }

    // =========================================================
    // STATUS
    // =========================================================

    public String getStatus() {

        return
                "JARVIS SELF BUILDER\n"
                        + "============================\n"
                        + "Workspace: "
                        + (
                        workspace.exists()
                                ? "READY"
                                : "ERROR"
                )
                        + "\n"
                        + "Backups: "
                        + (
                        backups.exists()
                                ? "READY"
                                : "ERROR"
                )
                        + "\n"
                        + "Snapshots: "
                        + (
                        snapshots.exists()
                                ? "READY"
                                : "ERROR"
                )
                        + "\n"
                        + "History: "
                        + (
                        history.exists()
                                ? "READY"
                                : "ERROR"
                )
                        + "\n"
                        + "Path:\n"
                        + workspace.getAbsolutePath();
    }

    public boolean isHealthy() {

        try {

            return workspace.exists()
                    && workspace.isDirectory()
                    && backups.exists()
                    && snapshots.exists()
                    && history.exists();

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // HISTORY
    // =========================================================

    private synchronized void recordHistory(
            String type,
            String message
    ) {

        try {

            JSONArray array =
                    new JSONArray(
                            readText(history)
                    );

            JSONObject item =
                    new JSONObject();

            item.put(
                    "type",
                    safeText(type)
            );

            item.put(
                    "message",
                    safeText(message)
            );

            item.put(
                    "time",
                    now()
            );

            array.put(item);

            while (
                    array.length()
                            > MAX_HISTORY
            ) {

                array.remove(0);
            }

            writeFile(
                    history,
                    array.toString()
            );

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // BACKUP
    // =========================================================

    private synchronized String createBackupAndReturn(
            String path
    ) {

        try {

            File source =
                    safeFile(path);

            if (!source.exists()) {
                return "";
            }

            String id =
                    new SimpleDateFormat(
                            "yyyyMMdd_HHmmss_SSS",
                            Locale.US
                    ).format(
                            new Date()
                    );

            File target =
                    new File(
                            backups,
                            id
                                    + "_"
                                    + source.getName()
                    );

            copyFile(
                    source,
                    target
            );

            recordHistory(
                    "BACKUP_CREATED",
                    path + " -> " + id
            );

            return id;

        } catch (Exception e) {

            return "";
        }
    }

    private void restoreBackup(
            String path,
            String backupId
    ) {

        if (isBlank(backupId)) {
            return;
        }

        try {

            File source =
                    safeFile(path);

            File backup =
                    findBackup(
                            backupId,
                            source.getName()
                    );

            if (backup != null
                    && backup.exists()) {

                copyFile(
                        backup,
                        source
                );
            }

        } catch (Exception ignored) {
        }
    }

    private File findBackup(
            String id,
            String name
    ) {

        File[] files =
                backups.listFiles();

        if (files == null) {
            return null;
        }

        String prefix =
                id + "_" + name;

        for (File file : files) {

            if (file.getName()
                    .startsWith(prefix)) {

                return file;
            }
        }

        return null;
    }

    // =========================================================
    // FILE SAFETY
    // =========================================================

    private boolean validPath(
            String path
    ) {

        if (path == null
                || path.trim().isEmpty()) {

            return false;
        }

        String clean =
                path.trim()
                        .replace(
                                '\\',
                                '/'
                        );

        if (clean.startsWith("/")
                || clean.contains("..")
                || clean.contains("\0")) {

            return false;
        }

        return true;
    }

    private File safeFile(
            String path
    ) throws Exception {

        File root =
                workspace.getCanonicalFile();

        File target =
                new File(
                        root,
                        path.replace(
                                '\\',
                                '/'
                        )
                ).getCanonicalFile();

        if (!target.getPath()
                .startsWith(
                        root.getPath()
                )) {

            throw new SecurityException(
                    "Invalid workspace path"
            );
        }

        File parent =
                target.getParentFile();

        if (parent != null
                && !parent.exists()) {

            parent.mkdirs();
        }

        return target;
    }

    // =========================================================
    // FILE UTILITIES
    // =========================================================

    private void atomicWrite(
            File file,
            String content
    ) throws Exception {

        File parent =
                file.getParentFile();

        if (parent != null
                && !parent.exists()) {

            parent.mkdirs();
        }

        File temp =
                new File(
                        file.getAbsolutePath()
                                + ".tmp"
                );

        writeFile(
                temp,
                content
        );

        if (file.exists()
                && !file.delete()) {

            temp.delete();

            throw new Exception(
                    "تعذر استبدال الملف"
            );
        }

        if (!temp.renameTo(file)) {

            temp.delete();

            throw new Exception(
                    "تعذر إتمام الكتابة"
            );
        }
    }

    private void writeFile(
            File file,
            String content
    ) throws Exception {

        File parent =
                file.getParentFile();

        if (parent != null
                && !parent.exists()) {

            parent.mkdirs();
        }

        FileOutputStream output =
                new FileOutputStream(file);

        try {

            output.write(
                    content.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            output.flush();

        } finally {

            output.close();
        }
    }

    private String readText(
            File file
    ) throws Exception {

        FileInputStream input =
                new FileInputStream(file);

        try {

            byte[] data =
                    new byte[
                            (int) Math.min(
                                    file.length(),
                                    Integer.MAX_VALUE
                            )
                    ];

            int offset = 0;
            int read;

            while (
                    offset < data.length
                            && (
                            read = input.read(
                                    data,
                                    offset,
                                    data.length - offset
                            )
                    ) > 0
            ) {

                offset += read;
            }

            return new String(
                    data,
                    0,
                    offset,
                    StandardCharsets.UTF_8
            );

        } finally {

            input.close();
        }
    }

    private void copyFile(
            File source,
            File target
    ) throws Exception {

        File parent =
                target.getParentFile();

        if (parent != null
                && !parent.exists()) {

            parent.mkdirs();
        }

        FileInputStream input =
                new FileInputStream(source);

        FileOutputStream output =
                new FileOutputStream(target);

        try {

            byte[] buffer =
                    new byte[8192];

            int length;

            while (
                    (length =
                            input.read(buffer))
                            > 0
            ) {

                output.write(
                        buffer,
                        0,
                        length
                );
            }

            output.flush();

        } finally {

            input.close();
            output.close();
        }
    }

    private void copyDirectory(
            File source,
            File target,
            File excludedRoot
    ) throws Exception {

        if (!source.exists()) {
            return;
        }

        if (source.equals(excludedRoot)) {
            return;
        }

        if (source.isFile()) {

            copyFile(
                    source,
                    target
            );

            return;
        }

        if (!target.exists()) {
            target.mkdirs();
        }

        File[] children =
                source.listFiles();

        if (children == null) {
            return;
        }

        for (File child : children) {

            if (child.equals(excludedRoot)) {
                continue;
            }

            if (child.equals(
                    snapshots
            )) {
                continue;
            }

            if (child.equals(
                    backups
            )) {
                continue;
            }

            if (child.equals(
                    history
            )) {
                continue;
            }

            File destination =
                    new File(
                            target,
                            child.getName()
                    );

            if (child.isDirectory()) {

                copyDirectory(
                        child,
                        destination,
                        excludedRoot
                );

            } else {

                copyFile(
                        child,
                        destination
                );
            }
        }
    }

    private List<File> collectFiles(
            File root
    ) {

        List<File> result =
                new ArrayList<>();

        if (root == null
                || !root.exists()) {

            return result;
        }

        if (root.isFile()) {

            result.add(root);

            return result;
        }

        File[] children =
                root.listFiles();

        if (children == null) {
            return result;
        }

        for (File child : children) {

            if (child.equals(
                    backups
            )
                    || child.equals(
                    snapshots
            )) {

                continue;
            }

            if (child.isDirectory()) {

                result.addAll(
                        collectFiles(child)
                );

            } else {

                result.add(child);
            }
        }

        return result;
    }

    private boolean isReadableTextFile(
            File file
    ) {

        if (file == null
                || !file.isFile()) {

            return false;
        }

        String name =
                file.getName()
                        .toLowerCase(
                                Locale.ROOT
                        );

        return name.endsWith(".java")
                || name.endsWith(".kt")
                || name.endsWith(".xml")
                || name.endsWith(".gradle")
                || name.endsWith(".properties")
                || name.endsWith(".json")
                || name.endsWith(".txt")
                || name.endsWith(".md");
    }

    private boolean isInside(
            File file,
            File directory
    ) {

        try {

            String child =
                    file.getCanonicalPath();

            String parent =
                    directory.getCanonicalPath();

            return child.startsWith(
                    parent
                            + File.separator
            );

        } catch (Exception e) {

            return false;
        }
    }

    private String relativePath(
            File file
    ) {

        try {

            String root =
                    workspace.getCanonicalPath();

            String path =
                    file.getCanonicalPath();

            if (path.startsWith(
                    root
            )) {

                return path.substring(
                        root.length()
                                + 1
                );
            }

            return file.getName();

        } catch (Exception e) {

            return file.getName();
        }
    }

    // =========================================================
    // HASH
    // =========================================================

    private String sha256File(
            File file
    ) throws Exception {

        return sha256(
                readText(file)
        );
    }

    private String sha256(
            String value
    ) throws Exception {

        MessageDigest digest =
                MessageDigest.getInstance(
                        "SHA-256"
                );

        byte[] hash =
                digest.digest(
                        value.getBytes(
                                StandardCharsets.UTF_8
                        )
                );

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

    // =========================================================
    // PLAN
    // =========================================================

    private void addPlanStep(
            JSONArray array,
            int number,
            String name,
            String description
    ) {

        try {

            JSONObject item =
                    new JSONObject();

            item.put(
                    "step",
                    number
            );

            item.put(
                    "name",
                    name
            );

            item.put(
                    "description",
                    description
            );

            array.put(item);

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // SUCCESS
    // =========================================================

    private boolean isEvolutionSuccess(
            String result
    ) {

        if (isBlank(result)) {
            return false;
        }

        String lower =
                result.toLowerCase(
                        Locale.ROOT
                );

        if (lower.contains("فشل")
                || lower.contains("failed")
                || lower.contains("error")
                || lower.contains("مرفوض")) {

            return false;
        }

        return lower.contains(
                "evolution success"
        )
                || lower.contains(
                "تعديل الكود ناجح"
        );
    }

    // =========================================================
    // TEXT HELPERS
    // =========================================================

    private boolean isBlank(
            String value
    ) {

        return value == null
                || value.trim().isEmpty();
    }

    private String safeText(
            String value
    ) {

        if (isBlank(value)) {
            return "unspecified";
        }

        return value.trim();
    }

    private String safeError(
            Exception e
    ) {

        if (e == null) {
            return "Unknown error";
        }

        String message =
                e.getMessage();

        if (isBlank(message)) {

            return e.getClass()
                    .getSimpleName();
        }

        return message;
    }

    private String now() {

        return new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
        ).format(
                new Date()
        );
    }
}