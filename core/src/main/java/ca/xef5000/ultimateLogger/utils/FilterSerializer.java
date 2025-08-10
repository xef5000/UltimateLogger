package ca.xef5000.ultimateLogger.utils;

import ca.xef5000.ultimateLogger.api.FilterCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FilterSerializer {
    private static final String AND_DELIMITER = ";";
    private static final String OR_DELIMITER = "&";
    private static final String CONDITION_DELIMITER = "|";

    /**
     * Serializes a filter string and list of filter conditions into a single compact string.
     * Format: filter;key1|op1|value1;key2|op2|value2... (AND conditions)
     * Format: filter&key1|op1|value1&key2|op2|value2... (OR conditions)
     */
    public static String serialize(String filter, List<FilterCondition> conditions) {
        StringBuilder sb = new StringBuilder();

        // Add the filter (even if null or empty)
        sb.append(filter == null ? "" : filter);

        // Add each condition
        if (conditions != null) {
            for (FilterCondition condition : conditions) {
                // Use appropriate delimiter based on logical operator
                String delimiter = (condition.logicalOperator() == FilterCondition.LogicalOperator.OR)
                    ? OR_DELIMITER : AND_DELIMITER;

                sb.append(delimiter)
                        .append(escapeSpecialChars(condition.key()))
                        .append(CONDITION_DELIMITER)
                        .append(escapeSpecialChars(condition.comparator()))
                        .append(CONDITION_DELIMITER)
                        .append(escapeSpecialChars(condition.value().toString()));
            }
        }

        return sb.toString();
    }

    /**
     * Deserializes a string into a filter and list of filter conditions.
     * Supports both AND (;) and OR (&) delimiters.
     * @return A Map.Entry where the key is the filter string and value is the list of conditions
     */
    public static Map.Entry<String, List<FilterCondition>> deserialize(String serialized) {
        if (serialized == null || serialized.isEmpty()) {
            return Map.entry("", List.of());
        }

        List<FilterCondition> conditions = new ArrayList<>();
        String filter = "";

        // First, handle the case where we start with OR delimiter
        if (serialized.startsWith(OR_DELIMITER)) {
            String[] orParts = serialized.split(OR_DELIMITER);
            filter = orParts[0]; // Should be empty

            for (int i = 1; i < orParts.length; i++) {
                FilterCondition condition = parseCondition(orParts[i], FilterCondition.LogicalOperator.OR);
                if (condition != null) conditions.add(condition);
            }
            return Map.entry(filter, conditions);
        }

        // Split by AND delimiter first
        String[] andParts = serialized.split(AND_DELIMITER);
        filter = andParts[0]; // First part is always the filter

        // Process each part
        for (int i = 1; i < andParts.length; i++) {
            String part = andParts[i];

            // Check if this part contains OR conditions
            if (part.contains(OR_DELIMITER)) {
                String[] orParts = part.split(OR_DELIMITER);

                // First OR part should be treated as OR (since it follows an AND delimiter)
                if (!orParts[0].isEmpty()) {
                    FilterCondition condition = parseCondition(orParts[0], FilterCondition.LogicalOperator.OR);
                    if (condition != null) conditions.add(condition);
                }

                // Remaining OR parts
                for (int j = 1; j < orParts.length; j++) {
                    FilterCondition condition = parseCondition(orParts[j], FilterCondition.LogicalOperator.OR);
                    if (condition != null) conditions.add(condition);
                }
            } else {
                // Pure AND condition
                FilterCondition condition = parseCondition(part, FilterCondition.LogicalOperator.AND);
                if (condition != null) conditions.add(condition);
            }
        }

        return Map.entry(filter, conditions);
    }

    /**
     * Parses a single condition string into a FilterCondition object.
     */
    private static FilterCondition parseCondition(String conditionStr, FilterCondition.LogicalOperator operator) {
        String[] condParts = conditionStr.split("\\" + CONDITION_DELIMITER);
        if (condParts.length == 3) {
            return new FilterCondition(
                    unescapeSpecialChars(condParts[0]),
                    unescapeSpecialChars(condParts[1]),
                    unescapeSpecialChars(condParts[2]),
                    operator
            );
        }
        return null;
    }

    /**
     * Escapes special characters to prevent issues with the delimiters.
     */
    private static String escapeSpecialChars(String input) {
        if (input == null) return "";
        return input
                .replace("\\", "\\\\")
                .replace(AND_DELIMITER, "\\" + AND_DELIMITER)
                .replace(OR_DELIMITER, "\\" + OR_DELIMITER)
                .replace(CONDITION_DELIMITER, "\\" + CONDITION_DELIMITER);
    }

    /**
     * Reverses the escaping of special characters.
     */
    private static String unescapeSpecialChars(String input) {
        if (input == null) return "";
        return input
                .replace("\\" + CONDITION_DELIMITER, CONDITION_DELIMITER)
                .replace("\\" + OR_DELIMITER, OR_DELIMITER)
                .replace("\\" + AND_DELIMITER, AND_DELIMITER)
                .replace("\\\\", "\\");
    }
}
