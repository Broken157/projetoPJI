package com.portifolio.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Render the official page only for missing HTML navigation, retaining HTTP 404. */
@Component
public class FrontendNotFoundResolver implements HandlerExceptionResolver, Ordered {
    @Override
    public int getOrder() { return -1; }

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception exception) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String accept = request.getHeader("Accept");
        if (!(exception instanceof NoResourceFoundException) || !"GET".equals(request.getMethod())
                || accept == null || !accept.contains(MediaType.TEXT_HTML_VALUE)
                || path.contains(".") || reserved(path) || response.isCommitted()) {
            return null;
        }
        var index = new ClassPathResource("static/index.html");
        if (!index.exists()) return null;
        try (var input = index.getInputStream()) {
            byte[] body = input.readAllBytes();
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType(MediaType.TEXT_HTML_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setContentLength(body.length);
            response.getOutputStream().write(body);
            return new ModelAndView();
        } catch (IOException ignored) {
            return null;
        }
    }

    private boolean reserved(String path) {
        return java.util.List.of("/api", "/ws", "/assets", "/static", "/css", "/js", "/error").stream()
                .anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"));
    }
}
