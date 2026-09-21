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
 * JARVIS Self Builder Engine
 *
 * المرحلة الأولى من نظام التطور الذاتي الحقيقي.
 *
 * القدرات:
 * - Workspace مستقل داخل تخزين JARVIS
 * - إنشاء وقراءة ملفات المشروع
 * - تعديل الملفات
 * - Backup قبل التعديل
 * - Snapshots
 * - Rollback
 * - Hash للتحقق من التغييرات
 * - البحث داخل ملفات المشروع
 * - تتبع آخر التغييرات
 *
 * ملاحظة:
 * هذا النظام يعدل نسخة Workspace، وليس APK المثبت مباشرة.
 * بناء APK وتحديثه سيتم ربطهما في المرحلة التالية.
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

    private void initializeWorkspace() {

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

                writeText(
                        historyFile,
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

    public String initialize() {

        initializeWorkspace();

        return
                "JARVIS SELF BUILDER\n"
                + "============================\n"
                + "Workspace: ONLINE ✓\n"
                + "المسار:\n"
                + workspace.getAbsolutePath();
    }

    public String getWorkspacePath() {

        return workspace.getAbsolutePath();
    }

    // =========================================================
    // DEVELOPMENT TARGET
    // =========================================================

    public String registerDevelopment(
            String systemName,
            String description
    ) {

        if (systemName == null ||
                systemName.trim().isEmpty()) {

            return
                    "خاصك تحدد اسم النظام اللي بغيتي نطورو.";
        }

        String name =
                systemName.trim();

        String details =
                description == null ||
                        description.trim().isEmpty()
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

        if (target == null ||
                target.trim().isEmpty()) {

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

    public String createBuildPlan(
            String systemName
    ) {

        if (systemName == null ||
                systemName.trim().isEmpty()) {

            return
                    "اسم النظام غير موجود.";
        }

        String name =
                systemName.trim();

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
                "إنشاء Backup"
        );

        taskManager.addTask(
                "تطبيق التعديل"
        );

        taskManager.addTask(
                "التحقق من التغيير"
        );

        taskManager.addTask(
                "اختبار النظام"
        );

        taskManager.addTask(
                "Rollback إذا فشل الاختبار"
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
                "تم إنشاء خطة التطوير الحقيقية ✓\n\n"
                + "النظام: "
                + name
                + "\n\n"
                + taskManager.getTasks();
    }

    // =========================================================
    // SOURCE FILE MANAGEMENT
    // =========================================================

    /**
     * يحفظ ملف داخل Workspace.
     *
     * path مثال:
     * app/src/main/java/com/kamal/jarvis/Test.java
     */
    public String writeSourceFile(
            String path,
            String content
    ) {

        if (!validPath(path)) {

            return
                    "مسار الملف غير صالح.";
        }

        if (content == null) {
            content = "";
        }

        try {

            File target =
                    safeFile(path);

            if (target.exists()) {

                createBackup(path);
            }

            File parent =
                    target.getParentFile();

            if (parent != null &&
                    !parent.exists()) {

                parent.mkdirs();
            }

            writeText(
                    target,
                    content
            );

            String hash =
                    sha256(
                            content
                    );

            recordHistory(
                    "WRITE",
                    path
                            + " | SHA256="
                            + hash
            );

            return
                    "تم تعديل الملف ✓\n"
                    + path
                    + "\n\nSHA-256:\n"
                    + hash;

        } catch (Exception e) {

            return
                    "فشل تعديل الملف:\n"
                    + safeError(e);
        }
    }

    public String readSourceFile(
            String path
    ) {

        if (!validPath(path)) {

            return
                    "مسار الملف غير صالح.";
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

    /**
     * يستعمل لاستيراد نسخة من ملف المصدر إلى Workspace.
     */
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
    // FILE INFORMATION
    // =========================================================

    public String getFileHash(
            String path
    ) {

        if (!validPath(path)) {

            return
                    "مسار الملف غير صالح.";
        }

        try {

            File file =
                    safeFile(path);

            if (!file.exists()) {

                return
                        "الملف غير موجود.";
            }

            return
                    sha256(
                            readText(file)
                    );

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

        if (query == null ||
                query.trim().isEmpty()) {

            return
                    "خاصني كلمة أو جملة للبحث.";
        }

        String search =
                query.toLowerCase(
                        Locale.getDefault()
                );

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

                String text =
                        readText(file);

                if (text.toLowerCase(
                        Locale.getDefault()
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
    // LIST PROJECT FILES
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

                result.append(
                        count
                )
                        .append(". ")
                        .append(
                                relativePath(file)
                        )
                        .append("\n");
            }

            if (count == 0) {

                return
                        "Workspace فارغ حاليا.";
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

    public String createSnapshot(
            String reason
    ) {

        try {

            String id =
                    new SimpleDateFormat(
                            "yyyyMMdd_HHmmss_SSS",
                            Locale.US
                    ).format(
                            new Date()
                    );

            File snapshot =
                    new File(
                            snapshots,
                            id
                    );

            snapshot.mkdirs();

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

                File parent =
                        destination.getParentFile();

                if (parent != null) {
                    parent.mkdirs();
                }

                copyFile(
                        file,
                        destination
                );

                copied++;
            }

            JSONObject info =
                    new JSONObject();

            info.put(
                    "id",
                    id
            );

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

            recordHistory(
                    "SNAPSHOT",
                    id
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

    public String rollback(
            String snapshotId
    ) {

        if (snapshotId == null ||
                snapshotId.trim().isEmpty()) {

            return
                    "خاصني ID ديال Snapshot.";
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

            List<File> files =
                    collectFiles(snapshot);

            int restored = 0;

            for (File source : files) {

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

                File parent =
                        destination.getParentFile();

                if (parent != null) {
                    parent.mkdirs();
                }

                copyFile(
                        source,
                        destination
                );

                restored++;
            }

            recordHistory(
                    "ROLLBACK",
                    snapshotId
            );

            return
                    "Rollback ناجح ✓\n"
                    + "Snapshot: "
                    + snapshotId
                    + "\n"
                    + "Files restored: "
                    + restored;

        } catch (Exception e) {

            return
                    "فشل Rollback:\n"
                    + safeError(e);
        }
    }

    // =========================================================
    // BACKUP
    // =========================================================

    private void createBackup(
            String path
    ) {

        try {

            File source =
                    safeFile(path);

            if (!source.exists()) {
                return;
            }

            String id =
                    new SimpleDateFormat(
                            "yyyyMMdd_HHmmss_SSS",
                            Locale.US
                    ).format(
                            new Date()
                    );

            File destination =
                    new File(
                            backups,
                            id
                                    + "_"
                                    + new File(path)
                                    .getName()
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

        } catch (Exception e) {

            recordHistory(
                    "BACKUP_ERROR",
                    safeError(e)
            );
        }
    }

    // =========================================================
    // DEVELOPMENT CAPABILITIES
    // =========================================================

    public String registerCapability(
            String name,
            String description
    ) {

        if (name == null ||
                name.trim().isEmpty()) {

            return
                    "خاصك تحدد اسم القدرة.";
        }

        String cleanName =
                name.trim();

        String cleanDescription =
                description == null ||
                        description.trim().isEmpty()
                        ? "Capability created by Self Builder"
                        : description.trim();

        try {

            boolean added =
                    capabilityManager.addCapability(
                            cleanName,
                            cleanDescription
                    );

            if (added) {

                recordHistory(
                        "CAPABILITY",
                        cleanName
                );

                return
                        "تم تسجيل القدرة ✓\n\n"
                        + cleanName;
            }

            return
                    "القدرة موجودة مسبقا.";

        } catch (Exception e) {

            return
                    "فشل تسجيل القدرة:\n"
                    + safeError(e);
        }
    }

    public String registerSkill(
            String name,
            String description
    ) {

        if (name == null ||
                name.trim().isEmpty()) {

            return
                    "خاصك تحدد اسم المهارة.";
        }

        String cleanName =
                name.trim();

        String cleanDescription =
                description == null ||
                        description.trim().isEmpty()
                        ? "Skill created by Self Builder"
                        : description.trim();

        try {

            boolean added =
                    skillManager.addSkill(
                            cleanName,
                            cleanDescription
                    );

            if (added) {

                recordHistory(
                        "SKILL",
                        cleanName
                );

                return
                        "تم تسجيل المهارة ✓\n\n"
                        + cleanName;
            }

            return
                    "المهارة موجودة مسبقا.";

        } catch (Exception e) {

            return
                    "فشل تسجيل المهارة:\n"
                    + safeError(e);
        }
    }

    // =========================================================
    // HISTORY
    // =========================================================

    private void recordHistory(
            String type,
            String details
    ) {

        try {

            JSONArray history =
                    new JSONArray(
                            readText(
                                    historyFile
                            )
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

            /*
             * نخلي الذاكرة محدودة باش ما يكبرش الملف
             * بلا نهاية.
             */
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

            return
                    readText(
                            historyFile
                    );

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

            int files =
                    countProjectFiles();

            return
                    "SELF BUILDER: ONLINE ✓\n"
                    + "Workspace: READY ✓\n"
                    + "Files: "
                    + files
                    + "\n"
                    + "Target: "
                    + (
                    target == null
                            ? "NONE"
                            : target
                    )
                    + "\n"
                    + "Backup: READY ✓\n"
                    + "Snapshot: READY ✓\n"
                    + "Rollback: READY ✓\n"
                    + "Code Editing: READY ✓\n"
                    + "APK Builder: NEXT PHASE";

        } catch (Exception e) {

            return
                    "Self Builder: ERROR ⚠\n"
                    + safeError(e);
        }
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

            memoryManager.getMemoryCount();
            skillManager.getSkillCount();
            capabilityManager.getCount();
            taskManager.getTaskCount();

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    public String getStatus() {

        if (isHealthy()) {

            return
                    "Self Builder Engine: ONLINE ✓";

        }

        return
                "Self Builder Engine: NEEDS ATTENTION ⚠";
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private boolean validPath(
            String path
    ) {

        if (path == null ||
                path.trim().isEmpty()) {

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
                workspace
                        .getCanonicalPath();

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

    private List<File> collectFiles(
            File root
    ) {

        List<File> result =
                new ArrayList<>();

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

        String rootPath =
                root.getAbsolutePath();

        String filePath =
                file.getAbsolutePath();

        if (filePath.startsWith(
                rootPath
        )) {

            String value =
                    filePath.substring(
                            rootPath.length()
                    );

            while (value.startsWith(
                    File.separator
            )) {

                value =
                        value.substring(1);
            }

            return value;
        }

        return file.getName();
    }

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
                        new FileInputStream(
                                source
                        );

                OutputStream output =
                        new FileOutputStream(
                                destination
                        )
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
                                    data.length
                                            - offset
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

        } finally {

            output.close();
        }
    }

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

    private String now() {

        return
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                ).format(
                        new Date()
                );
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
}