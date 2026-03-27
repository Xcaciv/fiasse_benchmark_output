package com.loosenotes.filter;

import com.loosenotes.util.RateLimiter;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

@WebFilter("/*")
public class RateLimitFilter implements Filter {

    private static final Map<String, Integer> RATE_LIMITS = Map.of(
            "/auth/login", 20,
            "/auth/register", 10,
            "/auth/forgot-password", 5,
            "/auth/reset-password", 5,
            "/share/", 60
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getServletPath();
        String ip = getClientIp(req);

        for (Map.Entry<String, Integer> entry : RATE_LIMITS.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                String key = entry.getKey() + ":" + ip;
                if (!RateLimiter.getInstance().isAllowed(key, entry.getValue())) {
                    long retryAfter = RateLimiter.getInstance().getRetryAfterSeconds(key);
                    res.setHeader("Retry-After", String.valueOf(retryAfter));
                    res.sendError(429, "Too Many Requests");
                    return;
                }
                break;
            }
        }

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
