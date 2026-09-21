package com.kamal.jarvis;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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
 * محرك التطوير الذاتي الأساسي.
 *
 * المسؤوليات:
 * - Workspace
 * - قراءة وكتابة الملفات
 * - إنشاء ملفات
 * - Patch آمن
 * - Snapshot كامل
 * - Rollback كامل
 * - Backup
 * - Hash verification
 * - البحث في المشروع
 * - تسجيل History
 * - تسجيل Skills / Capabilities
 *
 * مهم:
 * هذا المحرك يعمل داخل Workspace الخاص بالتطبيق.
 * تغيير Java/XML الحقيقي يحتاج Build من بعد.
 */
public class SelfBuilderEngine {

    private static final String WORKSPACE_NAME =
            "jarvis_workspace";

    private static final String BACKUP_NAME =
            "backups";

    private static final String SNAPSHOT_NAME =
            "snapshots";

    private static final String META_FILE =
            "builder_history.json";

    private final Context context;

    private final MemoryManager memoryManager;
    private final SkillManager skillManager;
    private final CapabilityManager capabilityManager;
    private final TaskManager taskManager;

    private final File workspace;
    private final File backups;
    private final File snapshots;
    private final File historyFile;

    public SelfBuilderEngine(Context context) {

        this.context =
                context.getApplicationContext();

        memoryManager =
                new MemoryManager(this.context);

        skillManager =
                new SkillManager(this.context);

        capabilityManager =
                new CapabilityManager(this.context);

        taskManager =
                new TaskManager(this.context);

        workspace =
                new File(
                        this.context.getFilesDir(),
                        WORKSPACE_NAME
                );

        backups =
                new File(
                        workspace,
                        BACKUP_NAME
                );

        snapshots =
                new File(
                        workspace,
                        SNAPSHOT_NAME
                );

        historyFile =
                new File(
                        workspace,
                        META_FILE
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

            if (!historyFile.exists()) {
                writeText(historyFile, "[]");
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
            return "خاصك تحدد اسم النظام اللي بغيتي نطورو.";
        }

        String name = systemName.trim();

        String details =
                isBlank(description)
                        ? "System development target"
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
                "TARGET",
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
            return "ما كاين حتى هدف تطوير حالي.";
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

        String name = systemName.trim();

        taskManager.addTask(
                "تحليل كود " + name
        );

        taskManager.addTask(
                "تحليل dependencies ديال " + name
        );

        taskManager.addTask(
                "تحديد الملفات اللي خاصها التعديل"
        );

        taskManager.addTask(
                "إنشاء Snapshot"
        );

        taskManager.addTask(
                "إنشاء Backup"
        );

        taskManager.addTask(
                "تطبيق التعديل"
        );

        taskManager.addTask(
                "التحقق من التغيير"
        );

        taskManager.addTask(
                "تشغيل الاختبارات"
        );

        taskManager.addTask(
                "Rollback إذا فشل"
        );

        taskManager.addTask(
                "حفظ التطور الناجح"
        );

        memoryManager.saveMemory(
                "__builder_plan__",
                name
        );

        recordHistory(
                "BUILD_PLAN",
                name
        );

        return
                "تم إنشاء خطة التطوير ✓\n\n"
                        + "النظام: "
                        + name
                        + "\n\n"
                        + taskManager.getTasks();
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

            String snapshot =
                    createSnapshot(
                            "قبل كتابة الملف: " + path
                    );

            if (target.exists()) {
                createBackupAndReturn(path);
            }

            atomicWrite(
                    target,
                    content
            );

            String verified =
                    readText(target);

            if (!verified.equals(content)) {

                if (!"NONE".equals(oldHash)) {
                    rollbackSnapshot(
                            snapshotIdFromResult(snapshot)
                    );
                }

                return
                        "فشل التحقق من كتابة الملف.\n"
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

            return
                    "تم تعديل الملف ✓\n"
                            + path
                            + "\n\nSHA-256:\n"
                            + newHash;

        } catch (Exception e) {

            return
                    "فشل تعديل الملف:\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // TEXT PATCH
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
                                + "النص المطلوب موجود أكثر من مرة.\n"
                                + "خاص تحديد جزء أدق من الكود.";
            }

            String snapshot =
                    createSnapshot(
                            "قبل Patch: " + safeText(reason)
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

            if (!verification.equals(updated)) {

                restoreBackup(
                        path,
                        backup
                );

                return
                        "فشل التحقق من التعديل.\n"
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
                            + " | hash="
                            + hash
            );

            memoryManager.saveMemory(
                    "__last_patch_file__",
                    path
            );

            memoryManager.saveMemory(
                    "__last_patch_snapshot__",
                    snapshotIdFromResult(snapshot)
            );

            return
                    "تعديل الكود ناجح ✓\n\n"
                            + "الملف: "
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
            return "Evolution مرفوض: مسار غير صالح.";
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

            String snapshotId =
                    snapshotIdFromResult(
                            snapshot
                    );

            String result =
                    applyTextPatch(
                            path,
                            oldText,
                            newText,
                            reason
                    );

            if (result == null ||
                    !result.contains(
                            "تعديل الكود ناجح"
                    )) {

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
                                + snapshotId;
            }

            String after =
                    readText(target);

            String afterHash =
                    sha256(after);

            if (beforeHash.equals(afterHash)) {

                rollback(snapshotId);

                recordHistory(
                        "EVOLUTION_FAILED",
                        "Hash لم يتغير: " + path
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
                    snapshotId
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
                            + snapshotId;

        } catch (Exception e) {

            return
                    "Evolution فشل ⚠\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // READ SOURCE
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
                        "الملف غير موجود في Workspace:\n"
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
    // HASH / EXISTS
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

                if (isInternalFile(file)) {
                    continue;
                }

                if (!isReadableTextFile(file)) {
                    continue;
                }

                String text =
                        readText(file);

                if (text.toLowerCase(
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
    // PROJECT FILES
    // =========================================================

    public String listProjectFiles() {

        try {

            List<File> files =
                    collectFiles(workspace);

            StringBuilder result =
                    new StringBuilder();

            int count = 0;

            for (File file : files) {

                if (isInternalFile(file)) {
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
                    "فشل قراءة ملفات المشروع:\n"
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
                    ).format(new Date());

            File snapshot =
                    new File(
                            snapshots,
                            id
                    );

            if (!snapshot.mkdirs()) {

                if (!snapshot.exists()) {
                    return
                            "فشل إنشاء Snapshot.";
                }
            }

            List<File> files =
                    collectFiles(workspace);

            int copied = 0;

            for (File file : files) {

                if (isInternalFile(file)) {
                    continue;
                }

                String relative =
                        relativePath(file);

                File destination =
                        new File(
                                snapshot,
                                relative
                        );

                copyFile(
                        file,
                        destination
                );

                copied++;
            }

            JSONObject info =
                    new JSONObject();

            info.put("id", id);

            info.put(
                    "reason",
                    reason == null
                            ? "evolution"
                            : reason
            );

            info.put(
                    "files",
                    copied
            );

            info.put(
                    "created",
                    now()
            );

            writeText(
                    new File(
                            snapshot,
                            "snapshot.json"
                    ),
                    info.toString()
            );

            memoryManager.saveMemory(
                    "__last_snapshot__",
                    id
            );

            recordHistory(
                    "SNAPSHOT",
                    id
                            + " | files="
                            + copied
            );

            return
                    "Snapshot ناجح ✓\n"
                            + "ID: "
                            + id
                            + "\n"
                            + "Files: "
                            + copied;

        } catch (Exception e) {

            return
                    "فشل Snapshot:\n"
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
            return "خاصني ID ديال Snapshot.";
        }

        return rollbackSnapshot(
                snapshotId.trim()
        );
    }

    private String rollbackSnapshot(
            String snapshotId
    ) {

        try {

            File snapshot =
                    new File(
                            snapshots,
                            snapshotId
                    );

            if (!snapshot.exists()) {
                return
                        "Snapshot غير موجود:\n"
                                + snapshotId;
            }

            /*
             * أولا:
             * حذف الملفات الحالية اللي ما كانتش موجودة
             * في Snapshot.
             */
            List<File> currentFiles =
                    collectFiles(workspace);

            int deleted = 0;

            for (File current : currentFiles) {

                if (isInternalFile(current)) {
                    continue;
                }

                String relative =
                        relativePath(current);

                File original =
                        new File(
                                snapshot,
                                relative
                        );

                if (!original.exists()) {

                    if (current.delete()) {
                        deleted++;
                    }
                }
            }

            /*
             * ثانيا:
             * إعادة كل الملفات الموجودة في Snapshot.
             */
            List<File> snapshotFiles =
                    collectFiles(snapshot);

            int restored = 0;

            for (File source : snapshotFiles) {

                if (source.getName()
                        .equals("snapshot.json")) {
                    continue;
                }

                String relative =
                        relativePathFrom(
                                snapshot,
                                source
                        );

                File destination =
                        new File(
                                workspace,
                                relative
                        );

                copyFile(
                        source,
                        destination
                );

                restored++;
            }

            recordHistory(
                    "ROLLBACK",
                    snapshotId
                            + " | restored="
                            + restored
                            + " | deleted="
                            + deleted
            );

            memoryManager.saveMemory(
                    "__last_rollback_snapshot__",
                    snapshotId
            );

            return
                    "Rollback ناجح ✓\n"
                            + "Snapshot: "
                            + snapshotId
                            + "\n"
                            + "Files restored: "
                            + restored
                            + "\n"
                            + "New files removed: "
                            + deleted;

        } catch (Exception e) {

            return
                    "فشل Rollback:\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // BACKUP
    // =========================================================

    private String createBackupAndReturn(
            String path
    ) throws Exception {

        File source =
                safeFile(path);

        if (!source.exists()) {
            return "NONE";
        }

        String id =
                new SimpleDateFormat(
                        "yyyyMMdd_HHmmss_SSS",
                        Locale.US
                ).format(new Date());

        File destination =
                new File(
                        backups,
                        id
                                + "_"
                                + source.getName()
                );

        copyFile(
                source,
                destination
        );

        recordHistory(
                "BACKUP",
                path
                        + " -> "
                        + destination.getName()
        );

        return destination.getAbsolutePath();
    }

    private void restoreBackup(
            String path,
            String backupPath
    ) {

        try {

            if (isBlank(backupPath)
                    || backupPath.equals("NONE")) {
                return;
            }

            File backup =
                    new File(backupPath);

            if (!backup.exists()) {
                return;
            }

            File target =
                    safeFile(path);

            copyFile(
                    backup,
                    target
            );

            recordHistory(
                    "BACKUP_RESTORE",
                    path
            );

        } catch (Exception e) {

            recordHistory(
                    "RESTORE_ERROR",
                    safeError(e)
            );
        }
    }

    // =========================================================
    // CAPABILITIES
    // =========================================================

    public String registerCapability(
            String name,
            String description
    ) {

        if (isBlank(name)) {
            return "خاصك تحدد اسم القدرة.";
        }

        String cleanName =
                name.trim();

        String cleanDescription =
                isBlank(description)
                        ? "Capability created by Self Builder"
                        : description.trim();

        try {

            boolean added =
                    capabilityManager.addCapability(
                            cleanName,
                            cleanDescription
                    );

            if (!added) {
                return "القدرة موجودة مسبقا.";
            }

            recordHistory(
                    "CAPABILITY",
                    cleanName
            );

            return
                    "تم تسجيل القدرة ✓\n\n"
                            + cleanName;

        } catch (Exception e) {

            return
                    "فشل تسجيل القدرة:\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // SKILLS
    // =========================================================

    public String registerSkill(
            String name,
            String description
    ) {

        if (isBlank(name)) {
            return "خاصك تحدد اسم المهارة.";
        }

        String cleanName =
                name.trim();

        String cleanDescription =
                isBlank(description)
                        ? "Skill created by Self Builder"
                        : description.trim();

        try {

            boolean added =
                    skillManager.addSkill(
                            cleanName,
                            cleanDescription
                    );

            if (!added) {
                return "المهارة موجودة مسبقا.";
            }

            recordHistory(
                    "SKILL",
                    cleanName
            );

            return
                    "تم تسجيل المهارة ✓\n\n"
                            + cleanName;

        } catch (Exception e) {

            return
                    "فشل تسجيل المهارة:\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // HISTORY
    // =========================================================

    private synchronized void recordHistory(
            String type,
            String details
    ) {

        try {

            JSONArray history =
                    new JSONArray(
                            readText(historyFile)
                    );

            JSONObject item =
                    new JSONObject();

            item.put(
                    "type",
                    type
            );

            item.put(
                    "details",
                    details == null
                            ? ""
                            : details
            );

            item.put(
                    "time",
                    now()
            );

            history.put(item);

            while (history.length() > 500) {
                history.remove(0);
            }

            writeText(
                    historyFile,
                    history.toString()
            );

        } catch (Exception ignored) {
        }
    }

    public String getBuildHistory() {

        try {
            return readText(historyFile);

        } catch (Exception e) {

            return
                    "تعذر قراءة سجل التطوير.";
        }
    }

    // =========================================================
    // STATUS
    // =========================================================

    public String getBuildStatus() {

        try {

            String target =
                    memoryManager.getMemory(
                            "__builder_target__"
                    );

            return
                    "SELF BUILDER: ONLINE ✓\n"
                            + "Workspace: READY ✓\n"
                            + "Files: "
                            + countProjectFiles()
                            + "\n"
                            + "Target: "
                            + (
                            isBlank(target)
                                    ? "NONE"
                                    : target
                    )
                            + "\n"
                            + "Backup: READY ✓\n"
                            + "Snapshot: READY ✓\n"
                            + "Rollback: READY ✓\n"
                            + "Code Editing: READY ✓\n"
                            + "Text Patch: READY ✓\n"
                            + "Evolution Transaction: READY ✓";

        } catch (Exception e) {

            return
                    "Self Builder: ERROR ⚠\n"
                            + safeError(e);
        }
    }

    public String getStatus() {

        return isHealthy()
                ? "Self Builder Engine: ONLINE ✓"
                : "Self Builder Engine: NEEDS ATTENTION ⚠";
    }

    public boolean isHealthy() {

        try {

            initializeWorkspace();

            if (!workspace.exists()) {
                return false;
            }

            if (!backups.exists()) {
                return false;
            }

            if (!snapshots.exists()) {
                return false;
            }

            if (!historyFile.exists()) {
                return false;
            }

            memoryManager.getMemoryCount();
            skillManager.getSkillCount();
            capabilityManager.getCount();
            taskManager.getTaskCount();

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // PATH SECURITY
    // =========================================================

    private boolean validPath(
            String path
    ) {

        if (isBlank(path)) {
            return false;
        }

        String clean =
                path.trim()
                        .replace(
                                '\\',
                                '/'
                        );

        if (clean.startsWith("/")) {
            return false;
        }

        if (clean.contains("..")) {
            return false;
        }

        if (clean.contains("\0")) {
            return false;
        }

        return true;
    }

    private File safeFile(
            String path
    ) throws Exception {

        String clean =
                path.trim()
                        .replace(
                                '\\',
                                '/'
                        );

        File file =
                new File(
                        workspace,
                        clean
                );

        String root =
                workspace.getCanonicalPath();

        String target =
                file.getCanonicalPath();

        if (!target.equals(root)
                && !target.startsWith(
                root + File.separator
        )) {

            throw new SecurityException(
                    "Path خارج Workspace"
            );
        }

        return file;
    }

    // =========================================================
    // FILE COLLECTION
    // =========================================================

    private List<File> collectFiles(
            File root
    ) {

        List<File> result =
                new ArrayList<>();

        if (root == null ||
                !root.exists()) {
            return result;
        }

        File[] children =
                root.listFiles();

        if (children == null) {
            return result;
        }

        for (File child : children) {

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

    private int countProjectFiles()
            throws Exception {

        int count = 0;

        for (File file :
                collectFiles(workspace)) {

            if (!isInternalFile(file)) {
                count++;
            }
        }

        return count;
    }

    private boolean isInternalFile(
            File file
    ) {

        String path =
                file.getAbsolutePath();

        return path.startsWith(
                backups.getAbsolutePath()
        )
                || path.startsWith(
                snapshots.getAbsolutePath()
        )
                || file.equals(historyFile);
    }

    private boolean isReadableTextFile(
            File file
    ) {

        String name =
                file.getName()
                        .toLowerCase(Locale.ROOT);

        return name.endsWith(".java")
                || name.endsWith(".xml")
                || name.endsWith(".gradle")
                || name.endsWith(".kts")
                || name.endsWith(".properties")
                || name.endsWith(".json")
                || name.endsWith(".txt")
                || name.endsWith(".md")
                || name.endsWith(".pro")
                || name.equals("manifest")
                || name.equals("settings.gradle")
                || name.equals("build.gradle");
    }

    // =========================================================
    // PATH HELPERS
    // =========================================================

    private String relativePath(
            File file
    ) {

        return relativePathFrom(
                workspace,
                file
        );
    }

    private String relativePathFrom(
            File root,
            File file
    ) {

        try {

            String rootPath =
                    root.getCanonicalPath();

            String filePath =
                    file.getCanonicalPath();

            if (filePath.startsWith(
                    rootPath
            )) {

                String value =
                        filePath.substring(
                                rootPath.length()
                        );

                while (
                        value.startsWith(
                                File.separator
                        )
                ) {

                    value =
                            value.substring(1);
                }

                return value;
            }

        } catch (Exception ignored) {
        }

        return file.getName();
    }

    // =========================================================
    // COPY
    // =========================================================

    private void copyFile(
            File source,
            File destination
    ) throws Exception {

        File parent =
                destination.getParentFile();

        if (parent != null &&
                !parent.exists()) {

            parent.mkdirs();
        }

        try (
                InputStream input =
                        new FileInputStream(source);

                OutputStream output =
                        new FileOutputStream(destination)
        ) {

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
        }
    }

    // =========================================================
    // ATOMIC WRITE
    // =========================================================

    private void atomicWrite(
            File target,
            String text
    ) throws Exception {

        File parent =
                target.getParentFile();

        if (parent != null &&
                !parent.exists()) {

            parent.mkdirs();
        }

        File temp =
                new File(
                        target.getAbsolutePath()
                                + ".tmp"
                );

        writeText(
                temp,
                text
        );

        if (target.exists()
                && !target.delete()) {

            temp.delete();

            throw new IllegalStateException(
                    "تعذر استبدال الملف القديم"
            );
        }

        if (!temp.renameTo(target)) {

            temp.delete();

            throw new IllegalStateException(
                    "تعذر إتمام الكتابة الآمنة"
            );
        }
    }

    // =========================================================
    // TEXT IO
    // =========================================================

    private String readText(
            File file
    ) throws Exception {

        FileInputStream input =
                new FileInputStream(file);

        try {

            byte[] data =
                    new byte[
                            (int) file.length()
                    ];

            int offset = 0;

            int read;

            while (
                    offset < data.length
                            && (read =
                            input.read(
                                    data,
                                    offset,
                                    data.length - offset
                            )) > 0
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

    private void writeText(
            File file,
            String text
    ) throws Exception {

        File parent =
                file.getParentFile();

        if (parent != null &&
                !parent.exists()) {

            parent.mkdirs();
        }

        FileOutputStream output =
                new FileOutputStream(file);

        try {

            output.write(
                    text.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            output.flush();

        } finally {

            output.close();
        }
    }

    // =========================================================
    // HASH
    // =========================================================

    private String sha256(
            String value
    ) throws Exception {

        MessageDigest digest =
                MessageDigest.getInstance(
                        "SHA-256"
                );

        byte[] bytes =
                digest.digest(
                        value.getBytes(
                                StandardCharsets.UTF_8
                        )
                );

        return bytesToHex(bytes);
    }

    private String sha256File(
            File file
    ) throws Exception {

        return sha256(
                readText(file)
        );
    }

    private String bytesToHex(
            byte[] bytes
    ) {

        StringBuilder result =
                new StringBuilder();

        for (byte b : bytes) {

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
    // SNAPSHOT ID
    // =========================================================

    private String snapshotIdFromResult(
            String result
    ) {

        if (result == null) {
            return "";
        }

        String marker =
                "ID: ";

        int index =
                result.indexOf(marker);

        if (index < 0) {
            return "";
        }

        String value =
                result.substring(
                        index + marker.length()
                );

        int end =
                value.indexOf("\n");

        if (end >= 0) {
            value =
                    value.substring(
                            0,
                            end
                    );
        }

        return value.trim();
    }

    // =========================================================
    // SAFE TEXT
    // =========================================================

    private String safeText(
            String value
    ) {

        if (isBlank(value)) {
            return "unspecified";
        }

        return value.trim();
    }

    private boolean isBlank(
            String value
    ) {

        return value == null
                || value.trim().isEmpty();
    }

    // =========================================================
    // TIME
    // =========================================================

    private String now() {

        return
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                ).format(
                        new Date()
                );
    }

    // =========================================================
    // ERROR
    // =========================================================

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
}