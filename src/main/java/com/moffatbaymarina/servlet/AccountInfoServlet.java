package com.moffatbaymarina.servlet;

import com.moffatbaymarina.config.DatabaseConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Returns account, boat, slip, and electric information for the
 * customer who is currently logged in.
 *
 * Database tables are based on GreenTeamDataBase.java:
 * customers -> boats -> reservations -> slips -> slip_types.
 */
@WebServlet("/account-info")
public class AccountInfoServlet extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        addCorsHeaders(request, response);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");

        HttpSession session = request.getSession(false);

        if (session == null) {
            sendUnauthorized(response);
            return;
        }

        Integer customerId = getCustomerIdFromSession(session);
        String email = getEmailFromSession(session);

        if (customerId == null && (email == null || email.isBlank())) {
            sendUnauthorized(response);
            return;
        }

        /*
         * This query returns the customer's account information, one boat,
         * and the newest non-cancelled reservation for that boat.
         *
         * LEFT JOIN is used so a registered customer can still see their
         * account and boat information even if they do not have a reservation.
         */
        String sqlById =
                "SELECT "
              + "c.first_name, "
              + "c.last_name, "
              + "b.boat_name, "
              + "b.boat_length_ft, "
              + "st.size_ft AS slip_size_ft, "
              + "r.electric_included "
              + "FROM customers c "
              + "LEFT JOIN boats b "
              + "ON b.customer_id = c.customer_id "
              + "LEFT JOIN reservations r "
              + "ON r.customer_id = c.customer_id "
              + "AND r.boat_id = b.boat_id "
              + "AND r.status <> 'CANCELLED' "
              + "LEFT JOIN slips s "
              + "ON s.slip_id = r.slip_id "
              + "LEFT JOIN slip_types st "
              + "ON st.slip_type_id = s.slip_type_id "
              + "WHERE c.customer_id = ? "
              + "ORDER BY "
              + "CASE WHEN r.status = 'CONFIRMED' THEN 0 "
              + "WHEN r.status = 'PENDING' THEN 1 ELSE 2 END, "
              + "r.created_at DESC, "
              + "b.created_at DESC "
              + "LIMIT 1";

        String sqlByEmail =
                "SELECT "
              + "c.first_name, "
              + "c.last_name, "
              + "b.boat_name, "
              + "b.boat_length_ft, "
              + "st.size_ft AS slip_size_ft, "
              + "r.electric_included "
              + "FROM customers c "
              + "LEFT JOIN boats b "
              + "ON b.customer_id = c.customer_id "
              + "LEFT JOIN reservations r "
              + "ON r.customer_id = c.customer_id "
              + "AND r.boat_id = b.boat_id "
              + "AND r.status <> 'CANCELLED' "
              + "LEFT JOIN slips s "
              + "ON s.slip_id = r.slip_id "
              + "LEFT JOIN slip_types st "
              + "ON st.slip_type_id = s.slip_type_id "
              + "WHERE c.email = ? "
              + "ORDER BY "
              + "CASE WHEN r.status = 'CONFIRMED' THEN 0 "
              + "WHEN r.status = 'PENDING' THEN 1 ELSE 2 END, "
              + "r.created_at DESC, "
              + "b.created_at DESC "
              + "LIMIT 1";

        String sql = customerId != null ? sqlById : sqlByEmail;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            if (customerId != null) {
                statement.setInt(1, customerId);
            } else {
                statement.setString(1, email);
            }

            try (ResultSet rs = statement.executeQuery()) {

                if (!rs.next()) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    writeJson(
                            response,
                            "{\"ok\":false,"
                          + "\"message\":\"No customer record was found.\"}");
                    return;
                }

                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");
                String boatName = rs.getString("boat_name");

                BigDecimal boatSize =
                        rs.getBigDecimal("boat_length_ft");

                BigDecimal slipSize =
                        rs.getBigDecimal("slip_size_ft");

                Boolean electric = null;
                boolean electricValue =
                        rs.getBoolean("electric_included");

                if (!rs.wasNull()) {
                    electric = electricValue;
                }

                String json =
                        "{"
                      + "\"ok\":true,"
                      + "\"firstName\":" + jsonString(firstName) + ","
                      + "\"lastName\":" + jsonString(lastName) + ","
                      + "\"boatName\":" + jsonString(boatName) + ","
                      + "\"boatSize\":" + jsonNumber(boatSize) + ","
                      + "\"slipSize\":" + jsonNumber(slipSize) + ","
                      + "\"electric\":" + jsonBoolean(electric)
                      + "}";

                writeJson(response, json);
            }

        } catch (Exception e) {
            getServletContext().log(
                    "Unable to load account information.", e);

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            writeJson(
                    response,
                    "{\"ok\":false,"
                  + "\"message\":" + jsonString(
                        "Account API error: "
                        + e.getClass().getSimpleName()
                        + ": "
                        + (e.getMessage() == null
                            ? "No error message supplied."
                            : e.getMessage()))
                  + "}");
        }
    }


    /**
     * Supports several common session attribute names so this servlet
     * can work with the team's existing LoginServlet.
     */
    private Integer getCustomerIdFromSession(HttpSession session) {

        String[] names = {
                "customerId",
                "customer_id",
                "userId",
                "loggedInCustomerId"
        };

        for (String name : names) {
            Object value = session.getAttribute(name);

            if (value instanceof Number number) {
                return number.intValue();
            }

            if (value instanceof String text) {
                try {
                    return Integer.valueOf(text);
                } catch (NumberFormatException ignored) {
                    // Try the next possible session attribute.
                }
            }
        }

        return null;
    }


    private String getEmailFromSession(HttpSession session) {

        String[] names = {
                "email",
                "userEmail",
                "customerEmail",
                "loggedInEmail"
        };

        for (String name : names) {
            Object value = session.getAttribute(name);

            if (value != null) {
                String text = value.toString().trim();

                if (!text.isEmpty()) {
                    return text;
                }
            }
        }

        return null;
    }


    @Override
    protected void doOptions(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        addCorsHeaders(request, response);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }


    /**
     * Allows the GitHub Pages front end to call this API while sending
     * the authenticated JSESSIONID cookie.
     *
     * IMPORTANT:
     * Access-Control-Allow-Origin cannot be "*" when credentials are used.
     */
    private void addCorsHeaders(
            HttpServletRequest request,
            HttpServletResponse response) {

        String origin = request.getHeader("Origin");

        // Allow the deployed GitHub Pages front end.
        // Also allow local development origins.
        if (origin != null && (
                origin.equals("https://max-jankowski.github.io")
                || origin.equals("http://localhost:8080")
                || origin.equals("http://127.0.0.1:8080")
                || origin.equals("http://localhost:5500")
                || origin.equals("http://127.0.0.1:5500"))) {

            response.setHeader(
                    "Access-Control-Allow-Origin",
                    origin);

            response.setHeader(
                    "Access-Control-Allow-Credentials",
                    "true");

            response.setHeader(
                    "Vary",
                    "Origin");
        }

        response.setHeader(
                "Access-Control-Allow-Methods",
                "GET, OPTIONS");

        response.setHeader(
                "Access-Control-Allow-Headers",
                "Content-Type, Accept");

        response.setHeader(
                "Access-Control-Max-Age",
                "3600");
    }


    private void sendUnauthorized(HttpServletResponse response)
            throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        writeJson(
                response,
                "{\"ok\":false,"
              + "\"message\":\"You must be logged in to view this page.\","
              + "\"redirect\":\"login.html\"}");
    }


    private void writeJson(
            HttpServletResponse response,
            String json)
            throws IOException {

        try (PrintWriter out = response.getWriter()) {
            out.print(json);
        }
    }


    private String jsonString(String value) {

        if (value == null) {
            return "null";
        }

        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");

        return "\"" + escaped + "\"";
    }


    private String jsonNumber(BigDecimal value) {
        return value == null ? "null" : value.toPlainString();
    }


    private String jsonBoolean(Boolean value) {
        return value == null ? "null" : value.toString();
    }
}
