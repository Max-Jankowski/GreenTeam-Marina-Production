/*
Alexander Baldree
Moffat Bay Marina
Logout Servlet
*/

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
 * Ends the authenticated customer session only when the user
 * explicitly clicks LOGOUT.
 *
 * Normal navigation such as HOME, ABOUT US, PRICING,
 * AVAILABILITY, WAIT LIST, and RESERVATION does not
 * invalidate the session.
 */
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    private static final ObjectMapper JSON =
        new ObjectMapper();

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");

        HttpSession session =
            request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        Map<String, Object> body =
            new LinkedHashMap<>();

        body.put("ok", true);
        body.put("message", "Logout successful.");
        body.put("redirect", "index.html");

        JSON.writeValue(
            response.getWriter(),
            body
        );
    }
}
