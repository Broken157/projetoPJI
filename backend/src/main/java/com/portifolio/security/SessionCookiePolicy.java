package com.portifolio.security;

import com.portifolio.exception.ForbiddenException;
import jakarta.servlet.http.*;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

@Component
public class SessionCookiePolicy {
    public static final String NAME = "palco_refresh";
    @Value("${jwt.refresh.expiration-days:30}") private long days;
    @Value("${app.cors.allowed-origins}") private List<String> allowedOrigins;

    public void validarOrigem(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        String own = request.getScheme() + "://" + request.getServerName();
        int port = request.getServerPort();
        if (!(port == 80 && request.getScheme().equals("http")) && !(port == 443 && request.getScheme().equals("https")))
            own += ":" + port;
        if (origin != null && !origin.equals(own) && !allowedOrigins.contains(origin))
            throw new ForbiddenException("Origem não autorizada.");
    }
    public String ler(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies()).filter(c -> NAME.equals(c.getName()))
                .map(Cookie::getValue).filter(v -> !v.isBlank()).findFirst().orElse(null);
    }
    public void gravar(HttpServletResponse response, String value) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(NAME, value == null ? "" : value)
                .httpOnly(true).secure(true).sameSite("Strict").path("/api/auth")
                .maxAge(value == null ? Duration.ZERO : Duration.ofDays(days)).build().toString());
    }
}
