package com.loosenotes.util;

public final class HtmlUtil {
    private HtmlUtil() {
    }

    public static String escape(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public static String nl2br(String input) {
        return escape(input).replace("\n", "<br>");
    }
}
