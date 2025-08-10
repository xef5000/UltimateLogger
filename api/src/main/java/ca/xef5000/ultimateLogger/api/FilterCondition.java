package ca.xef5000.ultimateLogger.api;

public record FilterCondition(String key, String comparator, Object value, LogicalOperator logicalOperator) {

    // Backward compatibility constructor - defaults to AND
    public FilterCondition(String key, String comparator, Object value) {
        this(key, comparator, value, LogicalOperator.AND);
    }

    public enum LogicalOperator {
        AND, OR
    }
}
