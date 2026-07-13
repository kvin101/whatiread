package com.whatiread.config.observability;

final class RepositoryMetricTags {

    private RepositoryMetricTags() {
    }

    static String inferOperation(String methodName) {
        if (methodName.startsWith("find")
                || methodName.startsWith("get")
                || methodName.startsWith("read")
                || methodName.startsWith("count")
                || methodName.startsWith("exists")
                || methodName.startsWith("list")
                || methodName.startsWith("search")
                || methodName.startsWith("query")) {
            return "select";
        }
        if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "delete";
        }
        if (methodName.startsWith("save") || methodName.startsWith("insert") || methodName.startsWith("persist")) {
            return "write";
        }
        if (methodName.startsWith("update")) {
            return "update";
        }
        return "other";
    }

    static String inferTable(String repositorySimpleName) {
        String entityName = repositorySimpleName.endsWith("Repository")
                ? repositorySimpleName.substring(0, repositorySimpleName.length() - "Repository".length())
                : repositorySimpleName;
        return camelCaseToSnake(entityName);
    }

    private static String camelCaseToSnake(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        StringBuilder builder = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (Character.isUpperCase(current)) {
                if (i > 0) {
                    builder.append('_');
                }
                builder.append(Character.toLowerCase(current));
            } else {
                builder.append(current);
            }
        }
        return builder.toString();
    }
}
