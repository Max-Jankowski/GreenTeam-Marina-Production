/**Alexander Baldree
Max Jankowski
Aftabur Rahman
Jordan Dardar

Green team Module 5
Modified by Max on 9-4-26

*/

package com.moffatbaymarina.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.mindrot.jbcrypt.BCrypt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moffatbaymarina.dao.CustomerDAO;
import com.moffatbaymarina.model.Customer;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// POST /login - checks email + password, makes sure the account is
// verified, then starts a session. Handles both a JSON body (AJAX-style)
// and a plain HTML form post, so we don't need two separate endpoints.
@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final ObjectMapper JSON = new ObjectMapper();

    // looking up client by email, verifies password. If both check out starts a
    // fresh session that holds customer Id.
    // 422 if credentials are blank, 401 if either email doesnt exist or passeord
    // wrong 403 if email verified field null
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        prepareJson(response);
        String[] credentials = readCredentials(request, response);
        if (credentials == null)
            return;

        String email = credentials[0].trim().toLowerCase();
        String password = credentials[1];
        if (email.isBlank() || password.isBlank()) {
            writeJson(response, 422, error("Email and password are required."));
            return;
        }

        try { // same error message either way, bad email or bad password. This way nobody can
              // use a failed login to figure out which emails exist
            Customer customer = new CustomerDAO().findByEmail(email);
            if (customer == null || !passwordMatches(password, customer.getPasswordHash())) {
                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                        error("Invalid email or password."));
                return;
            }
            if (!customer.isEmailVerified()) {
                Map<String, Object> body = error("Verify your email before logging in.");
                body.put("emailVerified", false);
                body.put("redirect", "verification.html");
                writeJson(response, HttpServletResponse.SC_FORBIDDEN, body);
                return;
            }

            // kills any old pre-login session first so it can't get hijacked
            // onto this account
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null)
                oldSession.invalidate();
            HttpSession session = request.getSession(true);
            session.setAttribute("customerId", customer.getCustomerId());
            session.setAttribute("email", customer.getEmail());
            session.setAttribute("firstName", customer.getFirstName());

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("ok", true);
            body.put("message", "Login successful.");
            body.put("customerId", customer.getCustomerId());
            body.put("email", customer.getEmail());
            body.put("firstName", customer.getFirstName());
            body.put("redirect", "post_login.html");
            writeJson(response, HttpServletResponse.SC_OK, body);
        } catch (SQLException e) {
            log("Login error", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    error("Login could not be completed."));
        }
    }

    // New accounts will use real BCrypt hashes. but for prototyping we will still
    // seed in plain text. so this falls back to a straight
    // string compare for those until the seed data gets converted.
    private boolean passwordMatches(String password, String storedValue) {
        if (storedValue == null || storedValue.isBlank())
            return false;

        if (storedValue.startsWith("$2a$")
                || storedValue.startsWith("$2b$")
                || storedValue.startsWith("$2y$")) {
            try {
                String verificationValue = storedValue;

                if (verificationValue.startsWith("$2y$")) {
                    verificationValue = "$2a$" + verificationValue.substring(4);
                }

                return BCrypt.checkpw(password, verificationValue);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        return password.equals(storedValue);
    }

    // grabs email/password from JSON if that's what gets sent, otherwise should
    // fall back to normal request params
    // Checks both "email"/"password" and the "loginEmail"/"loginPassword" names
    // some of our HTML uses
    private String[] readCredentials(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().contains("application/json")) {
            try {
                JsonNode data = JSON.readTree(request.getInputStream());
                return new String[] {
                        text(data, "email"),
                        text(data, "password")
                };
            } catch (IOException e) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                        error("Invalid JSON request body."));
                return null;
            }
        }
        String email = request.getParameter("email");
        if (email == null)
            email = request.getParameter("loginEmail");
        String password = request.getParameter("password");
        if (password == null)
            password = request.getParameter("loginPassword");
        return new String[] {
                email == null ? "" : email,
                password == null ? "" : password
        };
    }

    private String text(JsonNode data, String field) {
        JsonNode node = data.get(field);
        return node == null || node.isNull() ? "" : node.asText("").trim();
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", false);
        body.put("message", message);
        return body;
    }

    private void prepareJson(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
    }

    private void writeJson(HttpServletResponse response, int status, Object body)
            throws IOException {
        response.setStatus(status);
        JSON.writeValue(response.getWriter(), body);
    }

}
