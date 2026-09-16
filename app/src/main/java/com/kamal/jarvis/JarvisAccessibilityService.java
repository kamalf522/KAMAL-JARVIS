package com.kamal.jarvis;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

public class JarvisAccessibilityService extends AccessibilityService {

    private static JarvisAccessibilityService instance;

    private String lastPackageName = "";
    private String lastScreenText = "";

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        instance = this;

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();

        info.eventTypes =
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                        | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                        | AccessibilityEvent.TYPE_VIEW_CLICKED
                        | AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                        | AccessibilityEvent.TYPE_VIEW_SCROLLED;

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
    public void onAccessibilityEvent(AccessibilityEvent event) {

        if (event == null) {
            return;
        }

        CharSequence packageName = event.getPackageName();

        if (packageName != null) {
            lastPackageName = packageName.toString();
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();

        if (root != null) {
            lastScreenText = extractScreenText(root);
        }
    }

    @Override
    public void onInterrupt() {
        lastScreenText = "";
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    public static JarvisAccessibilityService getInstance() {
        return instance;
    }

    public boolean isConnected() {
        return instance != null;
    }

    public String getCurrentPackage() {
        return lastPackageName;
    }

    public String getScreenText() {

        AccessibilityNodeInfo root =
                getRootInActiveWindow();

        if (root == null) {
            return "JARVIS: ما قدرتش نوصل للشاشة الحالية.";
        }

        lastScreenText =
                extractScreenText(root);

        if (lastScreenText.trim().isEmpty()) {
            return "JARVIS: الشاشة الحالية ما فيهاش نص واضح.";
        }

        return lastScreenText;
    }

    public String getScreenTree() {

        AccessibilityNodeInfo root =
                getRootInActiveWindow();

        if (root == null) {
            return "JARVIS: ما كايناش شاشة نشطة.";
        }

        StringBuilder result =
                new StringBuilder();

        result.append("JARVIS SCREEN TREE\n");
        result.append("=================\n\n");

        buildNodeTree(root, result, 0);

        return result.toString();
    }

    private void buildNodeTree(
            AccessibilityNodeInfo node,
            StringBuilder result,
            int depth
    ) {

        if (node == null) {
            return;
        }

        for (int i = 0; i < depth; i++) {
            result.append("  ");
        }

        CharSequence text = node.getText();
        CharSequence description =
                node.getContentDescription();
        CharSequence className =
                node.getClassName();

        result.append("- ");

        if (text != null &&
                !text.toString().trim().isEmpty()) {

            result.append("text=\"")
                    .append(text)
                    .append("\" ");
        }

        if (description != null &&
                !description.toString().trim().isEmpty()) {

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

        result.append("\n");

        for (int i = 0;
             i < node.getChildCount();
             i++) {

            AccessibilityNodeInfo child =
                    node.getChild(i);

            if (child != null) {

                buildNodeTree(
                        child,
                        result,
                        depth + 1
                );

                child.recycle();
            }
        }
    }

    private String extractScreenText(
            AccessibilityNodeInfo node
    ) {

        if (node == null) {
            return "";
        }

        List<String> lines =
                new ArrayList<>();

        collectText(node, lines);

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

            if (result.indexOf(clean) == -1) {

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

        if (node == null) {
            return;
        }

        CharSequence text =
                node.getText();

        if (text != null &&
                !text.toString().trim().isEmpty()) {

            lines.add(text.toString());
        }

        CharSequence description =
                node.getContentDescription();

        if (description != null &&
                !description.toString().trim().isEmpty()) {

            lines.add(description.toString());
        }

        for (int i = 0;
             i < node.getChildCount();
             i++) {

            AccessibilityNodeInfo child =
                    node.getChild(i);

            if (child != null) {

                collectText(child, lines);

                child.recycle();
            }
        }
    }

    public boolean clickByText(String target) {

        if (target == null ||
                target.trim().isEmpty()) {

            return false;
        }

        AccessibilityNodeInfo root =
                getRootInActiveWindow();

        if (root == null) {
            return false;
        }

        boolean result =
                clickNodeByText(
                        root,
                        target.trim()
                );

        root.recycle();

        return result;
    }

    private boolean clickNodeByText(
            AccessibilityNodeInfo node,
            String target
    ) {

        if (node == null) {
            return false;
        }

        CharSequence text =
                node.getText();

        CharSequence description =
                node.getContentDescription();

        if (matches(text, target)
                || matches(description, target)) {

            if (node.isClickable()) {

                boolean clicked =
                        node.performAction(
                                AccessibilityNodeInfo.ACTION_CLICK
                        );

                if (clicked) {
                    return true;
                }
            }

            AccessibilityNodeInfo parent =
                    node.getParent();

            if (parent != null) {

                boolean clicked =
                        parent.performAction(
                                AccessibilityNodeInfo.ACTION_CLICK
                        );

                parent.recycle();

                if (clicked) {
                    return true;
                }
            }
        }

        for (int i = 0;
             i < node.getChildCount();
             i++) {

            AccessibilityNodeInfo child =
                    node.getChild(i);

            if (child != null) {

                boolean result =
                        clickNodeByText(
                                child,
                                target
                        );

                child.recycle();

                if (result) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean matches(
            CharSequence value,
            String target
    ) {

        if (value == null) {
            return false;
        }

        String current =
                value.toString()
                        .trim()
                        .toLowerCase();

        String wanted =
                target.trim()
                        .toLowerCase();

        return current.equals(wanted)
                || current.contains(wanted);
    }

    public boolean typeText(
            String target,
            String text
    ) {

        if (target == null ||
                target.trim().isEmpty()
                || text == null) {

            return false;
        }

        AccessibilityNodeInfo root =
                getRootInActiveWindow();

        if (root == null) {
            return false;
        }

        AccessibilityNodeInfo node =
                findEditableNode(
                        root,
                        target.trim()
                );

        if (node == null) {
            return false;
        }

        Bundle arguments =
                new Bundle();

        arguments.putCharSequence(
                AccessibilityNodeInfo
                        .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
        );

        boolean result =
                node.performAction(
                        AccessibilityNodeInfo.ACTION_SET_TEXT,
                        arguments
                );

        node.recycle();
        root.recycle();

        return result;
    }

    private AccessibilityNodeInfo findEditableNode(
            AccessibilityNodeInfo node,
            String target
    ) {

        if (node == null) {
            return null;
        }

        CharSequence nodeText =
                node.getText();

        CharSequence hint =
                node.getHintText();

        if (node.isEditable()) {

            if (target.isEmpty()
                    || matches(nodeText, target)
                    || matches(hint, target)) {

                return node;
            }
        }

        for (int i = 0;
             i < node.getChildCount();
             i++) {

            AccessibilityNodeInfo child =
                    node.getChild(i);

            if (child != null) {

                AccessibilityNodeInfo result =
                        findEditableNode(
                                child,
                                target
                        );

                if (result != null) {
                    return result;
                }

                child.recycle();
            }
        }

        return null;
    }

    public boolean scrollForward() {

        AccessibilityNodeInfo root =
                getRootInActiveWindow();

        if (root == null) {
            return false;
        }

        boolean result =
                performScroll(
                        root,
                        AccessibilityNodeInfo
                                .ACTION_SCROLL_FORWARD
                );

        root.recycle();

        return result;
    }

    public boolean scrollBackward() {

        AccessibilityNodeInfo root =
                getRootInActiveWindow();

        if (root == null) {
            return false;
        }

        boolean result =
                performScroll(
                        root,
                        AccessibilityNodeInfo
                                .ACTION_SCROLL_BACKWARD
                );

        root.recycle();

        return result;
    }

    private boolean performScroll(
            AccessibilityNodeInfo node,
            int action
    ) {

        if (node == null) {
            return false;
        }

        if (node.isScrollable()) {

            boolean result =
                    node.performAction(action);

            if (result) {
                return true;
            }
        }

        for (int i = 0;
             i < node.getChildCount();
             i++) {

            AccessibilityNodeInfo child =
                    node.getChild(i);

            if (child != null) {

                boolean result =
                        performScroll(
                                child,
                                action
                        );

                child.recycle();

                if (result) {
                    return true;
                }
            }
        }

        return false;
    }

    public String getNodeInfoByText(
            String target
    ) {

        if (target == null ||
                target.trim().isEmpty()) {

            return "JARVIS: خاصني النص اللي نقلب عليه.";
        }

        AccessibilityNodeInfo root =
                getRootInActiveWindow();

        if (root == null) {
            return "JARVIS: ما كايناش شاشة نشطة.";
        }

        AccessibilityNodeInfo node =
                findNodeByText(
                        root,
                        target.trim()
                );

        if (node == null) {
            return "JARVIS: ما لقيتش العنصر: "
                    + target;
        }

        Rect bounds =
                new Rect();

        node.getBoundsInScreen(bounds);

        String result =
                "العنصر موجود ✓\n"
                        + "النص: "
                        + String.valueOf(node.getText())
                        + "\n"
                        + "الوصف: "
                        + String.valueOf(
                        node.getContentDescription()
                )
                        + "\n"
                        + "قابل للضغط: "
                        + node.isClickable()
                        + "\n"
                        + "قابل للكتابة: "
                        + node.isEditable()
                        + "\n"
                        + "الموقع: "
                        + bounds.toShortString();

        node.recycle();
        root.recycle();

        return result;
    }

    private AccessibilityNodeInfo findNodeByText(
            AccessibilityNodeInfo node,
            String target
    ) {

        if (node == null) {
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

        for (int i = 0;
             i < node.getChildCount();
             i++) {

            AccessibilityNodeInfo child =
                    node.getChild(i);

            if (child != null) {

                AccessibilityNodeInfo result =
                        findNodeByText(
                                child,
                                target
                        );

                if (result != null) {
                    return result;
                }

                child.recycle();
            }
        }

        return null;
    }
}