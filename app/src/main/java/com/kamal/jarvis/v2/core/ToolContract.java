package com.kamal.jarvis.v2.core;

import java.util.Collections;
import java.util.Map;

public interface ToolContract {

    String getId();

    String getName();

    String getDescription();

    boolean isAvailable();

    JarvisResult<ToolOutput> execute(ToolInput input);

    default boolean requiresOwnerAuthorization(ToolInput input) {
        return false;
    }

    default Map<String, String> getMetadata() {
        return Collections.emptyMap();
    }

    final class ToolInput {

        private final String action;
        private final Map<String, Object> parameters;

        public ToolInput(
                String action,
                Map<String, Object> parameters
        ) {
            this.action = action == null ? "" : action;
            this.parameters = parameters == null
                    ? Collections.emptyMap()
                    : Collections.unmodifiableMap(parameters);
        }

        public String getAction() {
            return action;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public Object get(String key) {
            return parameters.get(key);
        }

        public boolean has(String key) {
            return parameters.containsKey(key);
        }
    }

    final class ToolOutput {

        private final String toolId;
        private final String action;
        private final Object result;

        public ToolOutput(
                String toolId,
                String action,
                Object result
        ) {
            this.toolId = toolId;
            this.action = action;
            this.result = result;
        }

        public String getToolId() {
            return toolId;
        }

        public String getAction() {
            return action;
        }

        public Object getResult() {
            return result;
        }
    }
}