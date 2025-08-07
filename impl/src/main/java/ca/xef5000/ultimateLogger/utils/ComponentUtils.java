package ca.xef5000.ultimateLogger.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class ComponentUtils {

    /**
     * Extracts plain text from any Adventure component.
     * @param component The component to extract text from
     * @return Plain text representation, or empty string if component is null
     */
    public static String extractText(Component component) {
        if (component == null) {
            return "";
        }

        // Use Adventure's built-in serializer for reliable text extraction
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
