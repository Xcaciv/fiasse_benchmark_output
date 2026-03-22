package com.loosenotes.util;

/**
 * HTML output encoding to prevent XSS (Integrity).
 * Applied to all user-supplied content rendered in JSP views.
 */
public final class HtmlEncoder {

    private HtmlEncoder() {}

    /**
     * Encodes characters that have special meaning in HTML.
     * Use on ALL untrusted data before rendering in HTML context.
     */
    public static String encode(String value) {
        if (value == null) return "";
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&':  sb.append("&amp;");  break;
                case '<':  sb.append("&lt;");   break;
                case '>':  sb.append("&gt;");   break;
                case '"':  sb.append("&quot;"); break;
                case '\'': sb.append("&#x27;"); break;
                case '/':  sb.append("&#x2F;"); break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Encodes for use inside a JavaScript string literal.
     * Use when emitting user data into a &lt;script&gt; context.
     */
    public static String encodeForJs(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("'", "\\'")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("<", "\\u003C")
                    .replace(">", "\\u003E");
    }
}
