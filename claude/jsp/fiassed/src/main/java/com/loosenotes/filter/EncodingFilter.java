package com.loosenotes.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import java.io.IOException;

/**
 * Servlet filter that enforces UTF-8 character encoding on every HTTP request
 * and response before any other processing occurs.
 *
 * <p>This filter must be mapped with the highest priority (lowest order value)
 * in {@code web.xml} or via annotation ordering so that no other filter or
 * servlet reads request parameters before the encoding is set. Once a
 * {@link ServletRequest} has been read, changing the encoding has no effect.</p>
 */
@WebFilter(filterName = "EncodingFilter", urlPatterns = "/*")
public class EncodingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(EncodingFilter.class);

    private static final String UTF8 = "UTF-8";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("EncodingFilter initialized (charset={})", UTF8);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        // Set request encoding only if the client did not specify one.
        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding(UTF8);
        }

        // Always enforce UTF-8 for the response regardless of what the
        // downstream handler may set; this ensures consistent output encoding.
        response.setCharacterEncoding(UTF8);

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        log.debug("EncodingFilter destroyed");
    }
}
