/**Alexander Baldree
Max Jankowski
Aftabur Rahman
Jordan Dardar

Green team Module 5
Modified by Max on 9-4-26

*/
package com.moffatbaymarina.servlet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moffatbaymarina.config.DatabaseConnection;
import com.moffatbaymarina.dao.CustomerDAO;
import com.moffatbaymarina.dao.EmailVerificationDAO;
import com.moffatbaymarina.model.EmailVerification;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;


 //handling the UST-04 story and verifing email. 
@WebServlet("/verification")
public class VerificationServlet extends HttpServlet {
    private static final ObjectMapper JSON = new ObjectMapper();
	 // verification link cliked from email, token will arrive as a parameter for query.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        verify(request.getParameter("token"), response);
    }
	 // the same verification, but posts token as a JSON instead of relying in a query string
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String token;
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().contains("application/json")) {
            try {
                JsonNode data = JSON.readTree(request.getInputStream());
                JsonNode tokenNode = data.get("token");
                token = tokenNode == null ? "" : tokenNode.asText("").trim();
            } catch (IOException e) {
                prepareJson(response);
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                        error("Invalid JSON request body."));
                return;
            }
        } else {
            token = request.getParameter("token");
        }
        verify(token, response);
    }

	 
	 //hashing the supplied token and looks for a match. if found, will mark the record used and switchs the 
	 // customer email_verified flag. commits both changes. if no token was supplied responds with a 422 and 404 if it doesnt match
    private void verify(String token, HttpServletResponse response) throws IOException {
        prepareJson(response);
        if (token == null || token.isBlank()) {
            writeJson(response, 422, error("Verification token is required."));
            return;
        }

        String cleanToken = token.trim();
        String tokenHash = sha256(cleanToken);
        EmailVerificationDAO verificationDAO = new EmailVerificationDAO();
        CustomerDAO customerDAO = new CustomerDAO();

        try {            
			// here findValid checks both the token and its hash. the demo data is in plain text 
			// but the real world entry will store only in hash. see the DAO for verification 
            EmailVerification verification = verificationDAO.findValid(cleanToken, tokenHash);
            if (verification == null) {
                writeJson(response, HttpServletResponse.SC_NOT_FOUND,
                        error("The verification token is invalid, expired, or already used."));
                return;
            }

            try (Connection connection = DatabaseConnection.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    if (!verificationDAO.markVerified(connection, verification.getVerificationId())) {
                        // markVerified only returns false if verified_at was set, Indicates that someone else 
                        // already completed this exact token. Treats it as used 
                        connection.rollback(); 
                        writeJson(response, HttpServletResponse.SC_CONFLICT,
                                error("The verification token has already been used."));
                        return;
                    }
                    customerDAO.setEmailVerified(connection, verification.getCustomerId(), true);
                    connection.commit();
                } catch (SQLException | RuntimeException e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            }

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("ok", true);
            body.put("message", "Email verified.");
            body.put("customerId", verification.getCustomerId());
            body.put("redirect", "login.html");
            writeJson(response, HttpServletResponse.SC_OK, body);
        } catch (SQLException | RuntimeException e) {
            log("Verification error", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    error("Email verification could not be completed."));
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte current : bytes) hex.append(String.format("%02x", current));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to hash verification token.", e);
        }
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
