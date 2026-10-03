
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
import com.moffatbaymarina.dao.BoatDAO;
import com.moffatbaymarina.dao.SlipDAO; // ADDED: needed for the public per-slip-type available count
import com.moffatbaymarina.dao.SlipTypeDAO;
import com.moffatbaymarina.dao.WaitlistDAO;
import com.moffatbaymarina.model.Boat;
import com.moffatbaymarina.model.SlipType;
import com.moffatbaymarina.model.WaitlistEntry;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Wait list stuff: 'GET' is now public (per the project spec, the wait list
// page itself needs no login) and returns a name-free per-slip-size summary;
// when the caller IS logged in it also returns that customer's own entries.
// joining the list still requires active session.
// CHANGED 9-19-26 by Max.  GET no longer requires login, see doGet below.
@WebServlet("/waitlist")
public class WaitlistServlet extends HttpServlet {
    private static final ObjectMapper JSON = new ObjectMapper();

	// returns the public wait list summary (counts only, no names - see project spec)
	// for every slip size, plus, when the caller is logged in, that customer's own
	// entries with their current position so they can see how many are ahead of them.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        prepareJson(response);
        Long customerId = authenticatedCustomerId(request);
        // MODIFIED: no more 401, the page must work for a visitor who isn't
        // logged in, so we just fall through with customerId possibly null.

        try {
            WaitlistDAO waitlistDAO = new WaitlistDAO();
            BoatDAO boatDAO = new BoatDAO();
            SlipTypeDAO slipTypeDAO = new SlipTypeDAO();
            SlipDAO slipDAO = new SlipDAO(); // ADDED 9-19: for the public availableCount per slip type

            // mod 9-19. public, counts-only summary - one row per slip size. No customer
            // names or IDs appear anywhere in this block, so it's safe for any visitor.
            List<Map<String, Object>> summary = new ArrayList<>();
            for (SlipType slipType : slipTypeDAO.findAll()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("slipTypeId", slipType.getSlipTypeId());
                row.put("sizeFt", slipType.getSizeFt());
                row.put("availableCount", slipDAO.countAvailable(slipType.getSlipTypeId()));
                row.put("waitingCount", waitlistDAO.countWaiting(slipType.getSlipTypeId()));
                summary.add(row);
            }

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("ok", true);
            body.put("loggedIn", customerId != null); // Anow tells the page whether to show "your entries" + the join form 9-19
            body.put("slipTypes", summary); // addition: public summary built above

            // modified on 9-20-26: personal entries are only fetched when someone is logged in, instead of gating the whole endpoint on it like before.
            if (customerId != null) {
                List<Map<String, Object>> results = new ArrayList<>();
                // tack the current position onto each entry so the customer
                // can see how many people are ahead of them
                for (WaitlistEntry entry : waitlistDAO.findByCustomerId(customerId)) {
                    Boat boat = boatDAO.findById(entry.getBoatId());
                    SlipType slipType = slipTypeDAO.findById(entry.getSlipTypeId());
                    Map<String, Object> row = waitlistMap(entry, boat, slipType);
                    row.put("position", waitlistDAO.getPosition(entry));
                    results.add(row);
                }
                body.put("entries", results); // CHANGED: moved inside the if (customerId != null) block
            }

            writeJson(response, HttpServletResponse.SC_OK, body);
        } catch (SQLException e) {
            log("Waitlist lookup error", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    error("Waitlist information could not be loaded."));
        }
    }


	 //adds a waitlist entry for one customer boat. the slip type can be given sliptypeID, 
	 // or worked out by boay length. This can happen by the length supplied or by the recorded value.
	 // preventing duplicate sign up. 
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        prepareJson(response);
        Long customerId = authenticatedCustomerId(request);
        if (customerId == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    error("Log in before joining the waitlist."));
            return;
        }

        JsonNode data = readJson(request, response);
        if (data == null) return;
        Long boatId = longValue(data, "boatId");
        String boatName = text(data, "boatName");
        Long slipTypeId = longValue(data, "slipTypeId");
        BigDecimal boatLength = decimal(data, "boatLength");

        BoatDAO boatDAO = new BoatDAO();
        SlipTypeDAO slipTypeDAO = new SlipTypeDAO();
        WaitlistDAO waitlistDAO = new WaitlistDAO();

        try {
            Boat boat;
            if (boatId != null) boat = boatDAO.findForCustomer(boatId, customerId);
            else if (!boatName.isBlank()) boat = boatDAO.findByCustomerAndName(customerId, boatName);
            else {
                writeJson(response, 422, error("Select a boat before joining the waitlist."));
                return;
            }
            if (boat == null) {
                writeJson(response, HttpServletResponse.SC_NOT_FOUND, error("Boat not found."));
                return;
            }
			 // slip type can come in directly, or we work it out from a
            // boat length, either the one passed in, or the boat's own
            SlipType slipType;
            if (slipTypeId != null) slipType = slipTypeDAO.findById(slipTypeId);
            else {
                BigDecimal length = boatLength != null ? boatLength : boat.getBoatLengthFt();
                slipType = slipTypeDAO.findRequiredForBoatLength(length);
            }
            if (slipType == null) {
                writeJson(response, 422, error("No matching slip type was found."));
                return;
            }

            if (waitlistDAO.hasActiveEntry(customerId, boat.getBoatId(), slipType.getSlipTypeId())) {
                writeJson(response, HttpServletResponse.SC_CONFLICT,
                        error("An active waitlist entry already exists for this boat and slip size."));
                return;
            }

            WaitlistEntry entry = new WaitlistEntry();
            entry.setCustomerId(customerId);
            entry.setBoatId(boat.getBoatId());
            entry.setSlipTypeId(slipType.getSlipTypeId());
            entry.setStatus("WAITING");
            waitlistDAO.insert(entry);

            // insert already fills in entry's waitlistId, so entry
            // already has everything we need - this re-fetch is a bit
            // redundant but doesn't hurt anything
            WaitlistEntry saved = waitlistDAO.findByCustomerId(customerId).stream()
                    .filter(item -> item.getWaitlistId() == entry.getWaitlistId())
                    .findFirst().orElse(entry);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("ok", true);
            body.put("message", "You have been added to the waitlist.");
            body.put("entry", waitlistMap(saved, boat, slipType));
            body.put("position", waitlistDAO.getPosition(saved));
            writeJson(response, HttpServletResponse.SC_CREATED, body);
        } catch (SQLException e) {
            log("Waitlist creation error", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    error("The waitlist entry could not be created."));
        }
    }

    private Map<String, Object> waitlistMap(WaitlistEntry entry, Boat boat, SlipType slipType) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("waitlistId", entry.getWaitlistId());
        map.put("customerId", entry.getCustomerId());
        map.put("boatId", entry.getBoatId());
        map.put("boatName", boat == null ? null : boat.getBoatName());
        map.put("boatLengthFt", boat == null ? null : boat.getBoatLengthFt());
        map.put("slipTypeId", entry.getSlipTypeId());
        map.put("slipSizeFt", slipType == null ? null : slipType.getSizeFt());
        map.put("joinedAt", entry.getJoinedAt() == null ? null : entry.getJoinedAt().toString());
        map.put("status", entry.getStatus());
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

    private BigDecimal decimal(JsonNode data, String field) {
        JsonNode node = data.get(field);
        if (node == null || node.isNull()) return null;
        try { return new BigDecimal(node.asText().trim()); }
        catch (NumberFormatException e) { return null; }
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
