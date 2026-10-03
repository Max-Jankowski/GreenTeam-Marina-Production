

/**Alexander Baldree
Max Jankowski
Aftabur Rahman
Jordan Dardar

Green team Module 5
Modified by Max on 9-4-26

*/
package com.moffatbaymarina.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moffatbaymarina.dao.BoatDAO;
import com.moffatbaymarina.dao.CustomerDAO;
import com.moffatbaymarina.dao.ReservationDAO;
import com.moffatbaymarina.dao.SlipDAO;
import com.moffatbaymarina.dao.SlipTypeDAO;
import com.moffatbaymarina.model.Boat;
import com.moffatbaymarina.model.Customer;
import com.moffatbaymarina.model.Reservation;
import com.moffatbaymarina.model.Slip;
import com.moffatbaymarina.model.SlipType;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// GET /reservation-lookup - the "find your reservation" page. Tries a
// reservation ID or email first, and if neither was given, falls back
// to whoever's currently logged in.
@WebServlet("/reservation-lookup")
public class ReservationLookupServlet extends HttpServlet {
    private static final ObjectMapper JSON = new ObjectMapper();

    
	 //if res id or email is in the query string, seaches by those. if both are missing falls back 
	 //to the client logged in reservations. if that too is not present there will be nothing to search 
	 //with so this triggers the 422
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        prepareJson(response);
        Long reservationId = parseLong(firstNonBlank(
                request.getParameter("reservationId"), request.getParameter("id")));
        String email = clean(request.getParameter("email"));
        Long sessionCustomerId = authenticatedCustomerId(request);

        if (sessionCustomerId == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    error("Log in to view your reservation history."));
            return;
        }

        try {
            ReservationDAO reservationDAO = new ReservationDAO();
            List<Reservation> reservations;
            if (reservationId != null || !email.isBlank()) {
                reservations = reservationDAO.searchForCustomer(
                        sessionCustomerId, reservationId, email);
            } else {
                reservations = reservationDAO.findByCustomerId(sessionCustomerId);
            }

            List<Map<String, Object>> results = new ArrayList<>();
            for (Reservation reservation : reservations) results.add(details(reservation));
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("ok", true);
            body.put("count", results.size());
            body.put("reservations", results);
            writeJson(response, HttpServletResponse.SC_OK, body);
        } catch (SQLException e) {
            log("Reservation lookup error", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    error("Reservation lookup could not be completed."));
        }
    }

    // builds the full display info for one reservation - looks up
    // customer/boat/slip/slip type separately per reservation. Kind of
    // a N+1 query pattern, fine for our scale but worth knowing about
    private Map<String, Object> details(Reservation reservation) throws SQLException {
        Customer customer = new CustomerDAO().findById(reservation.getCustomerId());
        Boat boat = new BoatDAO().findById(reservation.getBoatId());
        Slip slip = new SlipDAO().findById(reservation.getSlipId());
        SlipType slipType = slip == null ? null
                : new SlipTypeDAO().findById(slip.getSlipTypeId());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("reservationId", reservation.getReservationId());
        map.put("email", customer == null ? null : customer.getEmail());
        map.put("customerName", customer == null ? null
                : customer.getFirstName() + " " + customer.getLastName());
        map.put("boatName", boat == null ? null : boat.getBoatName());
        map.put("boatLengthFt", boat == null ? null : boat.getBoatLengthFt());
        map.put("slipNumber", slip == null ? null : slip.getSlipNumber());
        map.put("slipSizeFt", slipType == null ? null : slipType.getSizeFt());
        map.put("checkInDate", reservation.getCheckInDate() == null ? null
                : reservation.getCheckInDate().toString());
        map.put("expectedTerm", reservation.getExpectedTerm());
        map.put("monthlyCost", reservation.getMonthlyCost());
        map.put("electricIncluded", reservation.isElectricIncluded()); //adding to see if this fixs the positive indicator in summary
        map.put("status", reservation.getStatus());
        map.put("createdAt", reservation.getCreatedAt() == null ? null
                : reservation.getCreatedAt().toString());
        map.put("cancelledAt", reservation.getCancelledAt() == null ? null
                : reservation.getCancelledAt().toString());
        return map;
    }

     // Reads customerId out of the current session, if there is one.
     // used as the fallback lookup when no ID/email was supplied. 
    private Long authenticatedCustomerId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Object value = session.getAttribute("customerId");
        return value instanceof Number number ? number.longValue() : null;
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Long.valueOf(value.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }

    private Map<String, Object> error(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", false);
        body.put("message", message);
        return body;
    }

    private void prepareJson(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
    }

    private void writeJson(HttpServletResponse response, int status, Object body)
            throws IOException {
        response.setStatus(status);
        JSON.writeValue(response.getWriter(), body);
    }
}
