package com.moffatbaymarina.servlet;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Returns basic information for the currently authenticated customer.
 * The values come from the server-side session created by LoginServlet.
 */
@WebServlet("/current-user")
public class CurrentUserServlet extends HttpServlet {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("customerId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("ok", false);
            body.put("message", "No logged-in customer session was found.");
            body.put("redirect", "login.html");
            JSON.writeValue(response.getWriter(), body);
            return;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", true);
        body.put("customerId", session.getAttribute("customerId"));
        body.put("email", session.getAttribute("email"));
        body.put("firstName", session.getAttribute("firstName"));

        JSON.writeValue(response.getWriter(), body);
    }
}
