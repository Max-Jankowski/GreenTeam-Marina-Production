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
 * Logs out the currently authenticated customer.
 *
 * Normal navigation does not invalidate the session.
 * The session is destroyed only when /logout is requested.
 */
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", true);
        body.put("message", "Logout successful.");
        body.put("redirect", "index.html");

        JSON.writeValue(response.getWriter(), body);
    }

    /*
     * Optional GET support makes the endpoint easier to test directly
     * in a browser. The site JavaScript still uses POST.
     */
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        doPost(request, response);
    }
}
