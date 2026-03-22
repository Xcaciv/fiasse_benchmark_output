package com.loosenotes.util;

import javax.servlet.http.HttpServletRequest;

public final class RequestUtil {
    private RequestUtil() {
    }

    public static String baseUrl(HttpServletRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append(request.getScheme()).append("://").append(request.getServerName());
        boolean standardPort = ("http".equalsIgnoreCase(request.getScheme()) && request.getServerPort() == 80)
                || ("https".equalsIgnoreCase(request.getScheme()) && request.getServerPort() == 443);
        if (!standardPort) {
            builder.append(':').append(request.getServerPort());
        }
        builder.append(request.getContextPath());
        return builder.toString();
    }
}
