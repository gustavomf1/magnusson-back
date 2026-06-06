package com.magnossao.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CheckoutRateLimitFilter extends OncePerRequestFilter {

    private static final int LIMITE = 5;
    private static final long JANELA_MS = 60_000;

    private final Map<String, Deque<Long>> registros = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        if (!HttpMethod.POST.name().equals(req.getMethod()) || !"/api/pedidos".equals(req.getRequestURI())) {
            chain.doFilter(req, res);
            return;
        }
        String ip = obterIp(req);
        long agora = System.currentTimeMillis();
        Deque<Long> timestamps = registros.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && agora - timestamps.peekFirst() > JANELA_MS) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= LIMITE) {
                res.setStatus(429);
                res.setContentType("application/json");
                res.getWriter().write("{\"erro\": \"Muitas tentativas. Aguarde 1 minuto.\"}");
                return;
            }
            timestamps.addLast(agora);
        }
        chain.doFilter(req, res);
    }

    private String obterIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank()) ? forwarded.split(",")[0].trim() : req.getRemoteAddr();
    }
}
