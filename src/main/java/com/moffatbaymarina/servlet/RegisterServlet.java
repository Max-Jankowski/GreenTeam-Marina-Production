/**Alexander Baldree
Max Jankowski
Aftabur Rahman
Jordan Dardar

Green team Module 5
Modified by Max on 9-4-26

*/

package com.moffatbaymarina.servlet;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.mindrot.jbcrypt.BCrypt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moffatbaymarina.config.DatabaseConnection;
import com.moffatbaymarina.dao.BoatDAO;
import com.moffatbaymarina.dao.CustomerDAO;
import com.moffatbaymarina.dao.EmailVerificationDAO;
import com.moffatbaymarina.model.Boat;
import com.moffatbaymarina.model.Customer;
import com.moffatbaymarina.model.EmailVerification;
import com.moffatbaymarina.service.EmailService;

import jakarta.mail.MessagingException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// POST /register - creates the customer, their first boat, and an
// email verification token all in one go. Expects JSON in the body
// (register.html's script builds the exact shape we read below).
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern ZIP = Pattern.compile("^\\d{5}(-\\d{4})?$");

    // will validate each field, then if the customer with the email doesnt exist
    // will insert the row with cleint and boat.
    // if any of the inserts fail all are rolled back as one.
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        prepareJson(response);
        JsonNode data = readJson(request, response);
        if (data == null)
            return;

        String firstName = text(data, "firstName");
        String lastName = text(data, "lastName");
        String phone = text(data, "phone");
        String street = text(data, "street");
        String city = text(data, "city");
        String state = text(data, "state").toUpperCase();
        String zip = text(data, "zip");
        String email = text(data, "email").toLowerCase();
        String password = text(data, "password");
        String boatName = text(data, "boatName");
        String boatType = text(data, "boatType");
        String registrationNumber = text(data, "registrationNumber");
        BigDecimal boatLength = decimal(data, "boatLength");

        // one generic "fill everything out" message - register.html
        // already shows a field-specific error before it even calls us
        if (firstName.isBlank() || lastName.isBlank() || phone.isBlank()
                || street.isBlank() || city.isBlank() || state.isBlank()
                || zip.isBlank() || email.isBlank() || password.isBlank()
                || boatName.isBlank() || boatLength == null) {
            writeJson(response, 422, error("Complete all required registration fields."));
            return;
        }
        if (!EMAIL.matcher(email).matches()) {
            writeJson(response, 422, error("Enter a valid email address."));
            return;
        }
        if (!ZIP.matcher(zip).matches()) {
            writeJson(response, 422, error("Enter a valid ZIP code."));
            return;
        }
        if (state.length() != 2) {
            writeJson(response, 422, error("Use the two-letter state abbreviation."));
            return;
        }
        if (boatLength.compareTo(BigDecimal.ZERO) <= 0
                || boatLength.compareTo(new BigDecimal("200")) > 0) {
            writeJson(response, 422, error("Boat length must be between 1 and 200 feet."));
            return;
        }
        if (!validPassword(password)) {
            writeJson(response, 422, error(
                    "Password must be at least 8 characters and include uppercase, "
                            + "lowercase, a number, and a special character."));
            return;
        }

        CustomerDAO customerDAO = new CustomerDAO();
        BoatDAO boatDAO = new BoatDAO();
        EmailVerificationDAO verificationDAO = new EmailVerificationDAO();

        try {
            if (customerDAO.emailExists(email)) {
                writeJson(response, HttpServletResponse.SC_CONFLICT,
                        error("An account already exists for this email address."));
                return;
            }

            Customer customer = new Customer();
            Boat boat = new Boat();
            String rawToken = generateVerificationToken();

            // Customer + boat + verification token are inserted in one transaction.
            // If any database operation fails, the whole registration is rolled back.
            try (Connection connection = DatabaseConnection.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    customer.setFirstName(firstName);
                    customer.setLastName(lastName);
                    customer.setPhone(phone);
                    customer.setStreet(street);
                    customer.setCity(city);
                    customer.setState(state);
                    customer.setZip(zip);
                    customer.setEmail(email);
                    customer.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt(12)));
                    customer.setEmailVerified(false);
                    customerDAO.insert(connection, customer);

                    boat.setCustomerId(customer.getCustomerId());
                    boat.setBoatName(boatName);
                    boat.setBoatLengthFt(boatLength);
                    boat.setBoatType(blankToNull(boatType));
                    boat.setRegistrationNumber(blankToNull(registrationNumber));
                    boatDAO.insert(connection, boat);

                    EmailVerification verification = new EmailVerification();
                    verification.setCustomerId(customer.getCustomerId());
                    verification.setTokenHash(sha256(rawToken));
                    verification.setExpiresAt(LocalDateTime.now().plusHours(24));
                    verificationDAO.insert(connection, verification);

                    connection.commit();
                } catch (SQLException | RuntimeException e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            }

            // Email is sent after the database transaction commits. A temporary SMTP
            // failure should not roll back an account that was already saved correctly.
            boolean emailSent = true;
            try {
                EmailService.sendVerificationEmail(
                        customer.getEmail(),
                        customer.getFirstName(),
                        rawToken);
            } catch (MessagingException | RuntimeException e) {
                emailSent = false;
                log("Registration succeeded, but verification email could not be sent", e);
            }

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("ok", true);
            body.put("message", emailSent
                    ? "Account and boat information saved. Verification email sent."
                    : "Account and boat information saved, but the verification email could not be sent.");
            body.put("customerId", customer.getCustomerId());
            body.put("boatId", boat.getBoatId());
            body.put("emailVerified", false);
            body.put("emailSent", emailSent);

            // Kept for the course prototype so the verification flow can still be
            // tested locally even if SMTP credentials are not configured.
            body.put("verificationToken", rawToken);
            body.put("verificationUrl", "verification.html?token=" + rawToken);

            writeJson(response, HttpServletResponse.SC_CREATED, body);
        } catch (SQLException e) {
            log("Registration database error", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    error("The account could not be created."));
        } catch (RuntimeException e) {
            log("Registration error", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    error("The account could not be created."));
        }
    }

    // matches the rule shown on register.html: 8+ chars, upper, lower, digit, and a
    // special character
    private boolean validPassword(String password) {
        return password.length() >= 8
                && password.matches(".*[A-Z].*")
                && password.matches(".*[a-z].*")
                && password.matches(".*\\d.*")
                && password.matches(".*[^A-Za-z0-9].*");
    }

    // 32 random bytes, url-safe base64 so it drops straight into a link with no
    // escaping needed - this is the raw token we'd email out
    // https://www.geeksforgeeks.org/java/url-encoding-decoding-using-base64-in-java/
    private String generateVerificationToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // one-way hash of the token before we store it. look at
    // EmailVerificationDAO.findValid for how this gets checked later
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte current : bytes)
                hex.append(String.format("%02x", current));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to hash verification token.", e);
        }
    }

    // returns null with a 400 already written if the body isn't valid JSON
    private JsonNode readJson(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            return JSON.readTree(request.getInputStream());
        } catch (IOException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    error("Invalid JSON request body."));
            return null;
        }
    }

    private BigDecimal decimal(JsonNode data, String field) {
        JsonNode node = data.get(field);
        if (node == null || node.isNull())
            return null;
        try {
            return new BigDecimal(node.asText().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String text(JsonNode data, String field) {
        JsonNode node = data.get(field);
        return node == null || node.isNull() ? "" : node.asText("").trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
