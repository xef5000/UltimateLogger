package ca.xef5000.ultimateLogger.utils;

import ca.xef5000.ultimateLogger.api.FilterCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FilterSerializer {
    private static final String DELIMITER = ";";
    private static final String CONDITION_DELIMITER = "|";

    /**
     * Serializes a filter string and list of filter conditions into a single compact string.
     * Format: filter;key1|op1|value1;key2|op2|value2...
     */
    public static String serialize(String filter, List<FilterCondition> conditions) {
        StringBuilder sb = new StringBuilder();

        // Add the filter (even if null or empty)
        sb.append(filter == null ? "" : filter);

        // Add each condition
        if (conditions != null) {
            for (FilterCondition condition : conditions) {
                sb.append(DELIMITER)
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
     * @return A Map.Entry where the key is the filter string and value is the list of conditions
     */
    public static Map.Entry<String, List<FilterCondition>> deserialize(String serialized) {
        if (serialized == null || serialized.isEmpty()) {
            return Map.entry("", List.of());
        }

        String[] parts = serialized.split(DELIMITER);
        String filter = parts[0]; // First part is the filter

        List<FilterCondition> conditions = new ArrayList<>();

        // Parse remaining parts as conditions
        for (int i = 1; i < parts.length; i++) {
            String[] condParts = parts[i].split("\\" + CONDITION_DELIMITER);
            if (condParts.length == 3) {
                conditions.add(new FilterCondition(
                        unescapeSpecialChars(condParts[0]),
                        unescapeSpecialChars(condParts[1]),
                        unescapeSpecialChars(condParts[2])
                ));
            }
        }

        return Map.entry(filter, conditions);
    }

    /**
     * Escapes special characters to prevent issues with the delimiters.
     */
    private static String escapeSpecialChars(String input) {
        if (input == null) return "";
        return input
                .replace("\\", "\\\\")
                .replace(DELIMITER, "\\" + DELIMITER)
                .replace(CONDITION_DELIMITER, "\\" + CONDITION_DELIMITER);
    }

    /**
     * Reverses the escaping of special characters.
     */
    private static String unescapeSpecialChars(String input) {
        if (input == null) return "";
        return input
                .replace("\\" + CONDITION_DELIMITER, CONDITION_DELIMITER)
                .replace("\\" + DELIMITER, DELIMITER)
                .replace("\\\\", "\\");
    }
}
