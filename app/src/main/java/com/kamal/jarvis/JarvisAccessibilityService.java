package com.kamal.jarvis;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class JarvisAccessibilityService
        extends AccessibilityService {

    private static volatile JarvisAccessibilityService instance;

    private volatile String lastPackageName = "";
    private volatile String lastScreenText = "";

    private static final int MAX_TREE_DEPTH = 30;
    private static final int MAX_SCREEN_LINES = 500;

    // =========================================================
    // SERVICE
    // =========================================================

    @Override
    protected void onServiceConnected() {

        super.onServiceConnected();

        instance = this;

        AccessibilityServiceInfo info =
                new AccessibilityServiceInfo();

        info.eventTypes =
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                        | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                        | AccessibilityEvent.TYPE_VIEW_CLICKED
                        | AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                        | AccessibilityEvent.TYPE_VIEW_SCROLLED
                        | AccessibilityEvent.TYPE_WINDOWS_CHANGED;

        info.feedbackType =
                AccessibilityServiceInfo.FEEDBACK_GENERIC;

        info.flags =
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                        | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                        | AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY;

        info.notificationTimeout = 100;

        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(
            AccessibilityEvent event
    ) {

        if (event == null) {
            return;
        }

        CharSequence packageName =
                event.getPackageName();

        if (packageName != null) {

            lastPackageName =
                    packageName.toString();
        }

        AccessibilityNodeInfo root =
                getRootInActiveWindow();

        if (root == null) {
            return;
        }

        try {

            lastScreenText =
                    extractScreenText(root);

        } finally {

            root.recycle();
        }
    }

    @Override
    public void onInterrupt() {

        lastScreenText = "";
    }

    @Override
    public void onDestroy() {

        if (instance == this) {
            instance = null;
        }

        lastScreenText = "";
        lastPackageName = "";

        super.onDestroy();
    }

    // =========================================================
    // INSTANCE
    // =========================================================

    public static JarvisAccessibilityService
    getInstance() {

        return instance;
    }

    public boolean isConnected() {

        return instance == this;
    }

    public String getCurrentPackage() {

        return lastPackageName;
    }

    // =========================================================
    // SCREEN TEXT
    // =========================================================

    public String getScreenText() {

        AccessibilityNodeInfo root =
                getRootInActiveWindow();

        if (root == null) {

            return
                    "JARVIS: ما قدرتش نوصل للشاشة الحالية.";
        }

        try {

            lastScreenText =
                    extractScreenText(root);

        } finally {

            root.recycle();
        }

        if (lastScreenText == null ||
                lastScreenText.trim().isEmpty()) {

            return
                    "JARVIS: الشاشة الحالية ما فيهاش نص واضح.";
        }

        return lastScreenText;
    }

    // =========================================================
    // SCREEN TREE
    // =========================================================

    public String getScreenTree() {

        AccessibilityNodeInfo root =
                getRootInActiveWindow();

        if (root == null) {

            return
                    "JARVIS: ما كايناش شاشة نشطة.";
        }

        StringBuilder result =
                new StringBuilder();

        result.append(
                "JARVIS SCREEN TREE\n"
        );

        result.append(
                "=================\n\n"
        );

        try {

            buildNodeTree(
                    root,
                    result,
                    0
            );

        } finally {

            root.recycle();
        }

        return result.toString();
    }

    private void buildNodeTree(
            AccessibilityNodeInfo node,
            StringBuilder result,
            int depth
    ) {

        if (node == null ||
                result == null ||
                depth > MAX_TREE_DEPTH) {

            return;
        }

        for (int i = 0;
             i < depth;
             i++) {

            result.append("  ");
        }

        CharSequence text =
                node.getText();

        CharSequence description =
                node.getContentDescription();

        CharSequence className =
                node.getClassName();

        result.append("- ");

        if (hasText(text)) {

            result.append("text=\"")
                    .append(text)
                    .append("\" ");
        }

        if (hasText(description)) {

            result.append("description=\"")
                    .append(description)
                    .append("\" ");
        }

        if (className != null) {

            result.append("type=")
                    .append(className)
                    .append(" ");
        }

        if (node.isClickable()) {
            result.append("[CLICKABLE] ");
        }

        if (node.isEditable()) {
            result.append("[EDITABLE] ");
        }

        if (node.isScrollable()) {
            result.append("[SCROLLABLE] ");
        }

        if (node.isEnabled()) {
            result.append("[ENABLED] ");
        }

        result.append("\n");

        int childCount =
                node.getChildCount();

        for (int i = 0;
             i < childCount;
             i++) {

            AccessibilityNodeInfo child =
                    node.getChild(i);

            if (child == null) {
                continue;
            }

            try {

                buildNodeTree(
                        child,
                        result,
                        depth + 1
                );

            } finally {

                child.recycle();
            }
        }
    }

    // =========================================================
    // EXTRACT SCREEN TEXT
    // =========================================================

    private String extractScreenText(
            AccessibilityNodeInfo node
    ) {

        if (node == null) {
            return "";
        }

        List<String> lines =
                new ArrayList<>();

        collectText(
                node,
                lines
        );

        StringBuilder result =
                new StringBuilder();

        for (String line : lines) {

            if (line == null) {
                continue;
            }

            String clean =
                    line.trim();

            if (clean.isEmpty()) {
                continue;
            }

            if (!containsLine(
                    result,
                    clean
            )) {

                if (result.length() > 0) {
                    result.append("\n");
                }

                result.append(clean);
            }
        }

        return result.toString();
    }

    private void collectText(
            AccessibilityNodeInfo node,
            List<String> lines
    ) {

        if (node == null ||
                lines == null ||
                lines.size() >= MAX_SCREEN_LINES) {

            return;
        }

        CharSequence text =
                node.getText();

        if (hasText(text)) {

            lines.add(
                    text.toString()
            );
        }

        if (lines.size() >= MAX_SCREEN_LINES) {
            return;
        }

        CharSequence description =
                node.getContentDescription();

        if (hasText(description)) {

            lines.add(
                    description.toString()
            );
        }

        if (lines.size() >= MAX_SCREEN_LINES) {
            return;
        }

        int childCount =
                node.getChildCount();

        for (int i = 0;
             i < childCount;
             i++) {

            AccessibilityNodeInfo child =
                    node.getChild(i);

            if (child == null) {
                continue;
            }

            try {

                collectText(
                        child,
                        lines
                );

            } finally {

                child.recycle();
            }

            if (lines.size() >= MAX_SCREEN_LINES) {
                return;
            }
        }
    }

    private boolean containsLine(
            StringBuilder result,
            String line
    ) {

        if (result == null ||
                line == null) {

            return false;
        }

        String[] existing =
                result.toString()
                        .split("\n");

        for (String item : existing) {

            if (item.equals(line)) {
                return true;
            }
        }

        return false;
    }

    // =========================================================
    // CLICK BY TEXT
    // =========================================================

    public boolean clickByText(
            String target
    ) {

        if (target == null ||
                target.trim().isEmpty()) {

            return false;
        }

        AccessibilityNodeInfo root =
                getRootInActiveWindow();

        if (root == null) {
            return false;
        }

        try {

            return clickNodeByText(
                    root,
                    target.trim()
            );

        } finally {

            root.recycle();
        }
    }

    private boolean clickNodeByText(
            AccessibilityNodeInfo node,
            String target
    ) {

        if (node == null ||
                target == null) {

            return false;
        }

        CharSequence text =
                node.getText();

        CharSequence description =
                node.getContentDescription();

        if (matches(
                text,
                target
        ) || matches(
                description,
                target
        )) {

            if (node.isEnabled() &&
                    node.isClickable()) {

                if (node.performAction(
                        AccessibilityNodeInfo
                                .ACTION_CLICK
                )) {

                    return true;
                }
            }

            AccessibilityNodeInfo parent =
                    node.getParent();

            if (parent != null) {

                try {

                    if (parent.isEnabled() &&
                            parent.isClickable()) {

                        if (parent.performAction(
                                AccessibilityNodeInfo
                                        .ACTION_CLICK
                        )) {

                            return true;
                        }
                    }

                } finally {

                    parent.recycle();
                }
            }
        }

        int childCount =
                node.getChildCount();

        for (int i = 0;
             i < childCount;
             i++) {

            AccessibilityNodeInfo child =
                    node.getChild(i);

            if (child == null) {
                continue;
            }

            try {

                if (clickNodeByText(
                        child,
                        target
                )) {

                    return true;
                }

            } finally {

                child.recycle();
            }
        }

        return false;
    }

    // =========================================================
    // FIND NODE
    // =========================================================

    public String getNodeInfoByText(
            String target
    ) {

        if (target == null ||
                target.trim().isEmpty()) {

            return
                    "JARVIS: خاصني النص اللي نقلب عليه.";
        }

        AccessibilityNodeInfo root =
                getRootInActiveWindow();

        if (root == null) {

            return
                    "JARVIS: ما كايناش شاشة نشطة.";
        }

        AccessibilityNodeInfo node = null;

        try {

            node =
                    findNodeByText(
                            root,
                            target.trim()
                    );

            if (node == null) {

                return
                        "JARVIS: ما لقيتش العنصر: "
                                + target;
            }

            Rect bounds =
                    new Rect();

            node.getBoundsInScreen(
                    bounds
            );

            return
                    "العنصر موجود ✓\n"
                    + "النص: "
                    + safeCharSequence(
                            node.getText()
                    )
                    + "\n"
                    + "الوصف: "
                    + safeCharSequence(
                            node.getContentDescription()
                    )
                    + "\n"
                    + "النوع: "
                    + safeCharSequence(
                            node.getClassName()
                    )
                    + "\n"
                    + "قابل للضغط: "
                    + node.isClickable()
                    + "\n"
                    + "قابل للكتابة: "
                    + node.isEditable()
                    + "\n"
                    + "قابل للتمرير: "
                    + node.isScrollable()
                    + "\n"
                    + "مفعل: "
                    + node.isEnabled()
                    + "\n"
                    + "الموقع: "
                    + bounds.toShortString();

        } finally {

            if (node != null) {
                node.recycle();
            }

            root.recycle();
        }
    }

    private AccessibilityNodeInfo findNodeByText(
            AccessibilityNodeInfo node,
            String target
    ) {

        if (node == null ||
                target == null) {

            return null;
        }

        if (matches(
                node.getText(),
                target
        ) || matches(
                node.getContentDescription(),
                target
        )) {

            return node;
        }

        int childCount =
                node.getChildCount();

        for (int i = 0;
             i < childCount;
             i++) {

            AccessibilityNodeInfo child =
                    node.getChild(i);

            if (child == null) {
                continue;
            }

            AccessibilityNodeInfo found =
                    findNodeByText(
                            child,
                            target
                    );

            if (found != null) {

                child.recycle();

                return found;
            }

            child.recycle();
        }

        return null;
    }

    // =========================================================
    // TYPE TEXT
    // =========================================================

    public boolean typeText(
            String target,
            String text
    ) {

        if (text == null) {
            return false;
        }

        AccessibilityNodeInfo root =
                getRootInActiveWindow();

        if (root == null) {
            return false;
        }

        AccessibilityNodeInfo node = null;

        try {

            if (target == null ||
                    target.trim().isEmpty()) {

                node =
                        findFocusedEditableNode(
                                root
                        );

            } else {

                node =
                        findEditableNode(
                                root,
                                target.trim()
                        );
            }

            if (node == null) {
                return false;
            }

            if (!node.isEnabled() ||
                    !node.isEditable()) {

                return false;
            }

            Bundle arguments =
                    new Bundle();

            arguments.putCharSequence(
                    AccessibilityNodeInfo
                            .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text
            );

            return node.performAction(
                    AccessibilityNodeInfo
                            .ACTION_SET_TEXT,
                    arguments
            );

        } finally {

            if (node != null) {
                node.recycle();
            }

            root.recycle();
        }
    }

    private AccessibilityNodeInfo findEditableNode(
            AccessibilityNodeInfo node,
            String target
    ) {

        if (node == null ||
                target == null) {

            return null;
        }

        CharSequence nodeText =
                node.getText();

        CharSequence description =
                node.getContentDescription();

        CharSequence hint = null;

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            hint =
                    node.getHintText();
        }

        if (node.isEditable() &&
                node.isEnabled()) {

            if (matches(
                    nodeText,
                    target
            ) || matches(
                    description,
                    target
            ) || matches(
                    hint,
                    target
            )) {

                return node;
            }
        }

        int childCount =
                node.getChildCount();

        for (int i = 0;
             i < childCount;
             i++) {

            AccessibilityNodeInfo child =
                    node.getChild(i);

            if (child == null) {
                continue;
            }

            AccessibilityNodeInfo result =
                    findEditableNode(
                            child,
                            target
                    );

            if (result != null) {

                child.recycle();

                return result;
            }

            child.recycle();
        }

        return null;
    }

    private AccessibilityNodeInfo
    findFocusedEditableNode(
            AccessibilityNodeInfo node
    ) {

        if (node == null) {
            return null;
        }

        if (node.isEditable() &&
                node.isEnabled() &&
                node.isFocused()) {

            return node;
        }

        int childCount =
                node.getChildCount();

        for (int i = 0;
             i < childCount;
             i++) {

            AccessibilityNodeInfo child =
                    node.getChild(i);

            if (child == null) {
                continue;
            }

            AccessibilityNodeInfo result =
                    findFocusedEditableNode(
                            child
                    );

            if (result != null) {

                child.recycle();

                return result;
            }

            child.recycle();
        }

        return null;
    }

    // =========================================================
    // SCROLL
    // =========================================================

    public boolean scrollForward() {

        return scroll(
                AccessibilityNodeInfo
                        .ACTION_SCROLL_FORWARD
        );
    }

    public boolean scrollBackward() {

        return scroll(
                AccessibilityNodeInfo
                        .ACTION_SCROLL_BACKWARD
        );
    }

    private boolean scroll(
            int action
    ) {

        AccessibilityNodeInfo root =
                getRootInActiveWindow();

        if (root == null) {
            return false;
        }

        try {

            return performScroll(
                    root,
                    action
            );

        } finally {

            root.recycle();
        }
    }

    private boolean performScroll(
            AccessibilityNodeInfo node,
            int action
    ) {

        if (node == null) {
            return false;
        }

        if (node.isEnabled() &&
                node.isScrollable()) {

            if (node.performAction(action)) {
                return true;
            }
        }

        int childCount =
                node.getChildCount();

        for (int i = 0;
             i < childCount;
             i++) {

            AccessibilityNodeInfo child =
                    node.getChild(i);

            if (child == null) {
                continue;
            }

            try {

                if (performScroll(
                        child,
                        action
                )) {

                    return true;
                }

            } finally {

                child.recycle();
            }
        }

        return false;
    }

    // =========================================================
    // GLOBAL PHONE CONTROLS
    // =========================================================

    public boolean goHome() {

        return performGlobalAction(
                GLOBAL_ACTION_HOME
        );
    }

    public boolean goBack() {

        return performGlobalAction(
                GLOBAL_ACTION_BACK
        );
    }

    public boolean openRecents() {

        return performGlobalAction(
                GLOBAL_ACTION_RECENTS
        );
    }

    public boolean openNotifications() {

        return performGlobalAction(
                GLOBAL_ACTION_NOTIFICATIONS
        );
    }

    public boolean openQuickSettings() {

        return performGlobalAction(
                GLOBAL_ACTION_QUICK_SETTINGS
        );
    }

    public boolean lockScreen() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.P) {

            return performGlobalAction(
                    GLOBAL_ACTION_LOCK_SCREEN
            );
        }

        return false;
    }

    // =========================================================
    // STATUS
    // =========================================================

    public String getStatus() {

        if (instance == null) {

            return
                    "Accessibility Service: OFFLINE";
        }

        return
                "Accessibility Service: ONLINE ✓\n"
                + "التطبيق الحالي: "
                + (
                        lastPackageName == null ||
                                lastPackageName.isEmpty()
                                ? "غير معروف"
                                : lastPackageName
                );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private boolean matches(
            CharSequence value,
            String target
    ) {

        if (value == null ||
                target == null) {

            return false;
        }

        String current =
                normalizeText(
                        value.toString()
                );

        String wanted =
                normalizeText(
                        target
                );

        if (current.isEmpty() ||
                wanted.isEmpty()) {

            return false;
        }

        return current.equals(wanted)
                || current.contains(wanted)
                || wanted.contains(current);
    }

    private String normalizeText(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace("أ", "ا")
                .replace("إ", "ا")
                .replace("آ", "ا")
                .replace("ة", "ه")
                .replace("ى", "ي")
                .replaceAll(
                        "\\s+",
                        " "
                );
    }

    private boolean hasText(
            CharSequence value
    ) {

        return value != null
                && !value.toString()
                .trim()
                .isEmpty();
    }

    private String safeCharSequence(
            CharSequence value
    ) {

        return value == null
                ? ""
                : value.toString();
    }
}