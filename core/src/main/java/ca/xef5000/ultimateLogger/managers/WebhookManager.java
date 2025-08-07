package ca.xef5000.ultimateLogger.managers;

import ca.xef5000.ultimateLogger.UltimateLogger;
import ca.xef5000.ultimateLogger.api.FilterCondition;
import ca.xef5000.ultimateLogger.api.LogData;
import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class WebhookManager {
    private final UltimateLogger plugin;
    // It's best practice to create one client and reuse it.
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public WebhookManager(UltimateLogger plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Formats and sends a log to a Discord webhook asynchronously.
     * This is a "fire-and-forget" method.
     */
    public void sendWebhook(String url, String logType, LogData data) {
        sendWebhook(url, logType, data, null);
    }

    /**
     * Formats and sends a log to a Discord webhook asynchronously if it passes the filter conditions.
     * This is a "fire-and-forget" method.
     */
    public void sendWebhook(String url, String logType, LogData data, List<FilterCondition> conditions) {
        // Run the entire process asynchronously to never block the server thread.
        CompletableFuture.runAsync(() -> {
            try {
                // Check if the data matches all filter conditions before sending
                if (conditions != null && !conditions.isEmpty()) {
                    if (!matchesAllConditions(data, conditions)) {
                        // Skip sending webhook if conditions aren't met
                        return;
                    }
                }

                String jsonPayload = buildDiscordEmbed(logType, data);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> {
                            // Log an error if the webhook URL is invalid or Discord returns an error
                            if (response.statusCode() >= 300) {
                                plugin.getLogger().warning("Webhook for " + logType + " failed with status " + response.statusCode() + ": " + response.body());
                            }
                        });

            } catch (Exception e) {
                plugin.getLogger().severe("An error occurred while sending a webhook for " + logType);
                e.printStackTrace();
            }
        });
    }

    /**
     * Checks if the log data matches all filter conditions.
     */
    private boolean matchesAllConditions(LogData data, List<FilterCondition> conditions) {
        for (FilterCondition condition : conditions) {
            String key = condition.key();
            String comparator = condition.comparator();
            String value = condition.value().toString();
            Object dataValue = data.getData().get(key);

            if (dataValue == null) {
                return false;
            }

            String dataValueStr = dataValue.toString();

            boolean matches = switch (comparator) {
                case "=" -> dataValueStr.equals(value);
                case "!=" -> !dataValueStr.equals(value);
                case ">" -> {
                    try {
                        double numData = Double.parseDouble(dataValueStr);
                        double numValue = Double.parseDouble(value);
                        yield numData > numValue;
                    } catch (NumberFormatException e) {
                        yield false;
                    }
                }
                case "<" -> {
                    try {
                        double numData = Double.parseDouble(dataValueStr);
                        double numValue = Double.parseDouble(value);
                        yield numData < numValue;
                    } catch (NumberFormatException e) {
                        yield false;
                    }
                }
                case ">=" -> {
                    try {
                        double numData = Double.parseDouble(dataValueStr);
                        double numValue = Double.parseDouble(value);
                        yield numData >= numValue;
                    } catch (NumberFormatException e) {
                        yield false;
                    }
                }
                case "<=" -> {
                    try {
                        double numData = Double.parseDouble(dataValueStr);
                        double numValue = Double.parseDouble(value);
                        yield numData <= numValue;
                    } catch (NumberFormatException e) {
                        yield false;
                    }
                }
                case "startswith" -> dataValueStr.startsWith(value);
                case "endswith" -> dataValueStr.endsWith(value);
                case "contains" -> dataValueStr.contains(value);
                default -> false;
            };

            if (!matches) {
                return false;
            }
        }

        return true;
    }

    /**
     * Builds a JSON string compatible with Discord's embed format.
     */
    private String buildDiscordEmbed(String logType, LogData data) {
        // Build the fields for the embed
        List<Map<String, Object>> fields = new ArrayList<>();
        for (Map.Entry<String, Object> entry : data.getData().entrySet()) {
            fields.add(Map.of(
                    "name", formatKey(entry.getKey()),
                    "value", "```" + entry.getValue().toString() + "```", // Use code blocks for nice formatting
                    "inline", true
            ));
        }

        long logId = data.getId();

        // Build the main embed object
        Map<String, Object> embed = Map.of(
                "title", "New Log: " + logType,
                "color", 5814783, // A nice blue color
                "fields", fields,
                "footer", Map.of("text", "Log ID: " + logId),
                "timestamp", java.time.Instant.now().toString()
        );

        // Build the final payload that Discord expects
        Map<String, Object> payload = Map.of(
                "username", "UltimateLogger",
                "embeds", List.of(embed)
        );

        return gson.toJson(payload);
    }

    // Helper function to format keys like "player_name" into "Player Name"
    private String formatKey(String key) {
        if (key == null || key.isEmpty()) return "";
        String[] words = key.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            formatted.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase()).append(" ");
        }
        return formatted.toString().trim();
    }
}
