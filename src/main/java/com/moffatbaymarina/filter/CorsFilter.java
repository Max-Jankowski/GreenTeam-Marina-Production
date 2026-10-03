package com.moffatbaymarina.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

// Lets the frontend - now served from a different origin (GitHub Pages)
// than this backend (Tailscale Funnel) - call /register, /login,
// /reservation, /reservation-lookup and still have the JSESSIONID
// session cookie go along with the request. The origin has to be echoed
// back exactly (never "*") because Access-Control-Allow-Credentials
// requires a specific origin, not a wildcard.
@WebFilter("/*")
public class CorsFilter implements Filter {

    // The browser's Origin header is scheme + host only - never a path -
    // so this is "https://max-jankowski.github.io", NOT
    // ".../GreenTeam-Marina-Production/". GitHub Pages serves every repo
    // under one account from that same origin, so this one entry covers
    // the production site regardless of which repo it's published from.
    // The localhost entries are for testing the static pages straight
    // off disk / a local dev server before pushing to GitHub.
    private static final Set<String> ALLOWED_ORIGINS = Set.of(
            "https://max-jankowski.github.io",
            "http://localhost:5500",
            "http://127.0.0.1:5500"
    );

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String origin = request.getHeader("Origin");
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            // tells any cache/proxy the response differs by Origin, so one
            // origin's allow headers never get served back to another
            response.setHeader("Vary", "Origin");
        }

        // Preflight: the browser sends OPTIONS before the real request
        // whenever credentials or a JSON content-type are involved on a
        // cross-origin call. Answer it here and stop - it should never
        // reach RegisterServlet/LoginServlet/etc, which only implement
        // doGet/doPost/doPut.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept");
            response.setHeader("Access-Control-Max-Age", "3600");
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        chain.doFilter(req, res);
    }
}
