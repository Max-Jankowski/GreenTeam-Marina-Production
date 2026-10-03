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
import com.moffatbaymarina.dao.BoatDAO;
import com.moffatbaymarina.dao.ReservationDAO;
import com.moffatbaymarina.dao.SlipDAO;
import com.moffatbaymarina.dao.SlipTypeDAO;
import com.moffatbaymarina.model.Boat;
import com.moffatbaymarina.model.Reservation;
import com.moffatbaymarina.model.Slip;
import com.moffatbaymarina.model.SlipType;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;


 // Handles the reservation life cycle for a logged-in customer:
 // POST /reservation creates a new 'pending' reservation, and PUT /reservation confirms or cancels an existing one.  
 // Both require an active session. there's no unlogged in reservation creation.
 
@WebServlet("/reservation")
public class ReservationServlet extends HttpServlet {
    private static final ObjectMapper JSON = new ObjectMapper();

  
	 //looks up client's boat and checks which slip type it needs.
	 // then it claims one that is still available inside the transaction using row locking
	 // this is what in theory should stop to customers from claiming the same slip.
	 // makrked as pending if its claimed and then to held.
	 // triggers 401 if not logged in 409 if slip isnt open. 	 
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        prepareJson(response);
        Long customerId = authenticatedCustomerId(request);
        if (customerId == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    error("Log in before creating a reservation."));
            return;
        }

        JsonNode data = readJson(request, response);
        if (data == null) return;
        Long boatId = longValue(data, "boatId");
        String boatName = text(data, "boatName");
        String checkInValue = text(data, "checkInDate");
        if (checkInValue.isBlank()) checkInValue = text(data, "checkIn");
		String departureValue = text(data, "departureDate"); // ADDED read for departure 9-17-26
        String expectedTerm = text(data, "expectedTerm");		
        boolean electricIncluded = bool(data, "electricIncluded", false); // addition for opt in 9-10-26

        if ((boatId == null && boatName.isBlank()) || checkInValue.isBlank()
                || departureValue.isBlank() || expectedTerm.isBlank()) { // ADDED depart date to the req. field 9-17
            writeJson(response, 422,
                    error("Boat, check-in date, departure date, and expected term are required.")); // ADDED depart error message 9-17
            return;
        }

        LocalDate checkInDate;
        try { checkInDate = LocalDate.parse(checkInValue); }
        catch (DateTimeParseException e) {
            writeJson(response, 422, error("Enter a valid check-in date."));
            return;
        }
        if (checkInDate.isBefore(LocalDate.now())) {
            writeJson(response, 422, error("Check-in date cannot be in the past."));
            return;
        }
		
		// new for departure field 
        LocalDate departureDate; // ADDED the declaration for deaprture date 
        try { departureDate = LocalDate.parse(departureValue); } // ADDED parsing for depart 
        catch (DateTimeParseException e) { // catch for the date 
            writeJson(response, 422, error("Enter a valid departure date.")); // error response 917
            return; // daparture date validation 
        }
        if (!departureDate.isAfter(checkInDate)) { 
            writeJson(response, 422, error("Departure date must be after the check-in date.")); 
            return; 
        }

        BoatDAO boatDAO = new BoatDAO();
        SlipTypeDAO slipTypeDAO = new SlipTypeDAO();
        SlipDAO slipDAO = new SlipDAO();
        ReservationDAO reservationDAO = new ReservationDAO();

        try {
            // works whether they picked a boat from a dropdown meaning it has an id
            // or just typed the name again. In either case findForCustomer findByCustomerAndName make sure it's actually THEIR boat
            Boat boat = boatId != null
                    ? boatDAO.findForCustomer(boatId, customerId)
                    : boatDAO.findByCustomerAndName(customerId, boatName);
            if (boat == null) {
                writeJson(response, HttpServletResponse.SC_NOT_FOUND,
                        error("The selected boat was not found."));
                return;
            }

            SlipType slipType = slipTypeDAO.findRequiredForBoatLength(boat.getBoatLengthFt());
            if (slipType == null) {
                writeJson(response, 422, error("No slip type can accommodate this boat."));
                return;
            }

            
            // added electricFee only gets put in when the customer opts into the added service 9-10-26 Max
            BigDecimal monthlyCost = boat.getBoatLengthFt()
                    .multiply(slipType.getRatePerFoot());
            if (electricIncluded) {
                monthlyCost = monthlyCost.add(slipType.getElectricFee());
            }
            monthlyCost = monthlyCost.setScale(2, RoundingMode.HALF_UP);

            try (Connection connection = DatabaseConnection.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    // locks the slip row until we commit or rollback. stops two people from both grabbing the last open slip of this size at the same time.
                    Slip slip = slipDAO.findAvailableForUpdate(
                            connection, slipType.getSlipTypeId());
                    if (slip == null) {
                        connection.rollback();
                        Map<String, Object> body = error(
                                "No matching slips are currently available.");
                        body.put("waitlistRecommended", true);
                        body.put("slipTypeId", slipType.getSlipTypeId());
                        body.put("slipSizeFt", slipType.getSizeFt());
                        writeJson(response, HttpServletResponse.SC_CONFLICT, body);
                        return;
                    }

                    Reservation reservation = new Reservation();
                    reservation.setCustomerId(customerId);
                    reservation.setBoatId(boat.getBoatId());
                    reservation.setSlipId(slip.getSlipId());
                    reservation.setCheckInDate(checkInDate);
					reservation.setDepartureDate(departureDate); // ADDED setter for depart 
                    reservation.setExpectedTerm(expectedTerm);
                    reservation.setMonthlyCost(monthlyCost);
                    reservation.setElectricIncluded(electricIncluded); // added 9-10
                    reservation.setStatus("PENDING");
                    reservationDAO.insert(connection, reservation);
                    // Matches the supplied seed pattern: PENDING reservation--- HELD slip.
                    slipDAO.updateStatus(connection, slip.getSlipId(), "HELD");
                    connection.commit();

                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("ok", true);
                    body.put("message", "Reservation created.");
                    body.put("reservation", reservationMap(reservation, boat, slip, slipType));
                    body.put("redirect", "reservation-summary.html?id="
                            + reservation.getReservationId());
                    writeJson(response, HttpServletResponse.SC_CREATED, body);
                } catch (SQLException | RuntimeException e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        } catch (SQLException | RuntimeException e) {
            log("Reservation creation error", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    error("The reservation could not be created."));
        }
    }

    // confirm or cancel an existing reservation. updates the reservation and the slip status together. Checks the reservation actually
    // belongs to whoever's asking before touching anything.
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        prepareJson(response);
        Long customerId = authenticatedCustomerId(request);
        if (customerId == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    error("Log in before updating a reservation."));
            return;
        }

        JsonNode data = readJson(request, response);
        if (data == null) return;
        Long reservationId = longValue(data, "reservationId");
        if (reservationId == null) reservationId = longValue(data, "id");
        String status = text(data, "status").toUpperCase();
        if (reservationId == null
                || (!status.equals("CONFIRMED") && !status.equals("CANCELLED"))) {
            writeJson(response, 422,
                    error("Reservation ID and a CONFIRMED or CANCELLED status are required."));
            return;
        }

        ReservationDAO reservationDAO = new ReservationDAO();
        SlipDAO slipDAO = new SlipDAO();
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Reservation reservation = reservationDAO.findByIdForUpdate(connection, reservationId);
               // getCustomerId() is a primitive long here, so != auto-unboxes the boxed customerId first. This compares the actual numbers, not object identity, so it's fine
                if (reservation == null || reservation.getCustomerId() != customerId) {
                    connection.rollback();
                    writeJson(response, HttpServletResponse.SC_NOT_FOUND,
                            error("Reservation not found."));
                    return;
                }
                LocalDateTime cancelledAt = status.equals("CANCELLED")
                        ? LocalDateTime.now() : null;
                reservationDAO.updateStatus(connection, reservationId, status, cancelledAt);
                String slipStatus = status.equals("CONFIRMED") ? "RESERVED" : "AVAILABLE";
                slipDAO.updateStatus(connection, reservation.getSlipId(), slipStatus);
                connection.commit();

                Map<String, Object> body = new LinkedHashMap<>();
                body.put("ok", true);
                body.put("message", "Reservation status updated.");
                body.put("reservationId", reservationId);
                body.put("status", status);
                body.put("slipStatus", slipStatus);
                writeJson(response, HttpServletResponse.SC_OK, body);
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException | RuntimeException e) {
            log("Reservation update error", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    error("The reservation could not be updated."));
        }
    }

    private Map<String, Object> reservationMap(Reservation reservation, Boat boat,
                                                Slip slip, SlipType slipType) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("reservationId", reservation.getReservationId());
        map.put("customerId", reservation.getCustomerId());
        map.put("boatId", reservation.getBoatId());
        map.put("boatName", boat.getBoatName());
        map.put("boatLengthFt", boat.getBoatLengthFt());
        map.put("slipId", slip.getSlipId());
        map.put("slipNumber", slip.getSlipNumber());
        map.put("slipTypeId", slipType.getSlipTypeId());
        map.put("slipSizeFt", slipType.getSizeFt());
        map.put("checkInDate", reservation.getCheckInDate().toString());
		map.put("departureDate", reservation.getDepartureDate().toString()); // ADD field to response
        map.put("expectedTerm", reservation.getExpectedTerm());
        map.put("monthlyCost", reservation.getMonthlyCost());
        map.put("electricIncluded", reservation.isElectricIncluded()); //added really late 9-10
        map.put("status", reservation.getStatus());
        return map;
    }

     // both handlers above need to be logged in, hence checking this first
    private Long authenticatedCustomerId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Object value = session.getAttribute("customerId");
        return value instanceof Number number ? number.longValue() : null;
    }

    private JsonNode readJson(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try { return JSON.readTree(request.getInputStream()); }
        catch (IOException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    error("Invalid JSON request body."));
            return null;
        }
    }

    private Long longValue(JsonNode data, String field) {
        JsonNode node = data.get(field);
        if (node == null || node.isNull() || node.asText("").isBlank()) return null;
        try { return Long.valueOf(node.asText()); }
        catch (NumberFormatException e) { return null; }
    }

    // added on 9-10 by Max reads a true/false field, falling back to defaultValue if missing 
    private boolean bool(JsonNode data, String field, boolean defaultValue) {
        JsonNode node = data.get(field);
        return node == null || node.isNull() ? defaultValue : node.asBoolean(defaultValue);
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
