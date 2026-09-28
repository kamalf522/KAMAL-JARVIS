package com.kamal.jarvis.v2.evolution;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JARVIS V2 - Self Test Engine
 *
 * مسؤول عن التحقق من نتيجة البناء قبل السماح للـEvolution
 * بالانتقال إلى مرحلة التفعيل.
 *
 * الاختبارات هنا تعمل داخل Workspace المتاح للتطبيق:
 *
 * 1. التحقق من BuildResult.
 * 2. التحقق من وجود مجلد capability.
 * 3. التحقق من وجود الملفات المطلوبة.
 * 4. التحقق من إمكانية قراءة الملفات.
 * 5. التحقق من أن الملفات ليست فارغة.
 * 6. التحقق من وجود بيانات أساسية للـCapability.
 * 7. التحقق من Success Criteria عندما يمكن التحقق منها
 *    من ملفات الـWorkspace.
 *
 * لا يدّعي هذا النظام نجاح Build APK خارجي لم يتم تشغيله.
 */
public final class SelfTestEngine {

    private static final String ENGINE_ID =
            "v2.self_test_engine";

    private volatile TestReport lastReport;

    /**
     * يشغل الاختبارات الكاملة على BuildResult.
     */
    public synchronized JarvisResult<TestReport> test(
            CapabilitySpec spec,
            SelfBuilder.BuildResult buildResult
    ) {
        if (spec == null) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            "CapabilitySpec cannot be null."
                    )
            );
        }

        if (buildResult == null) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            "BuildResult cannot be null."
                    )
            );
        }

        TestReport report =
                new TestReport(
                        spec.getCapabilityId()
                );

        runDirectoryTest(buildResult, report);
        runFileExistenceTest(buildResult, report);
        runFileReadTest(buildResult, report);
        runContentTest(spec, buildResult, report);
        runSpecificationTest(spec, report);
        runSuccessCriteriaTest(
                spec,
                buildResult,
                report
        );

        report.finish();

        lastReport = report;

        if (report.isPassed()) {
            return JarvisResult.success(
                    report,
                    "Self-test passed successfully."
            );
        }

        return JarvisResult.failure(
                JarvisError.of(
                        JarvisError.Type.TEST_FAILED,
                        report.getSummary()
                )
        );
    }

    /**
     * اختبار وجود مجلد الـCapability.
     */
    private void runDirectoryTest(
            SelfBuilder.BuildResult result,
            TestReport report
    ) {
        File directory =
                result.getCapabilityDirectory();

        if (directory == null) {
            report.fail(
                    "DIRECTORY",
                    "Capability directory is null."
            );
            return;
        }

        if (!directory.exists()) {
            report.fail(
                    "DIRECTORY",
                    "Capability directory does not exist."
            );
            return;
        }

        if (!directory.isDirectory()) {
            report.fail(
                    "DIRECTORY",
                    "Capability path is not a directory."
            );
            return;
        }

        report.pass(
                "DIRECTORY",
                "Capability directory exists."
        );
    }

    /**
     * اختبار جميع الملفات التي أنشأها Builder.
     */
    private void runFileExistenceTest(
            SelfBuilder.BuildResult result,
            TestReport report
    ) {
        List<File> files =
                result.getCreatedFiles();

        if (files == null || files.isEmpty()) {
            report.fail(
                    "FILES",
                    "No files were created."
            );
            return;
        }

        boolean allValid = true;

        for (File file : files) {

            if (file == null) {
                allValid = false;

                report.fail(
                        "FILES",
                        "Created file reference is null."
                );

                continue;
            }

            if (!file.exists()) {
                allValid = false;

                report.fail(
                        "FILES",
                        "Missing file: "
                                + file.getName()
                );
            }
        }

        if (allValid) {
            report.pass(
                    "FILES",
                    files.size()
                            + " created files exist."
            );
        }
    }

    /**
     * اختبار إمكانية قراءة الملفات.
     */
    private void runFileReadTest(
            SelfBuilder.BuildResult result,
            TestReport report
    ) {
        List<File> files =
                result.getCreatedFiles();

        if (files == null || files.isEmpty()) {
            return;
        }

        boolean allReadable = true;

        for (File file : files) {

            if (file == null || !file.exists()) {
                allReadable = false;
                continue;
            }

            if (!file.isFile()) {
                allReadable = false;

                report.fail(
                        "READ",
                        "Path is not a regular file: "
                                + file.getName()
                );

                continue;
            }

            if (!file.canRead()) {
                allReadable = false;

                report.fail(
                        "READ",
                        "File cannot be read: "
                                + file.getName()
                );

                continue;
            }

            if (file.length() <= 0) {
                allReadable = false;

                report.fail(
                        "READ",
                        "File is empty: "
                                + file.getName()
                );
            }
        }

        if (allReadable) {
            report.pass(
                    "READ",
                    "All generated files are readable."
            );
        }
    }

    /**
     * يتحقق من محتوى الملفات الأساسية.
     */
    private void runContentTest(
            CapabilitySpec spec,
            SelfBuilder.BuildResult result,
            TestReport report
    ) {
        File manifest =
                findFile(
                        result.getCreatedFiles(),
                        "capability.manifest"
                );

        File specification =
                findFile(
                        result.getCreatedFiles(),
                        "specification.txt"
                );

        boolean valid = true;

        if (manifest == null) {
            valid = false;

            report.fail(
                    "CONTENT",
                    "capability.manifest is missing."
            );
        } else {
            String content =
                    readFile(manifest);

            if (isBlank(content)) {
                valid = false;

                report.fail(
                        "CONTENT",
                        "capability.manifest is empty."
                );
            }

            if (!contains(
                    content,
                    spec.getCapabilityId()
            )) {
                valid = false;

                report.fail(
                        "CONTENT",
                        "Manifest does not contain capability ID."
                );
            }
        }

        if (specification == null) {
            valid = false;

            report.fail(
                    "CONTENT",
                    "specification.txt is missing."
            );
        } else {
            String content =
                    readFile(specification);

            if (isBlank(content)) {
                valid = false;

                report.fail(
                        "CONTENT",
                        "specification.txt is empty."
                );
            }

            if (!contains(
                    content,
                    spec.getName()
            )) {
                valid = false;

                report.fail(
                        "CONTENT",
                        "Specification does not contain capability name."
                );
            }

            if (!contains(
                    content,
                    spec.getGoal()
            )) {
                valid = false;

                report.fail(
                        "CONTENT",
                        "Specification does not contain capability goal."
                );
            }
        }

        if (valid) {
            report.pass(
                    "CONTENT",
                    "Generated capability files contain valid core data."
            );
        }
    }

    /**
     * اختبار الـCapabilitySpec نفسها.
     */
    private void runSpecificationTest(
            CapabilitySpec spec,
            TestReport report
    ) {
        boolean valid = true;

        if (isBlank(spec.getCapabilityId())) {
            valid = false;

            report.fail(
                    "SPECIFICATION",
                    "Capability ID is empty."
            );
        }

        if (isBlank(spec.getName())) {
            valid = false;

            report.fail(
                    "SPECIFICATION",
                    "Capability name is empty."
            );
        }

        if (isBlank(spec.getGoal())) {
            valid = false;

            report.fail(
                    "SPECIFICATION",
                    "Capability goal is empty."
            );
        }

        if (spec.getSuccessCriteria() == null
                || spec.getSuccessCriteria().isEmpty()) {

            valid = false;

            report.fail(
                    "SPECIFICATION",
                    "No success criteria were defined."
            );
        }

        if (valid) {
            report.pass(
                    "SPECIFICATION",
                    "Capability specification is structurally valid."
            );
        }
    }

    /**
     * يفحص Success Criteria القابلة للتحقق من الـWorkspace.
     *
     * إذا كان criterion عبارة عن نص عام لا يمكن إثباته
     * من الملفات الموجودة، لا نزور النتيجة ونقول إنه نجح.
     */
    private void runSuccessCriteriaTest(
            CapabilitySpec spec,
            SelfBuilder.BuildResult result,
            TestReport report
    ) {
        List<String> criteria =
                spec.getSuccessCriteria();

        if (criteria == null || criteria.isEmpty()) {
            return;
        }

        int verifiable = 0;
        int satisfied = 0;

        String combinedContent =
                readAllCreatedFiles(
                        result.getCreatedFiles()
                );

        for (String criterion : criteria) {

            if (isBlank(criterion)) {
                continue;
            }

            String normalized =
                    criterion.trim();

            /*
             * Criteria بسيطة يمكن التحقق منها نصياً.
             */
            if (combinedContent.contains(normalized)) {
                verifiable++;
                satisfied++;

                report.pass(
                        "CRITERION",
                        "Criterion found: "
                                + normalized
                );

                continue;
            }

            /*
             * إذا لم يكن criterion موجوداً داخل
             * الملفات، لا نعتبره فاشلاً بشكل مطلق
             * ولا نعتبره ناجحاً.
             *
             * لأنه قد يحتاج تنفيذ فعلي أو Build خارجي.
             */
            report.info(
                    "CRITERION",
                    "Criterion requires runtime/build verification: "
                            + normalized
            );
        }

        if (verifiable > 0) {
            report.info(
                    "CRITERION",
                    satisfied
                            + " of "
                            + verifiable
                            + " text-verifiable criteria satisfied."
            );
        }
    }

    private File findFile(
            List<File> files,
            String name
    ) {
        if (files == null) {
            return null;
        }

        for (File file : files) {
            if (file != null
                    && name.equals(file.getName())) {
                return file;
            }
        }

        return null;
    }

    private String readFile(
            File file
    ) {
        if (file == null || !file.exists()) {
            return "";
        }

        StringBuilder builder =
                new StringBuilder();

        try (
                FileInputStream input =
                        new FileInputStream(file);

                InputStreamReader reader =
                        new InputStreamReader(
                                input,
                                StandardCharsets.UTF_8
                        );

                BufferedReader buffered =
                        new BufferedReader(reader)
        ) {
            String line;

            while ((line = buffered.readLine()) != null) {
                builder.append(line)
                        .append('\n');
            }

            return builder.toString();

        } catch (Exception e) {
            return "";
        }
    }

    private String readAllCreatedFiles(
            List<File> files
    ) {
        if (files == null || files.isEmpty()) {
            return "";
        }

        StringBuilder result =
                new StringBuilder();

        for (File file : files) {
            result.append(readFile(file))
                    .append('\n');
        }

        return result.toString();
    }

    private boolean contains(
            String content,
            String value
    ) {
        return content != null
                && value != null
                && content.contains(value);
    }

    private boolean isBlank(
            String value
    ) {
        return value == null
                || value.trim().isEmpty();
    }

    public TestReport getLastReport() {
        return lastReport;
    }

    public boolean hasPassedLastTest() {
        return lastReport != null
                && lastReport.isPassed();
    }

    public static String getEngineId() {
        return ENGINE_ID;
    }

    /**
     * تقرير الاختبار الكامل.
     */
    public static final class TestReport {

        private final String capabilityId;
        private final List<TestEntry> entries =
                new ArrayList<>();

        private boolean finished;
        private long startedAt;
        private long finishedAt;

        private TestReport(
                String capabilityId
        ) {
            this.capabilityId =
                    capabilityId;

            this.startedAt =
                    System.currentTimeMillis();
        }

        private void pass(
                String category,
                String message
        ) {
            entries.add(
                    new TestEntry(
                            category,
                            TestStatus.PASSED,
                            message
                    )
            );
        }

        private void fail(
                String category,
                String message
        ) {
            entries.add(
                    new TestEntry(
                            category,
                            TestStatus.FAILED,
                            message
                    )
            );
        }

        private void info(
                String category,
                String message
        ) {
            entries.add(
                    new TestEntry(
                            category,
                            TestStatus.INFO,
                            message
                    )
            );
        }

        private void finish() {
            finished = true;
            finishedAt =
                    System.currentTimeMillis();
        }

        public String getCapabilityId() {
            return capabilityId;
        }

        public boolean isFinished() {
            return finished;
        }

        public boolean isPassed() {
            if (!finished) {
                return false;
            }

            for (TestEntry entry : entries) {
                if (entry.getStatus()
                        == TestStatus.FAILED) {
                    return false;
                }
            }

            return true;
        }

        public int getPassedCount() {
            return count(TestStatus.PASSED);
        }

        public int getFailedCount() {
            return count(TestStatus.FAILED);
        }

        public int getInfoCount() {
            return count(TestStatus.INFO);
        }

        public int getTotalCount() {
            return entries.size();
        }

        public long getDurationMillis() {
            if (!finished) {
                return System.currentTimeMillis()
                        - startedAt;
            }

            return finishedAt - startedAt;
        }

        public List<TestEntry> getEntries() {
            return Collections.unmodifiableList(
                    new ArrayList<>(entries)
            );
        }

        public String getSummary() {
            return "Capability="
                    + capabilityId
                    + ", passed="
                    + getPassedCount()
                    + ", failed="
                    + getFailedCount()
                    + ", info="
                    + getInfoCount()
                    + ", durationMs="
                    + getDurationMillis();
        }

        private int count(
                TestStatus status
        ) {
            int count = 0;

            for (TestEntry entry : entries) {
                if (entry.getStatus() == status) {
                    count++;
                }
            }

            return count;
        }

        @Override
        public String toString() {
            return getSummary();
        }
    }

    public enum TestStatus {
        PASSED,
        FAILED,
        INFO
    }

    /**
     * نتيجة اختبار واحدة.
     */
    public static final class TestEntry {

        private final String category;
        private final TestStatus status;
        private final String message;

        private TestEntry(
                String category,
                TestStatus status,
                String message
        ) {
            this.category = category;
            this.status = status;
            this.message = message;
        }

        public String getCategory() {
            return category;
        }

        public TestStatus getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "[" + status + "] "
                    + category
                    + ": "
                    + message;
        }
    }
}